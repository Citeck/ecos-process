package ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.action

import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.action.SetStatusConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfDefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.action.SetStatusActionDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class AlfSetStatusConverter : EcosOmgConverter<SetStatusActionDef, TTask> {

    companion object {
        val PROP_ALF_STATUS = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "actionCaseStatus")
    }

    private val standardConverter = SetStatusConverter()

    override fun import(element: TTask, context: ImportContext): SetStatusActionDef {
        error("Unsupported")
    }

    override fun export(element: SetStatusActionDef, context: ExportContext): TTask {

        val task = standardConverter.export(element, context)
        task.otherAttributes[PROP_ALF_STATUS] = element.status
        task.otherAttributes[AlfDefinitionsConverter.PROP_NODE_TYPE] = "{http://www.citeck.ru/model/action/1.0}set-case-status"

        return task
    }
}
