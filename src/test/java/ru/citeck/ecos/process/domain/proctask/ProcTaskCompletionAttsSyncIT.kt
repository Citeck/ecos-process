package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.type.RecordEventsService
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaProcessEngineService
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.attssync.MetaDataChangeTaskAttsSyncListener
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttributeType
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordsMutateDao
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import java.util.Date

/** Real EprocApp, event listener, synchronizer, DTO cache, Camunda and PostgreSQL; only the document is a fixture. */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class, ProcTaskCompletionAttsSyncIT.TestConfig::class])
@Timeout(120)
class ProcTaskCompletionAttsSyncIT {

    @Autowired private lateinit var taskService: TaskService
    @Autowired private lateinit var runtimeService: RuntimeService
    @Autowired private lateinit var repositoryService: RepositoryService
    @Autowired private lateinit var documentChange: DocumentChangeDelegate
    @Autowired private lateinit var procTaskService: ProcTaskService
    @Autowired private lateinit var cachedConverter: CacheableTaskConverter
    @Autowired private lateinit var recordsService: RecordsService
    @Autowired private lateinit var helper: BpmnProcHelper
    @Autowired private lateinit var recordEvents: RecordEventsService
    @Autowired private lateinit var types: EcosTypesRegistry
    @Autowired private lateinit var syncListener: MetaDataChangeTaskAttsSyncListener

    private val typeRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    private val document = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@incident")
    private val unrelatedDocument = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@unrelated")
    private val deployments = mutableListOf<String>()
    private lateinit var documentDao: DocumentDao
    private lateinit var completingTaskId: String
    private var remainingTaskId: String? = null
    private var expectExistingVariable = false
    private var expectCachedActiveDto = false
    private var callbackCount = 0
    private var projectOnlyChangedFields = false

    @BeforeEach
    fun setUp() {
        AuthContext.runAsSystem {
            helper.cleanTaskAttsSyncSettings()
            types.setValue(
                DOCUMENT_TYPE,
                TypeDef.create().withId(DOCUMENT_TYPE).withModel(
                    TypeModelDef.create {
                        withAttributes(
                            listOf(
                                AttributeDef.create {
                                    withId(DOCUMENT_ATTRIBUTE)
                                    withType(AttributeType.DATETIME)
                                },
                                AttributeDef.create {
                                    withId(ADDITIONAL_ATTRIBUTE)
                                    withType(AttributeType.NUMBER)
                                }
                            )
                        )
                    }
                ).build()
            )
            documentDao = DocumentDao { before, after ->
                if (projectOnlyChangedFields) {
                    recordEvents.emitRecChanged(
                        after.ref,
                        mapOf(DOCUMENT_ATTRIBUTE to before.reactionDate),
                        mapOf(DOCUMENT_ATTRIBUTE to after.reactionDate)
                    )
                } else {
                    // The real producer includes unchanged model attributes in its before/after snapshots.
                    recordEvents.emitRecChanged(before, after)
                }
            }
            recordsService.register(documentDao)
            helper.createAttsSync(
                id = "completion-sync-regression",
                enabled = true,
                source = TaskAttsSyncSource.RECORD,
                name = "Completion sync regression",
                attributesSync = listOf(
                    TaskSyncAttribute(
                        id = TASK_ATTRIBUTE,
                        type = AttributeType.DATETIME,
                        ecosTypes = listOf(TaskSyncAttributeType(typeRef, DOCUMENT_ATTRIBUTE))
                    ),
                    TaskSyncAttribute(
                        id = ADDITIONAL_TASK_ATTRIBUTE,
                        type = AttributeType.NUMBER,
                        ecosTypes = listOf(TaskSyncAttributeType(typeRef, ADDITIONAL_ATTRIBUTE))
                    )
                )
            )
            syncListener.updateListeners()
            documentChange.action = {
                // Assert the actual engine state that previously allowed the FK failure.
                val pending = taskService.createTaskQuery()
                    .processVariableValueEquals(BPMN_DOCUMENT_REF, document.toString()).list()
                    .single { task -> task.id == completingTaskId } as TaskEntity
                assertThat(pending.isDeleted).isTrue()
                // Deletion clears a variable's value but leaves its entity in the task variable store.
                assertThat(taskService.getVariablesLocal(completingTaskId)).containsKey(VARIABLE)
                assertThat(taskService.getVariablesLocal(completingTaskId).containsKey(ADDITIONAL_VARIABLE))
                    .isEqualTo(expectExistingVariable)
                if (expectCachedActiveDto) {
                    assertThat(cachedConverter.convertTask(pending).isDeleted).isFalse()
                }

                val commandContext = Context.getCommandContext()
                recordsService.mutate(document, mapOf(DOCUMENT_ATTRIBUTE to AFTER))
                assertThat(Context.getCommandContext()).isSameAs(commandContext)
                callbackCount++

                // This must be updated synchronously by the production listener/synchronizer, before flush.
                remainingTaskId?.let { id ->
                    assertThat(taskService.getVariableLocal(id, VARIABLE)).isEqualTo(date(AFTER))
                }
            }
            deployments += repositoryService.createDeployment().addModelInstance(
                "$PROCESS.bpmn",
                Bpmn.createExecutableProcess(PROCESS).startEvent().userTask(TASK)
                    .serviceTask("changeDocument").camundaDelegateExpression("\${$DELEGATE}")
                    .endEvent().done()
            ).deploy().id
            deployments += repositoryService.createDeployment().addModelInstance(
                "$WAIT_PROCESS.bpmn",
                Bpmn.createExecutableProcess(WAIT_PROCESS).startEvent().userTask(TASK).endEvent().done()
            ).deploy().id
        }
    }

    @AfterEach
    fun cleanUp() {
        AuthContext.runAsSystem {
            deployments.forEach { repositoryService.deleteDeployment(it, true) }
            helper.cleanTaskAttsSyncSettings()
            documentChange.action = { error("No active integration fixture") }
        }
    }

    @ParameterizedTest(name = "legacy task with existing date but missing FP1, platform completion={0}")
    @ValueSource(booleans = [false, true])
    fun `completion skips deleted task but updates another active task of the same document`(platformCompletion: Boolean) {
        runCompletion(platformCompletion, withRemainingTask = true, existingVariable = false)
    }

    @Test
    fun `completion succeeds when the document has only the deleted synchronization target`() {
        runCompletion(platformCompletion = false, withRemainingTask = false, existingVariable = false)
    }

    @Test
    fun `new task with all synchronized attributes completes with the same document event`() {
        runCompletion(platformCompletion = true, withRemainingTask = true, existingVariable = true)
    }

    @Test
    fun `legacy task also completes if the event contains only the already existing date`() {
        projectOnlyChangedFields = true
        runCompletion(platformCompletion = true, withRemainingTask = true, existingVariable = false)
    }

    private fun runCompletion(platformCompletion: Boolean, withRemainingTask: Boolean, existingVariable: Boolean) {
        AuthContext.runAsSystem {
            val instance = start(PROCESS, document)
            completingTaskId = taskService.createTaskQuery().processInstanceId(instance).singleResult().id
            if (withRemainingTask) {
                val remaining = start(WAIT_PROCESS, document)
                remainingTaskId = taskService.createTaskQuery().processInstanceId(remaining).singleResult().id
            }
            val unrelated = start(WAIT_PROCESS, unrelatedDocument)
            val unrelatedTask = taskService.createTaskQuery().processInstanceId(unrelated).singleResult().id
            assertThat(taskService.getVariableLocal(unrelatedTask, VARIABLE)).isEqualTo(date(BEFORE))
            assertThat(taskService.getVariableLocal(completingTaskId, VARIABLE)).isEqualTo(date(BEFORE))
            assertThat(taskService.getVariableLocal(completingTaskId, ADDITIONAL_VARIABLE)).isEqualTo(FP1)
            if (!existingVariable) {
                // Model a legacy task: the date exists, but a later task projection has not been filled.
                taskService.removeVariableLocal(completingTaskId, ADDITIONAL_VARIABLE)
            }
            expectExistingVariable = existingVariable
            expectCachedActiveDto = !platformCompletion

            // Warm the real shared DTO cache. Direct engine completion leaves this old DTO in it.
            val dto = procTaskService.getTasksByDocument(document.toString()).single { it.id == completingTaskId }
            assertThat(dto.isDeleted).isFalse()
            TxnContext.doInTxn {
                if (platformCompletion) {
                    procTaskService.completeTask(CompleteTaskData(dto, Outcome(TASK, "done", MLText("done")), emptyMap()))
                } else {
                    taskService.complete(completingTaskId)
                }
            }

            assertThat(callbackCount).isEqualTo(1)
            assertThat(documentDao.values.getValue(document.getLocalId()).reactionDate).isEqualTo(AFTER)
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance).count()).isZero()
            assertThat(taskService.createTaskQuery().taskId(completingTaskId).count()).isZero()
            assertThat(runtimeService.createVariableInstanceQuery().taskIdIn(completingTaskId).count()).isZero()
            assertThat(runtimeService.createVariableInstanceQuery().executionIdIn(instance).count()).isZero()
            assertThat(taskService.getVariableLocal(unrelatedTask, VARIABLE)).isEqualTo(date(BEFORE))
            assertThat(taskService.getVariableLocal(unrelatedTask, ADDITIONAL_VARIABLE)).isEqualTo(FP1)
            remainingTaskId?.let { id ->
                assertThat(taskService.createTaskQuery().taskId(id).count()).isEqualTo(1)
                assertThat(taskService.getVariableLocal(id, VARIABLE)).isEqualTo(date(AFTER))
                assertThat(taskService.getVariableLocal(id, ADDITIONAL_VARIABLE)).isEqualTo(FP1)
            }
        }
    }

    private fun start(key: String, ref: EntityRef): String {
        return runtimeService.startProcessInstanceByKey(
            key,
            mapOf(BPMN_DOCUMENT_REF to ref.toString(), BPMN_DOCUMENT_TYPE to DOCUMENT_TYPE)
        ).id
    }

    /** In-process stand-in for the external document source; emits a real transactional Records event. */
    private class DocumentDao(private val changed: (Document, Document) -> Unit) :
        AbstractRecordsDao(),
        RecordsAttsDao,
        RecordsMutateDao {

        val values = listOf("incident", "unrelated").associateWith {
            Document(EntityRef.valueOf("eproc/$DOCUMENT_TYPE@$it"))
        }.toMutableMap()

        override fun getId() = "eproc/$DOCUMENT_TYPE"

        override fun getRecordsAtts(recordIds: List<String>): List<*> = recordIds.map { values.getValue(it) }

        override fun mutate(records: List<LocalRecordAtts>): List<String> = records.map {
            val before = values.getValue(it.id)
            val after = before.copy(reactionDate = it.getAtt(DOCUMENT_ATTRIBUTE).asText())
            values[it.id] = after
            changed(before, after)
            it.id
        }
    }

    private data class Document(
        @AttName("?id") val ref: EntityRef,
        @AttName("actualReactionDateTime") val reactionDate: String = BEFORE,
        @AttName("FP1") val fp1: Double = FP1,
        @AttName("_type") val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun completionDocumentChange() = DocumentChangeDelegate()
    }

    class DocumentChangeDelegate :
        JavaDelegate,
        CamundaProcessEngineService {
        lateinit var action: () -> Unit

        override fun getKey() = DELEGATE

        override fun execute(execution: DelegateExecution) = action()
    }

    companion object {
        private const val DOCUMENT_TYPE = "completion-sync-document"
        private const val DOCUMENT_ATTRIBUTE = "actualReactionDateTime"
        private const val TASK_ATTRIBUTE = "eventActualReactionDateTime"
        private const val VARIABLE = "_doc_$TASK_ATTRIBUTE"
        private const val ADDITIONAL_ATTRIBUTE = "FP1"
        private const val ADDITIONAL_TASK_ATTRIBUTE = "eventFP1"
        private const val ADDITIONAL_VARIABLE = "_doc_$ADDITIONAL_TASK_ATTRIBUTE"
        private const val FP1 = 1.0
        private const val BEFORE = "2026-10-29T00:00:00.000Z"
        private const val AFTER = "2026-10-30T00:00:00.000Z"
        private const val PROCESS = "completion-sync-regression"
        private const val WAIT_PROCESS = "completion-sync-wait"
        private const val TASK = "WaitForProcessing"
        private const val DELEGATE = "completionDocumentChange"

        private fun date(value: String): Date = Date.from(Instant.parse(value))
    }
}
