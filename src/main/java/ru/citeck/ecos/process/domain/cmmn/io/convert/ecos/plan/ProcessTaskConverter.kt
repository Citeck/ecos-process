package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.process.domain.cmmn.model.omg.TProcessTask
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.ProcessTaskDef
import javax.xml.namespace.QName

class ProcessTaskConverter : EcosOmgConverter<ProcessTaskDef, TProcessTask> {

    companion object {
        const val TYPE = "ProcessTask"

        val PROP_PROC_TYPE = QName(CmmnXmlUtils.NS_ECOS, "processType")
        val PROP_PROC_DEF_ID = QName(CmmnXmlUtils.NS_ECOS, "processDefId")
    }

    override fun import(element: TProcessTask, context: ImportContext): ProcessTaskDef {

        val procType = element.otherAttributes[PROP_PROC_TYPE] ?: ""
        val procDefId = element.otherAttributes[PROP_PROC_DEF_ID] ?: ""

        return ProcessTaskDef(element.isIsBlocking, procType, procDefId)
    }

    override fun export(element: ProcessTaskDef, context: ExportContext): TProcessTask {

        val procTask = TProcessTask()
        procTask.otherAttributes[PROP_PROC_TYPE] = element.processType
        procTask.otherAttributes[PROP_PROC_DEF_ID] = element.processDefId
        if (!element.isBlocking) {
            procTask.isIsBlocking = false
        }

        return procTask
    }

    override fun getElementType() = TYPE
}
