package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaMyBatisExtension
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.proctask.converter.TaskConverter
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordRefChangedListenerTest {

    companion object {
        private const val PROC_ID = "ref-changed-listener-test"
        private const val TYPE_ID = "test-ref-changed-type"
        private val TYPE_REF = EntityRef.valueOf("emodel/type@$TYPE_ID")
    }

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var camundaMyBatisExtension: CamundaMyBatisExtension

    @Autowired
    private lateinit var bpmnEventHelper: BpmnEventHelper

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var camundaTaskService: TaskService

    @Autowired
    private lateinit var taskConverter: TaskConverter

    @Autowired
    private lateinit var typesRegistry: EcosTypesRegistry

    @BeforeAll
    fun setUp() {
        typesRegistry.setValue(
            TYPE_ID,
            TypeDef.create {
                withId(TYPE_ID)
                withParentRef(ModelUtils.getTypeRef("user-base"))
            }
        )

        recordsService.register(
            RecordsDaoBuilder.create("emodel/$TYPE_ID")
                .addRecord("old-local-id", TestRecord(type = TYPE_REF))
                .addRecord("new-local-id", TestRecord(type = TYPE_REF))
                .build()
        )

        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )
    }

    @Test
    fun `update documentRef variable via direct SQL when ref changes`() {

        val oldRef = EntityRef.valueOf("emodel/$TYPE_ID@old-local-id")
        val newRef = EntityRef.valueOf("emodel/$TYPE_ID@new-local-id")

        val process = bpmnProcessService.startProcess(
            StartProcessRequest(
                "",
                PROC_ID,
                oldRef.toString(),
                mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )

        val varBefore = runtimeService.getVariable(process.id, BPMN_DOCUMENT_REF)
        assertThat(varBefore).isEqualTo(oldRef.toString())

        val updatedVars = camundaMyBatisExtension.updateDocumentRefVariables(oldRef, newRef)
        assertThat(updatedVars).isEqualTo(1)

        val updatedKeys = camundaMyBatisExtension.updateDocumentRefInBusinessKey(oldRef, newRef)
        assertThat(updatedKeys).isGreaterThanOrEqualTo(1)

        val updatedSubs = camundaMyBatisExtension.updateDocumentRefInEventSubscriptions(oldRef, newRef)
        assertThat(updatedSubs).isGreaterThanOrEqualTo(1)

        val varAfter = runtimeService.getVariable(process.id, BPMN_DOCUMENT_REF)
        assertThat(varAfter).isEqualTo(newRef.toString())

        val processInstance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(process.id)
            .singleResult()
        assertThat(processInstance.businessKey).isEqualTo(newRef.toString())

        val subscriptions = camundaMyBatisExtension.getEventSubscriptionsByEventNamesLikeStart(
            listOf("COMMENT_CREATE;$newRef%")
        )
        assertThat(subscriptions).isNotEmpty
        assertThat(subscriptions).allSatisfy { sub ->
            assertThat(sub.eventName).contains(newRef.toString())
            assertThat(sub.eventName).doesNotContain(oldRef.toString())
        }
    }

    @Test
    fun `update documentRef in multiple process instances`() {

        val oldRef = EntityRef.valueOf("emodel/$TYPE_ID@multi-old")
        val newRef = EntityRef.valueOf("emodel/$TYPE_ID@multi-new")

        val process1 = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROC_ID,
                businessKey = oldRef.toString(),
                variables = mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )
        val process2 = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROC_ID,
                businessKey = oldRef.toString(),
                variables = mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )

        val updatedCount = camundaMyBatisExtension.updateDocumentRefVariables(oldRef, newRef)
        assertThat(updatedCount).isEqualTo(2)

        assertThat(runtimeService.getVariable(process1.id, BPMN_DOCUMENT_REF)).isEqualTo(newRef.toString())
        assertThat(runtimeService.getVariable(process2.id, BPMN_DOCUMENT_REF)).isEqualTo(newRef.toString())
    }

    @Test
    fun `update returns zero when no matching documentRef exists`() {

        val nonExistentRef = EntityRef.valueOf("emodel/$TYPE_ID@does-not-exist")
        val newRef = EntityRef.valueOf("emodel/$TYPE_ID@new-ref")

        val updatedCount = camundaMyBatisExtension.updateDocumentRefVariables(nonExistentRef, newRef)
        assertThat(updatedCount).isEqualTo(0)
    }

    @Test
    fun `RecordRefChangedEvent triggers documentRef update in active process`() {

        val oldRef = EntityRef.valueOf("emodel/$TYPE_ID@event-old")
        val newRef = EntityRef.valueOf("emodel/$TYPE_ID@event-new")

        val process = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROC_ID,
                businessKey = oldRef.toString(),
                variables = mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )

        assertThat(runtimeService.getVariable(process.id, BPMN_DOCUMENT_REF)).isEqualTo(oldRef.toString())

        bpmnEventHelper.sendRecordRefChangedEvent(
            RecordRefChangedEventDto(
                record = oldRef,
                before = oldRef,
                after = newRef
            )
        )

        assertThat(runtimeService.getVariable(process.id, BPMN_DOCUMENT_REF)).isEqualTo(newRef.toString())

        val processInstance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(process.id)
            .singleResult()
        assertThat(processInstance.businessKey).isEqualTo(newRef.toString())

        val subscriptions = camundaMyBatisExtension.getEventSubscriptionsByEventNamesLikeStart(
            listOf("COMMENT_CREATE;$newRef%")
        )
        assertThat(subscriptions).isNotEmpty
        assertThat(subscriptions).allSatisfy { sub ->
            assertThat(sub.eventName).contains(newRef.toString())
        }
    }

    @Test
    fun `RecordRefChangedEvent updates documentRef in multiple active processes`() {

        val oldRef = EntityRef.valueOf("emodel/$TYPE_ID@event-multi-old")
        val newRef = EntityRef.valueOf("emodel/$TYPE_ID@event-multi-new")

        val process1 = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROC_ID,
                businessKey = oldRef.toString(),
                variables = mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )
        val process2 = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROC_ID,
                businessKey = oldRef.toString(),
                variables = mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )

        bpmnEventHelper.sendRecordRefChangedEvent(
            RecordRefChangedEventDto(
                record = oldRef,
                before = oldRef,
                after = newRef
            )
        )

        assertThat(runtimeService.getVariable(process1.id, BPMN_DOCUMENT_REF)).isEqualTo(newRef.toString())
        assertThat(runtimeService.getVariable(process2.id, BPMN_DOCUMENT_REF)).isEqualTo(newRef.toString())

        val instance1 = runtimeService.createProcessInstanceQuery()
            .processInstanceId(process1.id)
            .singleResult()
        val instance2 = runtimeService.createProcessInstanceQuery()
            .processInstanceId(process2.id)
            .singleResult()
        assertThat(instance1.businessKey).isEqualTo(newRef.toString())
        assertThat(instance2.businessKey).isEqualTo(newRef.toString())
    }

    @Test
    fun `RecordRefChangedEvent evicts task DTO cache so documentRef is fresh`() {

        val oldRef = EntityRef.valueOf("emodel/$TYPE_ID@cache-old")
        val newRef = EntityRef.valueOf("emodel/$TYPE_ID@cache-new")

        recordsService.register(
            RecordsDaoBuilder.create("emodel/$TYPE_ID")
                .addRecord("cache-old", TestRecord(type = TYPE_REF))
                .addRecord("cache-new", TestRecord(type = TYPE_REF))
                .build()
        )

        val process = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROC_ID,
                businessKey = oldRef.toString(),
                variables = mapOf(BPMN_DOCUMENT_REF to oldRef.toString())
            )
        )

        val camundaTask = camundaTaskService.createTaskQuery()
            .processInstanceId(process.id)
            .initializeFormKeys()
            .singleResult()
        assertThat(camundaTask).isNotNull

        // Populate the cache — documentRef should be the old ref
        val cachedDto = taskConverter.toProcTask(camundaTask)
        assertThat(cachedDto.documentRef).isEqualTo(oldRef)

        // Fire RecordRefChangedEvent — updates DB and evicts task cache
        bpmnEventHelper.sendRecordRefChangedEvent(
            RecordRefChangedEventDto(
                record = oldRef,
                before = oldRef,
                after = newRef
            )
        )

        // Load again — cache was evicted, so the converter re-reads from Camunda DB
        val refreshedDto = taskConverter.toProcTask(camundaTask)
        assertThat(refreshedDto.documentRef).isEqualTo(newRef)
    }

    class TestRecord(
        @param:AttName("_type")
        val type: EntityRef
    )
}
