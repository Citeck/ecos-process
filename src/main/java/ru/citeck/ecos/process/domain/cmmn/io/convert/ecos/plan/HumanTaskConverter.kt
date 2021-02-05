package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.model.omg.THumanTask
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.HumanTaskDef
import ru.citeck.ecos.records2.RecordRef
import javax.xml.namespace.QName

class HumanTaskConverter : EcosOmgConverter<HumanTaskDef, THumanTask> {

    companion object {
        const val TYPE = "HumanTask"

        val PROP_ROLES = QName(CmmnXmlUtils.NS_ECOS, "roles")
        val PROP_FORM_REF = QName(CmmnXmlUtils.NS_ECOS, "formRef")
    }

    override fun import(element: THumanTask, context: ImportContext): HumanTaskDef {

        val roles = element.otherAttributes[PROP_ROLES] ?: "[]"
        val rolesList = Json.mapper.readList(roles, String::class.java)
        val formRef = element.otherAttributes[PROP_FORM_REF]?.let { RecordRef.valueOf(it) }

        return HumanTaskDef(rolesList, formRef)
    }

    override fun export(element: HumanTaskDef, context: ExportContext): THumanTask {

        val task = THumanTask()
        task.otherAttributes[PROP_ROLES] = Json.mapper.toString(element.roles)
        task.otherAttributes[PROP_FORM_REF] = element.formRef?.toString()

        return task
    }
}
