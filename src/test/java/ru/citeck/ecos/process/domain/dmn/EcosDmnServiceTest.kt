package ru.citeck.ecos.process.domain.dmn

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.camunda.bpm.engine.ProcessEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.PolyglotContexts
import ru.citeck.ecos.process.domain.dmn.service.EcosDmnService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.concurrent.TimeUnit

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class EcosDmnServiceTest {

    @Autowired
    private lateinit var ecosDmnService: EcosDmnService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var processEngine: ProcessEngine

    @Test
    fun `a javascript decision table evaluates and leaves no polyglot context behind`() {
        // camunda's DMN resolves a ScriptEngine itself and builds its own bindings, so this path never reaches
        // EcosScriptingEnvironment and used to leak two contexts per evaluation
        val procId = "dmn-javascript-test"
        helper.saveAndDeployDmnFromResource("test/dmn/$procId.dmn.xml", procId)
        val evaluate = {
            ecosDmnService.evaluateDecisionByKeyAndCollectMapEntries(
                "Decision_dmn_javascript",
                mapOf("color" to "green")
            )
        }

        // baseline BEFORE the first evaluation on purpose: taking it after would hide a context retained once
        // per expression - which is exactly what implementing Compilable used to do, via compile()'s checkSyntax
        val before = PolyglotContexts.liveCount(processEngine)

        // FEEL could not produce "GO", so a green assertion means javascript really ran. The `items` output is
        // what pins the conversion: a js array comes back lazy, unlike the eager string
        val expected = mapOf("result" to listOf("GO"), "items" to listOf(listOf(1, 2, 3)))
        repeat(10) { assertThat(evaluate()).isEqualTo(expected) }

        // other threads of a full @SpringBootTest touch the same engine, so retry as the sibling BPMN measurement does
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            assertThat(PolyglotContexts.liveCount(processEngine))
                .`as`("javascript decision evaluations must close every context they create, compile included")
                .isLessThanOrEqualTo(before)
        }
    }

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
