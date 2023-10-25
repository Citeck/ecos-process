package ru.citeck.ecos.process.common.section.records

import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin
import ru.citeck.ecos.webapp.api.entity.EntityRef

class SectionParentMixin(private val sectionSrcId: String) : AttMixin {

    override fun getAtt(path: String, value: AttValueCtx): Any {
        var parentRefStr = value.getAtt("parentRef?id").asText()
        if (parentRefStr.isBlank() && value.getLocalId() != "ROOT") {
            parentRefStr = "eproc/$sectionSrcId@ROOT"
        }
        return EntityRef.valueOf(parentRefStr)
    }

    override fun getProvidedAtts(): Collection<String> {
        return setOf(RecordConstants.ATT_PARENT)
    }
}
