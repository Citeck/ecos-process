package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.CaseFileItemTransitionEnum
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.CaseFileOnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.*

class CaseFileOnPartConverter : EcosOmgConverter<CaseFileOnPartDef, TCaseFileItemOnPart> {

    override fun import(element: TCaseFileItemOnPart, context: ImportContext): CaseFileOnPartDef {

        val onPartSource = element.sourceRef as TPlanItem
        val name = element.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] ?: element.name

        return CaseFileOnPartDef(
            element.id,
            Json.mapper.read(name, MLText::class.java) ?: MLText(),
            onPartSource.id,
            CaseFileItemTransitionEnum.fromValue(element.standardEvent.value())
        )
    }

    override fun export(element: CaseFileOnPartDef, context: ExportContext): TCaseFileItemOnPart {

        val result = TCaseFileItemOnPart()

        result.id = element.id
        result.standardEvent = CaseFileItemTransition.fromValue(element.standardEvent.getValue())
        result.sourceRef = element.sourceRef

        result.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(element.name)

        return result
    }
}
