package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.resolver

import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProps

/**
 * @author Roman Makarskiy
 */
class CamundaEcosVariablesScriptResolver(
    private val ecosWebAppProps: EcosWebAppProps
) : Resolver {

    companion object {
        private const val WEB_URL_VAR = "webUrl"

        private val KEY_SET = mutableSetOf(WEB_URL_VAR)
    }

    override fun containsKey(key: Any?): Boolean {
        return KEY_SET.contains(key)
    }

    override fun get(key: Any?): Any? {
        return when (key) {
            WEB_URL_VAR -> {
                ecosWebAppProps.webUrl
            }
            else -> null
        }
    }

    override fun keySet(): MutableSet<String> {
        return mutableSetOf()
    }
}
