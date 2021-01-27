package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan

import ru.citeck.ecos.process.domain.cmmn.model.TProcessTask
import ru.citeck.ecos.process.domain.cmmn.service.CmmnUtils
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type.CmmnProcessTask
import javax.xml.namespace.QName

class ProcessTaskConverter : CmmnConverter<TProcessTask, CmmnProcessTask> {

    companion object {
        const val TYPE = "ProcessTask"

        val PROP_PROC_TYPE = QName(CmmnUtils.NS_ECOS, "processType")
        val PROP_PROC_DEF_ID = QName(CmmnUtils.NS_ECOS, "processDefId")
    }

    override fun import(element: TProcessTask, context: ImportContext): CmmnProcessTask {

        val procType = element.otherAttributes[PROP_PROC_TYPE] ?: ""
        val procDefId = element.otherAttributes[PROP_PROC_DEF_ID] ?: ""

        return CmmnProcessTask(element.isIsBlocking, procType, procDefId)
    }

    override fun export(element: CmmnProcessTask, context: ExportContext): TProcessTask {

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
