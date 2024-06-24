package ru.citeck.ecos.process.domain.proctask.attssync

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin

@Component
class ProcTaskAttsSyncMixin : AttMixin {

    companion object {
        private const val ATT_ATTRIBUTES = "attributes"

        private val providedAtts = listOf(ATT_ATTRIBUTES)
    }

    override fun getAtt(path: String, value: AttValueCtx): Any? {
        if (path != ATT_ATTRIBUTES) {
            return null
        }

        val attributesSync = value.getAtt("attributesSync[]?json").asList(TaskSyncAttribute::class.java)

        return attributesSync.map { it.id }
    }

    override fun getProvidedAtts(): Collection<String> {
        return providedAtts
    }
}
