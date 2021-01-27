package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.model.THumanTask
import ru.citeck.ecos.process.domain.cmmn.service.CmmnUtils
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type.CmmnHumanTask
import javax.xml.namespace.QName

class HumanTaskConverter : CmmnConverter<THumanTask, CmmnHumanTask> {

    companion object {
        const val TYPE = "HumanTask"

        val PROP_ROLES = QName(CmmnUtils.NS_ECOS, "roles")
    }

    override fun import(element: THumanTask, context: ImportContext): CmmnHumanTask {

        val roles = element.otherAttributes[PROP_ROLES] ?: "[]"
        val rolesList = Json.mapper.readList(roles, String::class.java)

        return CmmnHumanTask(rolesList)
    }

    override fun export(element: CmmnHumanTask, context: ExportContext): THumanTask {

        val task = THumanTask()
        task.otherAttributes[PROP_ROLES] = Json.mapper.toString(element.roles)

        return task
    }

    override fun getElementType() = TYPE
}
