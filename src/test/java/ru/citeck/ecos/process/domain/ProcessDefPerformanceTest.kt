package ru.citeck.ecos.process.domain

import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils.getTypeRef
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

private const val REVISION_COUNT = 299
private const val REVISION_TOTAL_COUNT = REVISION_COUNT + 1

@TestMethodOrder(OrderAnnotation::class)
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnProcDefPerformanceTest {

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var procDefRevRepo: ProcDefRevRepository

    @Autowired
    private lateinit var procDefRepo: ProcDefRepository

    @Autowired
    private lateinit var tenantService: ProcTenantService

    private val typeRef = getTypeRef(ProcessDefServiceTest.type0Id)
    private val procDefRef = ProcDefRef.create(BPMN_PROC_TYPE, "test-id")
    private val procDefDataStr = ResourceUtils.getFile("classpath:test/bpmn/large-test-process.bpmn.xml")
        .readText(StandardCharsets.UTF_8)

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @BeforeAll
    fun setUp() {

        val newProcessDefDto = NewProcessDefDto(
            "test-id",
            MLText.EMPTY,
            BPMN_PROC_TYPE,
            "xml",
            "{http://www.citeck.ru/model/content/idocs/1.0}contractor",
            typeRef,
            EntityRef.EMPTY,
            procDefDataStr.toByteArray(StandardCharsets.UTF_8),
            null,
            enabled = true,
            autoStartEnabled = false,
            sectionRef = EntityRef.EMPTY
        )

        procDefService.uploadProcDef(newProcessDefDto)

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
        val found = procDefRevRepo.findAllByProcessDef(procDef)

        assertEquals(REVISION_TOTAL_COUNT, found.size)
    }

    @Test
    @Order(2)
    fun `Delete many revisions should go without a memory leak`() {
        val tenant = tenantService.getCurrent()

        val procDef = procDefRepo.findOneByIdTntAndProcTypeAndExtId(tenant, procDefRef.type, procDefRef.id)!!
        val found = procDefRevRepo.findAllByProcessDef(procDef)

        procDefRevRepo.deleteAll(found)

        val foundAfterDelete = procDefRevRepo.findAllByProcessDef(procDef)

        assertEquals(0, foundAfterDelete.size)
    }
}
