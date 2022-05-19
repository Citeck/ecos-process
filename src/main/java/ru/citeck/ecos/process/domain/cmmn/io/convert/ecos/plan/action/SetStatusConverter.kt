package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.action

import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.action.SetStatusActionDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class SetStatusConverter : EcosOmgConverter<SetStatusActionDef, TTask> {

    companion object {
        val PROP_STATUS = QName(CmmnXmlUtils.NS_ECOS, "status")
    }

    override fun import(element: TTask, context: ImportContext): SetStatusActionDef {

        val status = element.otherAttributes[PROP_STATUS] ?: ""

        return SetStatusActionDef(status)
    }

    override fun export(element: SetStatusActionDef, context: ExportContext): TTask {

        val task = TTask()
        task.otherAttributes[PROP_STATUS] = element.status

        return task
    }
}
