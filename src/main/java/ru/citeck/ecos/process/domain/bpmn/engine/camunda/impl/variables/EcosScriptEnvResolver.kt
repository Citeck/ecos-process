package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.impl.scripting.env.ScriptEnvResolver
import org.springframework.util.ResourceUtils
import java.io.FileNotFoundException

private const val ECOS_ENV_PATH_TEMPLATE = "classpath:camunda/script/env/%s/ecos_env.%s"

/**
 * Currently we only support javascript script lang as default.
 * If new languages appear, you must also add a file with variables, like [camunda/script/env/javascript/ecos_env.js].
 */
class EcosScriptEnvResolver : ScriptEnvResolver {

    companion object {
        private val log = KotlinLogging.logger {}

        private val EXTENSIONS_MAPPING = mapOf(
            "javascript" to "js",
        )
    }

    override fun resolve(language: String?): Array<String>? {
        if (language.isNullOrBlank()) return null

        val extension = EXTENSIONS_MAPPING[language.lowercase()] ?: return null
        val scriptEnvPath = String.format(ECOS_ENV_PATH_TEMPLATE, language, extension)

        val scriptText = try {
            ResourceUtils.getFile(scriptEnvPath).readText()
        } catch (e: FileNotFoundException) {
            log.warn("Script env file not found", e)
            return null
        }

        return arrayOf(scriptText)
    }
}
