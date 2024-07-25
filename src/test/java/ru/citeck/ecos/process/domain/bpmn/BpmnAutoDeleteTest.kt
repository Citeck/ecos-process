package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
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
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
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
class BpmnAutoDeleteTest {

    @Autowired
    private lateinit var typesRegistry: EcosTypesRegistry

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var bpmnEventHelper: BpmnEventHelper

    @Autowired
    private lateinit var helper: BpmnProcHelper

    companion object {
        private val testType1Id = "test-type-1"
        private val testType2Id = "test-type-2"
        private val testType1Record = TestTypeRecord(type = EntityRef.valueOf("emodel/type@$testType1Id"))
        private val testType2Record = TestTypeRecord(type = EntityRef.valueOf("emodel/type@$testType2Id"))
        private val testType1Ref = EntityRef.valueOf("emodel/$testType1Id@1")
        private val testType2Ref = EntityRef.valueOf("emodel/$testType2Id@2")
    }

    @BeforeAll
    fun setUp() {
        typesRegistry.setValue(
            testType1Id,
            TypeDef.create {
                withId(testType1Id)
                withParentRef(ModelUtils.getTypeRef("user-base"))
            }
        )

        typesRegistry.setValue(
            testType2Id,
            TypeDef.create {
                withId(testType2Id)
                withParentRef(ModelUtils.getTypeRef("user-base"))
            }
        )

        recordsService.register(
            RecordsDaoBuilder.create("emodel/$testType1Id")
                .addRecord(
                    testType1Ref.getLocalId(),
                    testType1Record
                )
                .build()
        )
        recordsService.register(
            RecordsDaoBuilder.create("emodel/$testType2Id")
                .addRecord(
                    testType2Ref.getLocalId(),
                    testType2Record
                )
                .build()
        )
    }

    @Test
    fun `test process autoDelete on`() {
        val procId = "bpmn-auto-delete-true-test"

        helper.saveBpmnWithAction(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.DEPLOY
        )

        val startedProcess = bpmnProcessService.startProcess(
            StartProcessRequest(
                procId,
                testType1Ref.toString(),
                emptyMap()
            )
        )
        val processList = bpmnProcessService.getProcessInstancesForBusinessKey(testType1Ref.toString()).map {
            it.id
        }
        assertThat(processList).hasSize(1)
        assertThat(processList).contains(startedProcess.id)

        bpmnEventHelper.sendRecordDeletedEvent(
            RecordDeletedEventDto(testType1Ref)
        )

        val newProcessList = bpmnProcessService.getProcessInstancesForBusinessKey(testType1Ref.toString())
        assertThat(newProcessList).hasSize(0)
    }

    @Test
    fun `test process autoDelete off`() {
        val procId = "bpmn-auto-delete-false-test"

        helper.saveBpmnWithAction(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.DEPLOY
        )

        val startedProcess = bpmnProcessService.startProcess(
            StartProcessRequest(
                procId,
                testType2Ref.toString(),
                emptyMap()
            )
        )
        val processList = bpmnProcessService.getProcessInstancesForBusinessKey(testType2Ref.toString()).map {
            it.id
        }
        assertThat(processList).hasSize(1)
        assertThat(processList).contains(startedProcess.id)

        bpmnEventHelper.sendRecordDeletedEvent(
            RecordDeletedEventDto(testType2Ref)
        )

        val newProcessList = bpmnProcessService.getProcessInstancesForBusinessKey(testType2Ref.toString()).map {
            it.id
        }
        assertThat(newProcessList).hasSize(1)
        assertThat(newProcessList).contains(startedProcess.id)
    }

    class TestTypeRecord(
        @AttName("test")
        val test: String = "test",

        @AttName("_type")
        val type: EntityRef,

        @AttName("_isDraft")
        val isDraft: Boolean = false
    )
}
