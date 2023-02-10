// TOOD: uncomment

package ru.citeck.ecos.process.domain.procdef

/*
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefVersionRecords
import ru.citeck.ecos.process.domain.bpmn.api.records.VersionQuery
import ru.citeck.ecos.process.domain.getBpmnProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef.Companion.create
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

private const val REVISION_COUNT = 299
private const val REVISION_TOTAL_COUNT = REVISION_COUNT + 1

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessDefPerformanceTest {

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var procDefRevRepo: ProcDefRevRepository

    @Autowired
    private lateinit var procDefRepo: ProcDefRepository

    @Autowired
    private lateinit var tenantService: ProcTenantService

    @Autowired
    private lateinit var recordsService: RecordsService

    private val procDefRef = ProcDefRef.create(BPMN_PROC_TYPE, "test-id")
    private val procDefDataStr = ResourceUtils.getFile("classpath:test/bpmn/large-test-process.bpmn.xml")
        .readText(StandardCharsets.UTF_8)

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @BeforeAll
    fun setUp() {
        val definitions = procDefService.findAll(VoidPredicate.INSTANCE, Int.MAX_VALUE, 0)

        definitions.forEach(
            Consumer { d: ProcDefDto ->
                procDefService.delete(
                    create(
                        d.procType,
                        d.id
                    )
                )
            }
        )

        procDefService.uploadProcDef(
            getBpmnProcessDefDto(
                "test/bpmn/large-test-process.bpmn.xml",
                "test-id"
            )
        )

        val processDefById = procDefService.getProcessDefById(procDefRef)!!

        for (i in 0 until REVISION_COUNT) {
            processDefById.data = procDefDataStr.replace("onFamiliarization", "onFamiliarization$i")
                .toByteArray(StandardCharsets.UTF_8)

            log.info { "Create rev: $i" }
            procDefService.uploadNewRev(processDefById)
        }
    }

    @Test
    @Order(1)
    fun `Getting many revisions should go without a memory leak`() {
        val tenant = tenantService.getCurrent()

        val procDef = procDefRepo.findOneByIdTntAndProcTypeAndExtId(tenant, procDefRef.type, procDefRef.id)!!
        val foundRevisions = procDefRevRepo.findAllByProcessDefWithoutData(procDef)
        val foundRevisionsDto = procDefService.getProcessDefRevs(procDefRef)

        assertEquals(REVISION_TOTAL_COUNT, foundRevisions.size)
        assertEquals(REVISION_TOTAL_COUNT, foundRevisionsDto.size)
    }

    @Test
    @Order(2)
    fun `Getting many revisions from version records without data att should go without a memory leak`() {
        val allRecords = recordsService.query(
            RecordsQuery.create {
                withSourceId(BpmnProcDefVersionRecords.ID)
                withQuery(
                    VersionQuery(
                        EntityRef.create(BpmnProcDefVersionRecords.ID, procDefRef.id),
                    )
                )
                withMaxItems(Int.MAX_VALUE)
            },
            mapOf(
                "format" to "format",
                "modified" to "modified"
            )
        ).getRecords()

        assertEquals(REVISION_TOTAL_COUNT, allRecords.size)
    }

    @Test
    @Order(3)
    fun `Delete many revisions should go without a memory leak`() {
        val tenant = tenantService.getCurrent()

        val procDef = procDefRepo.findOneByIdTntAndProcTypeAndExtId(tenant, procDefRef.type, procDefRef.id)!!

        procDefService.delete(procDefRef)

        val foundRevisionsAfterDelete = procDefRevRepo.findAllByProcessDefWithoutData(procDef)

        assertEquals(0, foundRevisionsAfterDelete.size)
    }
}*/
