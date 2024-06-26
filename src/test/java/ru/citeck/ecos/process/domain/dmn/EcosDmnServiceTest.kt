package ru.citeck.ecos.process.domain.dmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.dmn.service.EcosDmnService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class EcosDmnServiceTest {

    @Autowired
    private lateinit var ecosDmnService: EcosDmnService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Test
    fun `evaluate one decision one variable as collect map entries`() {
        val procId = "simple-dmn-test"
        helper.saveAndDeployDmnFromResource("test/dmn/$procId.dmn.xml", procId)

        val result = ecosDmnService.evaluateDecisionByKeyAndCollectMapEntries(
            "Decision_simple-dmn",
            mapOf(
                "color" to "red"
            )
        )

        assertThat(result).isEqualTo(
            mapOf(
                "result" to listOf("stop")
            )
        )
    }

    @Test
    fun `evaluate required decision with result lis as collect map entries`() {
        val procId = "dmn-test-multiple-input-expression"
        helper.saveAndDeployDmnFromResource("test/dmn/$procId.dmn.xml", procId)

        val result = ecosDmnService.evaluateDecisionByKeyAndCollectMapEntries(
            "Decision_dish_beverages",
            mapOf(
                "season" to "Spring",
                "guestCount" to 10,
                "guestsWithChildren" to true
            )
        )

        assertThat(result).isEqualTo(
            mapOf(
                "beverages" to listOf("Guiness", "Apple Juice")
            )
        )
    }
}
