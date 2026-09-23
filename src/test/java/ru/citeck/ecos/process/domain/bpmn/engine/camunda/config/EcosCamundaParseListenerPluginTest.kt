package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.stereotype.Component

/**
 * A parse listener only reaches the engine if [EcosCamundaParseListenerPlugin] adds it to
 * `customPreBPMNParseListeners`. Annotating it with `@Component` is not enough — Spring will
 * create the bean and nothing will ever call it, which is how `aiAgentTask` shipped: its scripts
 * and "save result to document attribute" silently did nothing.
 *
 * So the check is on the whole set: every parse listener component in the project must be
 * registered. A listener that is intentionally wired somewhere else has to be excluded here
 * explicitly, with a reason.
 */
class EcosCamundaParseListenerPluginTest {

    @Test
    fun `every parse listener component is registered in the engine`() {
        val configuration = StandaloneProcessEngineConfiguration()

        pluginWithMockedListeners().preInit(configuration)

        val registered = configuration.customPreBPMNParseListeners
        val notRegistered = parseListenerComponents().filter { listener ->
            registered.none { listener.isInstance(it) }
        }

        assertThat(notRegistered)
            .describedAs(
                "parse listener components missing from EcosCamundaParseListenerPlugin — " +
                    "the beans exist but the engine never calls them"
            ).isEmpty()
    }

    @Test
    fun `registration keeps the parse listeners already configured on the engine`() {
        val alreadyConfigured = Mockito.mock(AbstractBpmnParseListener::class.java)
        val configuration = StandaloneProcessEngineConfiguration()
        configuration.customPreBPMNParseListeners = mutableListOf(alreadyConfigured)

        pluginWithMockedListeners().preInit(configuration)

        assertThat(configuration.customPreBPMNParseListeners).contains(alreadyConfigured)
    }

    private fun pluginWithMockedListeners(): EcosCamundaParseListenerPlugin {
        val constructor = EcosCamundaParseListenerPlugin::class.java.declaredConstructors.single()
        val arguments = constructor.parameterTypes.map { Mockito.mock(it) }.toTypedArray()

        return constructor.newInstance(*arguments) as EcosCamundaParseListenerPlugin
    }

    private fun parseListenerComponents(): List<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(AbstractBpmnParseListener::class.java))

        return scanner
            .findCandidateComponents("ru.citeck.ecos.process")
            .mapNotNull { it.beanClassName }
            .map { Class.forName(it) }
            .filter { it.isAnnotationPresent(Component::class.java) }
    }
}
