package ru.citeck.ecos.process.domain.bpmn

import org.apache.commons.lang3.time.FastDateFormat
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.createAttsSync
import ru.citeck.ecos.process.domain.proctask.attssync.*
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_ATTS
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.process.domain.saveBpmnWithAction
import ru.citeck.ecos.process.domain.withDocPrefix
import ru.citeck.ecos.process.domain.withDocTypePrefix
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class UserTaskAttsSyncTest {

    companion object {
        private const val PROC_ID_SIMPLE_TASK = "bpmn-task-atts-document-simple-task-create"
        private const val DOCUMENT_TYPE = "documentSync"
        private const val DOCUMENT_CHILD_TYPE = "documentSyncChild"

        private val docRecord = DocRecord()
        private val docRecord2 = DocRecord2()
        private val modifiedDocRecord = DocRecordModified()
        private val docRecordChild = DocRecordChild()

        private val docRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@1")
        private val docRef2 = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@2")
        private val docRefChild = EntityRef.valueOf("eproc/$DOCUMENT_CHILD_TYPE@1")

        private val variables_docRef = mapOf(
            BPMN_DOCUMENT_REF to docRef.toString(),
            BPMN_DOCUMENT_TYPE to DOCUMENT_TYPE
        )

        private const val TYPE_PROCEDURE = "zv_2"
        private const val TYPE_CODE = "a_1"
        private const val TYPE_URGENCY = 20.0
        private const val TYPE_URGENCY_UPDATED = 30.0
    }

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var procTaskService: ProcTaskService

    @Autowired
    private lateinit var bpmnEventHelper: BpmnEventHelper

    @Autowired
    private lateinit var procTaskAttsSyncService: ProcTaskAttsSyncService

    @Autowired
    private lateinit var metaDataChangeTaskAttsSyncListener: MetaDataChangeTaskAttsSyncListener

    @Autowired
    private lateinit var ecosTypeRegistry: EcosTypesRegistry

    private lateinit var documentRecordsDao: InMemRecordsDao<Any>
    private lateinit var childDocumentRecordsDao: InMemRecordsDao<Any>

    private val taskAttsSync = mutableListOf<EntityRef>()
    private val startedProcesIds = mutableListOf<String>()

    private lateinit var typeDef: TypeDef

    private val allDocumentAtts = listOf(
        "sum",
        "name",
        "description",
        "attToCheckUpdateOnChangeSettings",
        "date",
        "dateTime",
        "documentAssoc",
        "new",
        "attWithoutSync"
    )

    @BeforeEach
    fun setUp() {
        saveBpmnWithAction(
            "test/bpmn/$PROC_ID_SIMPLE_TASK.bpmn.xml",
            PROC_ID_SIMPLE_TASK,
            BpmnProcessDefActions.DEPLOY
        )

        documentRecordsDao = RecordsDaoBuilder.create("eproc/$DOCUMENT_TYPE")
            .addRecord(
                docRef.getLocalId(),
                docRecord
            )
            .addRecord(
                docRef2.getLocalId(),
                docRecord2
            )
            .build()

        childDocumentRecordsDao = RecordsDaoBuilder.create("eproc/$DOCUMENT_CHILD_TYPE")
            .addRecord(
                docRefChild.getLocalId(),
                docRecordChild
            )
            .build()

        recordsService.register(documentRecordsDao)
        recordsService.register(childDocumentRecordsDao)

        typeDef = TypeDef.create()
            .withId(DOCUMENT_TYPE)
            .withModel(
                TypeModelDef.create {
                    withAttributes(
                        listOf(
                            AttributeDef.create {
                                withId("sum")
                                withType(AttributeType.NUMBER)
                            },
                            AttributeDef.create {
                                withId("name")
                                withType(AttributeType.TEXT)
                            },
                            AttributeDef.create {
                                withId("description")
                                withType(AttributeType.TEXT)
                            },
                            AttributeDef.create {
                                withId("attToCheckUpdateOnChangeSettings")
                                withType(AttributeType.TEXT)
                            },
                            AttributeDef.create {
                                withId("date")
                                withType(AttributeType.DATE)
                            },
                            AttributeDef.create {
                                withId("dateTime")
                                withType(AttributeType.DATETIME)
                            },
                            AttributeDef.create {
                                withId("documentAssoc")
                                withType(AttributeType.ASSOC)
                            },
                            AttributeDef.create {
                                withId("new")
                                withType(AttributeType.BOOLEAN)
                            },
                            AttributeDef.create {
                                withId("attWithoutSync")
                                withType(AttributeType.TEXT)
                            }
                        )
                    )
                }
            )
            .withConfig(
                ObjectData.create(
                    mapOf(
                        "procedure" to TYPE_PROCEDURE,
                        "urgency" to TYPE_URGENCY,
                        "code" to TYPE_CODE
                    )
                )
            )
            .build()
        ecosTypeRegistry.setValue(
            DOCUMENT_TYPE,
            typeDef
        )

        val childTypeDef = TypeDef.create()
            .withId(DOCUMENT_CHILD_TYPE)
            .withParent(docRecord.type)
            .withModel(
                TypeModelDef.create {
                    withAttributes(
                        listOf(
                            AttributeDef.create {
                                withId("name")
                                withType(AttributeType.TEXT)
                            }
                        )
                    )
                }
            )
            .build()
        ecosTypeRegistry.setValue(
            DOCUMENT_CHILD_TYPE,
            childTypeDef
        )
    }

    @AfterEach
    fun clean() {
        recordsService.delete(taskAttsSync)
        startedProcesIds.forEach {
            bpmnProcessService.deleteProcessInstance(it)
        }
    }

    @Test
    fun `create task should fill specified document atts`() {
        createSyncSettings()
        startProcess()

        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        val sum = procTaskService.getVariableLocal(taskId, "sum".withDocPrefix()) as Double
        assertThat(sum).isEqualTo(docRecord.sum)

        val name = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
        assertThat(name).isEqualTo(docRecord.name)

        val description = procTaskService.getVariableLocal(taskId, "description".withDocPrefix()) as String
        assertThat(description).isEqualTo(docRecord.description)

        val date = procTaskService.getVariableLocal(taskId, "date".withDocPrefix()) as Date
        assertThat(date).isEqualTo(docRecord.date)

        val dateTime = procTaskService.getVariableLocal(taskId, "dateTime".withDocPrefix()) as Date
        assertThat(dateTime).isEqualTo(docRecord.dateTime)

        val documentAssoc = procTaskService.getVariableLocal(taskId, "documentAssoc".withDocPrefix()) as String
        assertThat(documentAssoc.toEntityRef()).isEqualTo(docRecord.documentAssoc)

        val new = procTaskService.getVariableLocal(taskId, "new".withDocPrefix()) as Boolean
        assertThat(new).isEqualTo(docRecord.new)

        val attWithoutSync = procTaskService.getVariableLocal(taskId, "attWithoutSync".withDocPrefix())
        assertThat(attWithoutSync).isNull()

        val typeProcedure = procTaskService.getVariableLocal(taskId, "procedure".withDocTypePrefix()) as String
        assertThat(typeProcedure).isEqualTo(TYPE_PROCEDURE)

        val typeUrgency = procTaskService.getVariableLocal(taskId, "urgency".withDocTypePrefix()) as Double
        assertThat(typeUrgency).isEqualTo(TYPE_URGENCY)

        val typeCode = procTaskService.getVariablesLocal(taskId)["code".withDocTypePrefix()]
        assertThat(typeCode).isNull()
    }

    @Test
    fun `create task with disabled sync settings should not fill document atts`() {
        createSyncSettings(enabled = false)
        startProcess()

        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        val variables = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variables).isEmpty()
    }

    @Test
    fun `change flag to enabled should fill document atts`() {
        createSyncSettings(enabled = false)
        startProcess()

        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        val variablesBefore = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variablesBefore).isEmpty()

        val syncSettingsRef = taskAttsSync[0]
        val atts = RecordAtts(syncSettingsRef)
        atts["enabled"] = true
        recordsService.mutate(atts)

        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val variablesAfter = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
            assertThat(variablesAfter).isNotEmpty()
        }
    }

    @Test
    fun `create sync settings should fill document atts`() {
        startProcess()

        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        val variables = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variables).isEmpty()

        createSyncSettings()

        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val variablesAfter = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
            assertThat(variablesAfter).isNotEmpty()
        }
    }

    @Test
    fun `changed synced document attributes should be updated on tasks`() {
        createSyncSettings()
        metaDataChangeTaskAttsSyncListener.updateListeners()
        startProcess()

        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // check initial value, that should be written on task create
        val sum = procTaskService.getVariableLocal(taskId, "sum".withDocPrefix()) as Double
        assertThat(sum).isEqualTo(docRecord.sum)

        val name = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
        assertThat(name).isEqualTo(docRecord.name)

        val description = procTaskService.getVariableLocal(taskId, "description".withDocPrefix()) as String
        assertThat(description).isEqualTo(docRecord.description)

        val date = procTaskService.getVariableLocal(taskId, "date".withDocPrefix()) as Date
        assertThat(date).isEqualTo(docRecord.date)

        val dateTime = procTaskService.getVariableLocal(taskId, "dateTime".withDocPrefix()) as Date
        assertThat(dateTime).isEqualTo(docRecord.dateTime)

        val documentAssoc = procTaskService.getVariableLocal(taskId, "documentAssoc".withDocPrefix()) as String
        assertThat(documentAssoc.toEntityRef()).isEqualTo(docRecord.documentAssoc)

        val new = procTaskService.getVariableLocal(taskId, "new".withDocPrefix()) as Boolean
        assertThat(new).isEqualTo(docRecord.new)

        val attWithoutSync = procTaskService.getVariableLocal(taskId, "attWithoutSync".withDocPrefix())
        assertThat(attWithoutSync).isNull()

        // update document
        documentRecordsDao.setRecord(
            docRef.getLocalId(),
            modifiedDocRecord
        )
        bpmnEventHelper.sendRecordChangedEvent(
            RecordChangedEventDto(
                record = docRef,
                diff = Diff(
                    listOf(
                        ChangedValue("attWithoutSync"),
                        ChangedValue("sum"),
                        ChangedValue("date"),
                        ChangedValue("dateTime"),
                        ChangedValue("documentAssoc"),
                        ChangedValue("new"),
                        ChangedValue("description")
                    )
                ),
                after = mapOf(
                    "attWithoutSync" to modifiedDocRecord.attWithoutSync,
                    "sum" to modifiedDocRecord.sum,
                    "date" to modifiedDocRecord.date,
                    "dateTime" to modifiedDocRecord.dateTime,
                    "documentAssoc" to modifiedDocRecord.documentAssoc,
                    "new" to modifiedDocRecord.new,
                    "description" to modifiedDocRecord.description
                ),
                typeDef = ecosTypeRegistry.getTypeInfo(docRecord.type)
            )
        )

        // check values after update
        val attWithoutSyncAfterUpdate = procTaskService.getVariableLocal(taskId, "attWithoutSync".withDocPrefix())
        assertThat(attWithoutSyncAfterUpdate).isNull()

        val sumModified = procTaskService.getVariableLocal(taskId, "sum".withDocPrefix()) as Double
        assertThat(sumModified).isEqualTo(modifiedDocRecord.sum)

        val dateModified = procTaskService.getVariableLocal(taskId, "date".withDocPrefix()) as Date
        assertThat(dateModified).isEqualTo(modifiedDocRecord.date)

        val dateTimeModified = procTaskService.getVariableLocal(taskId, "dateTime".withDocPrefix()) as Date
        assertThat(dateTimeModified).isEqualTo(modifiedDocRecord.dateTime)

        val documentAssocModified = procTaskService.getVariableLocal(taskId, "documentAssoc".withDocPrefix()) as String
        assertThat(documentAssocModified.toEntityRef()).isEqualTo(modifiedDocRecord.documentAssoc)

        val newModified = procTaskService.getVariableLocal(taskId, "new".withDocPrefix()) as Boolean
        assertThat(newModified).isEqualTo(modifiedDocRecord.new)

        val nameNotModified = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
        assertThat(nameNotModified).isEqualTo(docRecord.name)

        val descriptionModifiedToNull = procTaskService.getVariableLocal(taskId, "description".withDocPrefix())
        assertThat(descriptionModifiedToNull).isNull()

        // revert document dao to initial state
        documentRecordsDao.setRecord(
            docRef.getLocalId(),
            docRecord
        )
    }

    @Test
    fun `changed synced document attributes of disabled settings should not be updated tasks`() {
        createSyncSettings(enabled = false)
        metaDataChangeTaskAttsSyncListener.updateListeners()
        startProcess()

        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // update document
        documentRecordsDao.setRecord(
            docRef.getLocalId(),
            modifiedDocRecord
        )
        bpmnEventHelper.sendRecordChangedEvent(
            RecordChangedEventDto(
                record = docRef,
                diff = Diff(
                    listOf(
                        ChangedValue("attWithoutSync"),
                        ChangedValue("sum"),
                        ChangedValue("date"),
                        ChangedValue("dateTime"),
                        ChangedValue("documentAssoc"),
                        ChangedValue("new"),
                        ChangedValue("description")
                    )
                ),
                after = mapOf(
                    "attWithoutSync" to modifiedDocRecord.attWithoutSync,
                    "sum" to modifiedDocRecord.sum,
                    "date" to modifiedDocRecord.date,
                    "dateTime" to modifiedDocRecord.dateTime,
                    "documentAssoc" to modifiedDocRecord.documentAssoc,
                    "new" to modifiedDocRecord.new,
                    "description" to modifiedDocRecord.description
                ),
                typeDef = ecosTypeRegistry.getTypeInfo(docRecord.type)
            )
        )

        // check values after update
        val variables = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variables).isEmpty()

        // revert document dao to initial state
        documentRecordsDao.setRecord(
            docRef.getLocalId(),
            docRecord
        )
    }

    @Test
    fun `change sync settings attributes should update task atts`() {
        createSyncSettings()
        startProcess()

        val syncSettingsRef = taskAttsSync[0]
        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // check initial value
        val att = procTaskService.getVariableLocal(taskId, "attToCheckUpdateOnChangeSettings".withDocPrefix())
        assertThat(att).isNull()

        // add attribute to sync settings
        val currentAttsSettings =
            procTaskAttsSyncService.getSyncSettings(syncSettingsRef) ?: error("Sync settings not found")
        val attsSync = currentAttsSettings.attributesSync.toMutableSet()
        attsSync.add(
            TaskSyncAttribute(
                id = "attToCheckUpdateOnChangeSettings",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = docRecord.type,
                        attribute = "attToCheckUpdateOnChangeSettings"
                    )
                )
            )
        )

        val atts = RecordAtts(syncSettingsRef)
        atts[PROC_TASK_ATTS_SYNC_ATTS] = attsSync
        recordsService.mutate(atts)

        // check new attribute value
        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val checkedAtt =
                procTaskService.getVariableLocal(taskId, "attToCheckUpdateOnChangeSettings".withDocPrefix())
            assertThat(checkedAtt).isEqualTo(docRecord.attToCheckUpdateOnChangeSettings)
        }
    }

    @Test
    fun `change sync settings attributes on disabled settings should not update task atts`() {
        createSyncSettings(enabled = false)
        startProcess()

        val syncSettingsRef = taskAttsSync[0]
        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // add attribute to sync settings
        val currentAttsSettings =
            procTaskAttsSyncService.getSyncSettings(syncSettingsRef) ?: error("Sync settings not found")
        val attsSync = currentAttsSettings.attributesSync.toMutableSet()
        attsSync.add(
            TaskSyncAttribute(
                id = "attToCheckUpdateOnChangeSettings",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = docRecord.type,
                        attribute = "attToCheckUpdateOnChangeSettings"
                    )
                )
            )
        )

        val atts = RecordAtts(syncSettingsRef)
        atts[PROC_TASK_ATTS_SYNC_ATTS] = attsSync
        recordsService.mutate(atts)

        // check after update
        val variables = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variables).isEmpty()
    }

    @Test
    fun `change sync settings type attributes should update task atts`() {
        createSyncSettings()
        startProcess()

        val syncSettingsRef = taskAttsSync[1]
        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // check initial value
        val procedure = procTaskService.getVariableLocal(taskId, "code".withDocTypePrefix())
        assertThat(procedure).isNull()

        // add type attribute to sync settings
        val currentAttsSettings =
            procTaskAttsSyncService.getSyncSettings(syncSettingsRef) ?: error("Sync settings not found")
        val attsSync = currentAttsSettings.attributesSync.toMutableSet()
        attsSync.add(
            TaskSyncAttribute(
                id = "code",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = docRecord.type,
                        recordExpressionAttribute = "config.code"
                    )
                )
            )
        )

        val atts = RecordAtts(syncSettingsRef)
        atts[PROC_TASK_ATTS_SYNC_ATTS] = attsSync
        recordsService.mutate(atts)

        // check new attribute value
        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val code = procTaskService.getVariableLocal(taskId, "code".withDocTypePrefix())
            assertThat(code).isEqualTo(TYPE_CODE)
        }
    }

    @Test
    fun `change sync settings type attributes on disabled settings should not update task atts`() {
        createSyncSettings(enabled = false)
        startProcess()

        val syncSettingsRef = taskAttsSync[1]
        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // add type attribute to sync settings
        val currentAttsSettings =
            procTaskAttsSyncService.getSyncSettings(syncSettingsRef) ?: error("Sync settings not found")
        val attsSync = currentAttsSettings.attributesSync.toMutableSet()
        attsSync.add(
            TaskSyncAttribute(
                id = "code",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = docRecord.type,
                        recordExpressionAttribute = "config.code"
                    )
                )
            )
        )

        val atts = RecordAtts(syncSettingsRef)
        atts[PROC_TASK_ATTS_SYNC_ATTS] = attsSync
        recordsService.mutate(atts)

        // check after update
        val variables = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variables).isEmpty()

        val code = procTaskService.getVariableLocal(taskId, "code".withDocTypePrefix())
        assertThat(code).isNull()
    }

    @Test
    fun `change type should update task atts`() {
        val typeRef = "emodel/type@$DOCUMENT_TYPE".toEntityRef()
        bpmnEventHelper.sendRecordChangedEvent(
            RecordChangedEventDto(
                record = typeRef,
                diff = Diff(emptyList())
            )
        )

        createSyncSettings()
        startProcess()


        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // check initial value
        val typeUrgency = procTaskService.getVariableLocal(taskId, "urgency".withDocTypePrefix())
        assertThat(typeUrgency).isEqualTo(TYPE_URGENCY)

        // update urgency on type
        ecosTypeRegistry.setValue(
            DOCUMENT_TYPE,
            typeDef.copy(
                config = ObjectData.create(
                    mapOf(
                        "procedure" to TYPE_PROCEDURE,
                        "urgency" to TYPE_URGENCY_UPDATED,
                        "code" to TYPE_CODE
                    )
                )
            )
        )

        bpmnEventHelper.sendRecordChangedEvent(
            RecordChangedEventDto(
                record = typeRef,
                diff = Diff(emptyList())
            )
        )

        // check new attribute value
        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val updatedTypeUrgency = procTaskService.getVariableLocal(taskId, "urgency".withDocTypePrefix()) as Double
            assertThat(updatedTypeUrgency).isEqualTo(TYPE_URGENCY_UPDATED)
        }
    }

    @Test
    fun `change type on disabled settings should not update task atts`() {
        createSyncSettings(enabled = false)
        startProcess()

        val typeRef = "emodel/type@$DOCUMENT_TYPE".toEntityRef()
        val taskId = procTaskService.getTasksByDocument(docRef.toString())[0].id

        // update urgency on type
        ecosTypeRegistry.setValue(
            DOCUMENT_TYPE,
            typeDef.copy(
                config = ObjectData.create(
                    mapOf(
                        "procedure" to TYPE_PROCEDURE,
                        "urgency" to TYPE_URGENCY_UPDATED,
                        "code" to TYPE_CODE
                    )
                )
            )
        )

        bpmnEventHelper.sendRecordChangedEvent(
            RecordChangedEventDto(
                record = typeRef,
                diff = Diff(emptyList())
            )
        )

        // check after update
        val variables = procTaskService.getVariablesLocal(taskId, allDocumentAtts.map { it.withDocPrefix() })
        assertThat(variables).isEmpty()

        val updatedTypeUrgency = procTaskService.getVariableLocal(taskId, "urgency".withDocTypePrefix())
        assertThat(updatedTypeUrgency).isNull()
    }

    @Test
    fun `child with create task should fill specified document atts`() {
        createSyncSettings()
        startProcessForChild()

        val taskId = procTaskService.getTasksByDocument(docRefChild.toString())[0].id

        val name = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
        assertThat(name).isEqualTo(docRecordChild.name)
    }

    @Test
    fun `child with change flag to enabled should fill document atts`() {
        createSyncSettings(enabled = false)
        startProcessForChild()

        val taskId = procTaskService.getTasksByDocument(docRefChild.toString())[0].id

        val nameBefore = procTaskService.getVariableLocal(taskId, "name".withDocPrefix())
        assertThat(nameBefore).isNull()

        val syncSettingsRef = taskAttsSync[0]
        val atts = RecordAtts(syncSettingsRef)
        atts["enabled"] = true
        recordsService.mutate(atts)

        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val nameAfter = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
            assertThat(nameAfter).isEqualTo(docRecordChild.name)
        }
    }

    @Test
    fun `child with create sync settings should fill document atts`() {
        startProcessForChild()

        val taskId = procTaskService.getTasksByDocument(docRefChild.toString())[0].id

        val variables = procTaskService.getVariablesLocal(taskId, listOf("name".withDocPrefix()))
        assertThat(variables).isEmpty()

        createSyncSettings()

        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            val variablesAfter = procTaskService.getVariablesLocal(taskId, listOf("name".withDocPrefix()))
            assertThat(variablesAfter).isNotEmpty()
        }
    }

    @Test
    fun `child with changed synced document attributes should be updated on tasks`() {
        createSyncSettings()
        metaDataChangeTaskAttsSyncListener.updateListeners()
        startProcessForChild()

        val taskId = procTaskService.getTasksByDocument(docRefChild.toString())[0].id

        // check initial value, that should be written on task create
        val name = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
        assertThat(name).isEqualTo(docRecordChild.name)

        // update document
        childDocumentRecordsDao.setRecord(
            docRefChild.getLocalId(),
            DocRecordChild(name = "Doc 1 child updated")
        )
        bpmnEventHelper.sendRecordChangedEvent(
            RecordChangedEventDto(
                record = docRefChild,
                diff = Diff(
                    listOf(
                        ChangedValue("name")
                    )
                ),
                after = mapOf(
                    "name" to "Doc 1 child updated"
                ),
                typeDef = ecosTypeRegistry.getTypeInfo(docRecordChild.type)
            )
        )

        // check values after update
        val nameUpdated = procTaskService.getVariableLocal(taskId, "name".withDocPrefix()) as String
        assertThat(nameUpdated).isEqualTo("Doc 1 child updated")

        // revert document dao to initial state
        childDocumentRecordsDao.setRecord(
            docRefChild.getLocalId(),
            docRecordChild
        )
    }

    private fun createSyncSettings(enabled: Boolean = true) {
        taskAttsSync.add(
            createAttsSync(
                id = "test-task-atts-document-record-sync",
                enabled = enabled,
                source = TaskAttsSyncSource.RECORD,
                name = "test-task-atts-document-sync",
                attributesSync = listOf(
                    TaskSyncAttribute(
                        id = "name",
                        type = AttributeType.TEXT,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "name"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "description",
                        type = AttributeType.TEXT,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "description"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "sum",
                        type = AttributeType.NUMBER,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "sum"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "date",
                        type = AttributeType.DATE,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "date"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "dateTime",
                        type = AttributeType.DATETIME,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "dateTime"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "documentAssoc",
                        type = AttributeType.ASSOC,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "documentAssoc"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "new",
                        type = AttributeType.BOOLEAN,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "new"
                            )
                        )
                    )
                )
            )
        )

        taskAttsSync.add(
            createAttsSync(
                id = "test-task-atts-document-type-sync",
                enabled = enabled,
                source = TaskAttsSyncSource.TYPE,
                name = "test-task-atts-document-type-sync",
                attributesSync = listOf(
                    TaskSyncAttribute(
                        id = "procedure",
                        type = AttributeType.TEXT,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                recordExpressionAttribute = "config.procedure"
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "urgency",
                        type = AttributeType.NUMBER,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                recordExpressionAttribute = "config.urgency?num"
                            )
                        )
                    )
                )
            )
        )
    }

    class DocRecord(

        @AttName("name")
        val name: String = "Doc 1",

        @AttName("description")
        val description: String? = "Description of document 1",

        @AttName("attWithoutSync")
        val attWithoutSync: String = "attWithoutSync",

        @AttName("attToCheckUpdateOnChangeSettings")
        val attToCheckUpdateOnChangeSettings: String = "attToCheckUpdateOnChangeSettings",

        @AttName("sum")
        val sum: Double = 5_500.0,

        @AttName("date")
        val date: Date = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss").parse("2021-01-01T00:00:00"),

        @AttName("dateTime")
        val dateTime: Date = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss").parse("2023-04-05T18:30:15"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.valueOf("store/doc@1"),

        @AttName("new")
        val new: Boolean = true,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocRecordChild(
        @AttName("name")
        val name: String = "Doc 1 child",

        @AttName("parents")
        val parent: List<EntityRef> = listOf(docRecord.type),

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_CHILD_TYPE")
    )

    class DocRecordModified(

        @AttName("name")
        val name: String = "Doc 1",

        @AttName("description")
        val description: String? = null,

        @AttName("attWithoutSync")
        val attWithoutSync: String = "attWithoutSync",

        @AttName("sum")
        val sum: Double = 9_500.0,

        @AttName("date")
        val date: Date = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss").parse("2021-01-01T15:00:00"),

        @AttName("dateTime")
        val dateTime: Date = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss").parse("2023-04-05T13:30:15"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.valueOf("store/doc@1"),

        @AttName("new")
        val new: Boolean = false,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")

    )

    class DocRecord2(

        @AttName("name")
        val name: String = "Doc 2",

        @AttName("sum")
        val sum: Double = 13_000.0,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    private fun startProcess() {
        val instance = bpmnProcessService.startProcess(
            StartProcessRequest(
                "bpmn-task-atts-document-simple-task-create",
                docRef.toString(),
                variables_docRef
            )
        )
        startedProcesIds.add(instance.id)
    }

    private fun startProcessForChild() {
        val instance = bpmnProcessService.startProcess(
            StartProcessRequest(
                "bpmn-task-atts-document-simple-task-create",
                docRefChild.toString(),
                mapOf(
                    BPMN_DOCUMENT_REF to docRefChild.toString(),
                    BPMN_DOCUMENT_TYPE to DOCUMENT_CHILD_TYPE
                )
            )
        )
        startedProcesIds.add(instance.id)
    }
}
