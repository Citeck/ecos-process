package ru.citeck.ecos.process.domain.proctask

import org.apache.commons.lang3.time.FastDateFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttributeType
import ru.citeck.ecos.process.domain.saveBpmnWithAction
import ru.citeck.ecos.process.domain.withDocPrefix
import ru.citeck.ecos.process.domain.withDocTypePrefix
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.*

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcTaskSyncedAttsQueryTest {

    companion object {
        private val dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssXXX")

        private const val PROC_ID_SIMPLE_TASK = "bpmn-task-atts-document-simple-task-create"
        private const val DOCUMENT_TYPE = "doc-sync-query"

        private const val TYPE_PROCEDURE = "zv_2"
        private const val TYPE_CODE = "a_1"
        private const val TYPE_URGENCY = 20.0

        private val docRecord = DocRecord()
        private val docRecord2 = DocRecord2()
        private val docRecord3 = DocRecord3()
        private val docRecord4 = DocRecord4()
        private val docRecord5 = DocRecord5()
        private val docRecord6 = DocRecord6()

        private val allDocRefs = listOf(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    private val taskAttsSync = mutableListOf<EntityRef>()
    private val startedProcesIds = mutableListOf<String>()

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var ecosTypeRegistry: EcosTypesRegistry

    private lateinit var documentRecordsDao: InMemRecordsDao<Any>

    private lateinit var typeDef: TypeDef

    @BeforeAll
    fun setUp() {
        saveBpmnWithAction(
            "test/bpmn/$PROC_ID_SIMPLE_TASK.bpmn.xml",
            PROC_ID_SIMPLE_TASK,
            BpmnProcessDefActions.DEPLOY
        )

        documentRecordsDao = RecordsDaoBuilder.create("eproc/$DOCUMENT_TYPE")
            .addRecord(
                docRecord.docRef.getLocalId(),
                docRecord
            )
            .addRecord(
                docRecord2.docRef.getLocalId(),
                docRecord2
            )
            .addRecord(
                docRecord3.docRef.getLocalId(),
                docRecord3
            )
            .addRecord(
                docRecord4.docRef.getLocalId(),
                docRecord4
            )
            .addRecord(
                docRecord5.docRef.getLocalId(),
                docRecord5
            )
            .addRecord(
                docRecord6.docRef.getLocalId(),
                docRecord6
            )
            .build()

        recordsService.register(documentRecordsDao)

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
                                withId("bool")
                                withType(AttributeType.BOOLEAN)
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

        taskAttsSync.add(
            createAttsSync(
                id = "test-task-atts-document-sync-query",
                enabled = true,
                source = TaskAttsSyncSource.RECORD,
                name = "test-task-atts-document-sync-query",
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
                        id = "bool",
                        type = AttributeType.BOOLEAN,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = docRecord.type,
                                attribute = "bool"
                            )
                        )
                    )
                )
            )
        )

        taskAttsSync.add(
            createAttsSync(
                id = "test-task-atts-document-type-sync",
                enabled = true,
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
                    ),
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

            )
        )

        allDocRefs.forEach { docRef ->
            val instance = bpmnProcessService.startProcess(
                StartProcessRequest(
                    "bpmn-task-atts-document-simple-task-create",
                    docRef.toString(),
                    mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        BPMN_DOCUMENT_TYPE to DOCUMENT_TYPE
                    )
                )
            )
            startedProcesIds.add(instance.id)
        }
    }

    @AfterAll
    fun clean() {
        recordsService.delete(taskAttsSync)
        startedProcesIds.forEach {
            bpmnProcessService.deleteProcessInstance(it)
        }
    }

    @Test
    fun `query string equals`() {
        val found = queryTasks(
            Predicates.eq("name".withDocPrefix(), docRecord2.name)
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord2.docRef)
    }

    @Test
    fun `query string contains`() {
        val found = queryTasks(
            Predicates.contains("name".withDocPrefix(), "Doc")
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query string contains should not be case sensitive`() {
        val found = queryTasks(
            Predicates.contains("name".withDocPrefix(), "doc")
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query string like`() {
        val found = queryTasks(
            Predicates.like("name".withDocPrefix(), "Doc")
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query string like should not be case sensitive`() {
        val found = queryTasks(
            Predicates.like("name".withDocPrefix(), "doc")
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query string not equals`() {
        val found = queryTasks(
            Predicates.not(
                Predicates.eq("name".withDocPrefix(), docRecord2.name)
            )
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query string is empty`() {
        val found = queryTasks(
            Predicates.empty("name".withDocPrefix())
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord6.docRef)
    }

    @Test
    fun `query string not empty`() {
        val found = queryTasks(
            Predicates.not(
                Predicates.empty("name".withDocPrefix())
            )
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query string start with`() {
        val found = queryTasks(
            Predicates.like("name".withDocPrefix(), "Doc%")
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query string end with`() {
        val found = queryTasks(
            Predicates.like("name".withDocPrefix(), "%5")
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord5.docRef)
    }

    @Test
    fun `query number equals`() {
        val found = queryTasks(
            Predicates.eq("sum".withDocPrefix(), docRecord2.sum)
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord2.docRef)
    }

    @Test
    fun `query number not equals`() {
        val found = queryTasks(
            Predicates.not(
                Predicates.eq("sum".withDocPrefix(), docRecord2.sum)
            )
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query number is empty`() {
        val found = queryTasks(
            Predicates.empty("sum".withDocPrefix())
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord6.docRef)
    }

    @Test
    fun `query number greater than`() {
        val found = queryTasks(
            Predicates.gt("sum".withDocPrefix(), docRecord3.sum)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query number greater than or equals`() {
        val found = queryTasks(
            Predicates.ge("sum".withDocPrefix(), docRecord3.sum)
        )

        assertThat(found).hasSize(3)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query number less than`() {
        val found = queryTasks(
            Predicates.lt("sum".withDocPrefix(), docRecord3.sum)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef
        )
    }

    @Test
    fun `query number less than or equals`() {
        val found = queryTasks(
            Predicates.le("sum".withDocPrefix(), docRecord3.sum)
        )

        assertThat(found).hasSize(3)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef
        )
    }

    @Test
    fun `query boolean equals true`() {
        val found = queryTasks(
            Predicates.eq("bool".withDocPrefix(), true)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord2.docRef,
            docRecord4.docRef
        )
    }

    @Test
    fun `query boolean equals false`() {
        val found = queryTasks(
            Predicates.eq("bool".withDocPrefix(), false)
        )

        assertThat(found).hasSize(4)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord3.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query date equals`() {
        val found = queryTasks(
            Predicates.eq("date".withDocPrefix(), docRecord2.date)
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord2.docRef)
    }

    @Test
    fun `query date not equals`() {
        val found = queryTasks(
            Predicates.not(
                Predicates.eq("date".withDocPrefix(), docRecord2.date)
            )
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query date is empty`() {
        val found = queryTasks(
            Predicates.empty("date".withDocPrefix())
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord6.docRef)
    }

    @Test
    fun `query date greater than`() {
        val found = queryTasks(
            Predicates.gt("date".withDocPrefix(), docRecord3.date)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query date greater than or equals`() {
        val found = queryTasks(
            Predicates.ge("date".withDocPrefix(), docRecord3.date)
        )

        assertThat(found).hasSize(3)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query date less than`() {
        val found = queryTasks(
            Predicates.lt("date".withDocPrefix(), docRecord3.date)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef
        )
    }

    @Test
    fun `query date less than or equals`() {
        val found = queryTasks(
            Predicates.le("date".withDocPrefix(), docRecord3.date)
        )

        assertThat(found).hasSize(3)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef
        )
    }

    @Test
    fun `query datetime equals`() {
        val found = queryTasks(
            Predicates.eq("dateTime".withDocPrefix(), docRecord2.dateTime)
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord2.docRef)
    }

    @Test
    fun `query datetime not equals`() {
        val found = queryTasks(
            Predicates.not(
                Predicates.eq("dateTime".withDocPrefix(), docRecord2.dateTime)
            )
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query datetime is empty`() {
        val found = queryTasks(
            Predicates.empty("dateTime".withDocPrefix())
        )

        assertThat(found).hasSize(1)
        assertThat(found[0].docRef).isEqualTo(docRecord6.docRef)
    }

    @Test
    fun `query datetime greater than`() {
        val found = queryTasks(
            Predicates.gt("dateTime".withDocPrefix(), docRecord3.dateTime)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query datetime greater than or equals`() {
        val found = queryTasks(
            Predicates.ge("dateTime".withDocPrefix(), docRecord3.dateTime)
        )

        assertThat(found).hasSize(3)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query datetime less than`() {
        val found = queryTasks(
            Predicates.lt("dateTime".withDocPrefix(), docRecord3.dateTime)
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef
        )
    }

    @Test
    fun `query datetime less than or equals`() {
        val found = queryTasks(
            Predicates.le("dateTime".withDocPrefix(), docRecord3.dateTime)
        )

        assertThat(found).hasSize(3)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef
        )
    }

    @Test
    fun `query date time range absolute`() {
        val found = queryTasks(
            Predicates.eq("date".withDocPrefix(), "2021-01-01T00:00:00.0Z/2022-02-03T00:00:00.0Z")
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef
        )
    }

    @Test
    fun `query date time range absolute with time`() {
        val found = queryTasks(
            Predicates.eq("dateTime".withDocPrefix(), "2022-01-01T00:30:15.0Z/2023-04-05T18:30:16.0Z")
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef
        )
    }

    @Test
    fun `query date time range absolute with time and timezone`() {
        val found = queryTasks(
            Predicates.eq("dateTime".withDocPrefix(), "2022-01-01T00:30:15.0Z/2023-04-05T21:30:16.0+03:00")
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef
        )
    }

    @Test
    fun `query date time range relatively`() {
        val found = queryTasks(
            Predicates.eq("date".withDocPrefix(), "-P100000D/\$NOW")
        )

        assertThat(found).hasSize(5)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef
        )
    }

    @Test
    fun `query assoc contains`() {
        val found = queryTasks(
            ValuePredicate.contains(
                "documentAssoc".withDocPrefix(),
                listOf(
                    docRecord2.documentAssoc,
                    docRecord3.documentAssoc
                )
            )

        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord2.docRef,
            docRecord3.docRef
        )
    }

    @Test
    fun `query assoc not contains`() {
        val found = queryTasks(
            Predicates.not(
                ValuePredicate.contains(
                    "documentAssoc".withDocPrefix(),
                    listOf(
                        docRecord2.documentAssoc,
                        docRecord3.documentAssoc
                    )
                )
            )
        )

        assertThat(found).hasSize(4)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query assoc is empty`() {
        val found = queryTasks(
            Predicates.empty("documentAssoc".withDocPrefix())
        )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query assoc is not empty`() {
        val found = queryTasks(
            Predicates.not(
                Predicates.empty("documentAssoc".withDocPrefix())
            )
        )

        assertThat(found).hasSize(4)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef
        )
    }

    @Test
    fun `query string equals on att from type`() {
        val found = queryTasks(
            Predicates.eq("procedure".withDocTypePrefix(), TYPE_PROCEDURE)
        )



        assertThat(found).hasSize(6)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    @Test
    fun `query number equals on att from type`() {
        val found = queryTasks(
            Predicates.eq("urgency".withDocTypePrefix(), TYPE_URGENCY)
        )

        assertThat(found).hasSize(6)
        assertThat(found.map { it.docRef }).containsExactlyInAnyOrder(
            docRecord.docRef,
            docRecord2.docRef,
            docRecord3.docRef,
            docRecord4.docRef,
            docRecord5.docRef,
            docRecord6.docRef
        )
    }

    private fun queryTasks(predicate: Predicate, sortBy: SortBy? = null): List<TaskInfo> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withSortBy(sortBy)
                withPage(QueryPage(10_000, 0, null))
            },
            TaskInfo::class.java
        ).getRecords()
    }

    data class TaskInfo(
        val id: String,

        @AttName("documentRef?id")
        val docRef: EntityRef
    )

    class DocRecord(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@1"),

        @AttName("name")
        val name: String = "Doc 1",

        @AttName("sum")
        val sum: Double = 5_500.0,

        @AttName("date")
        val date: Date = dateFormat.parse("2021-01-01T00:00:00Z"),

        @AttName("dateTime")
        val dateTime: Date = dateFormat.parse("2022-01-01T00:30:15Z"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.valueOf("store/doc@2"),

        @AttName("bool")
        val bool: Boolean = false,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocRecord2(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@2"),

        @AttName("name")
        val name: String = "Doc 2",

        @AttName("sum")
        val sum: Double = 7_000.0,

        @AttName("date")
        val date: Date = dateFormat.parse("2022-02-02T00:00:00Z"),

        @AttName("dateTime")
        val dateTime: Date = dateFormat.parse("2023-04-05T18:30:15Z"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.valueOf("store/doc@3"),

        @AttName("bool")
        val bool: Boolean = true,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocRecord3(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@3"),

        @AttName("name")
        val name: String = "Doc 3",

        @AttName("sum")
        val sum: Double = 8_500.0,

        @AttName("date")
        val date: Date = dateFormat.parse("2022-03-03T00:00:00Z"),

        @AttName("dateTime")
        val dateTime: Date = dateFormat.parse("2023-04-05T20:30:00Z"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.valueOf("store/doc@4"),

        @AttName("bool")
        val bool: Boolean = false,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocRecord4(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@4"),

        @AttName("name")
        val name: String = "Doc 4",

        @AttName("sum")
        val sum: Double = 10_000.0,

        @AttName("date")
        val date: Date = dateFormat.parse("2022-03-03T18:00:00Z"),

        @AttName("dateTime")
        val dateTime: Date = dateFormat.parse("2023-06-08T18:30:15Z"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.valueOf("store/doc@5"),

        @AttName("bool")
        val bool: Boolean = true,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocRecord5(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@5"),

        @AttName("name")
        val name: String = "Doc 5",

        @AttName("sum")
        val sum: Double = 12_500.0,

        @AttName("date")
        val date: Date = dateFormat.parse("2024-05-05T00:00:00Z"),

        @AttName("dateTime")
        val dateTime: Date = dateFormat.parse("2024-02-03T16:32:10Z"),

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.EMPTY,

        @AttName("bool")
        val bool: Boolean = false,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocRecord6(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@6"),

        @AttName("name")
        val name: String? = null,

        @AttName("sum")
        val sum: Double? = null,

        @AttName("date")
        val date: Date? = null,

        @AttName("dateTime")
        val dateTime: Date? = null,

        @AttName("documentAssoc")
        val documentAssoc: EntityRef = EntityRef.EMPTY,

        @AttName("bool")
        val bool: Boolean? = null,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )
}
