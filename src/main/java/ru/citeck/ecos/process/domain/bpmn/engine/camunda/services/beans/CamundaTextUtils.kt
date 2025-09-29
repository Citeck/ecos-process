package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component("TextUtils")
class CamundaTextUtils : CamundaProcessEngineService {

    override fun getKey(): String {
        return "TextUtils"
    }

    fun htmlToText(html: Any?): String {
        val content = html?.toString() ?: ""
        if (content.isBlank()) {
            return ""
        }

        return Jsoup.parse(content).text()
    }

    fun extractCodeBlock(content: Any?): String? {
        val text = content?.toString() ?: return null
        return text.extractCodeBlock()
    }

    private fun String.extractCodeBlock(): String? {
        return if (contains("```")) {
            replace(Regex("```[a-zA-Z]*\\s*([\\s\\S]*?)\\s*```"), "$1").trim()
        } else {
            this
        }
    }
}
