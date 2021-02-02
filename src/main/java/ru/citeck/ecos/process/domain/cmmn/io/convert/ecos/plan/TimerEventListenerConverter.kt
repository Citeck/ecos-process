package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.process.domain.cmmn.model.omg.TTimerEventListener
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.listener.TimerEventListenerDef

class TimerEventListenerConverter : EcosOmgConverter<TimerEventListenerDef, TTimerEventListener> {

    companion object {
        const val TYPE = "TimerEventListener"
    }

    override fun import(element: TTimerEventListener, context: ImportContext): TimerEventListenerDef {
        //todo
        return TimerEventListenerDef()
    }

    override fun export(element: TimerEventListenerDef, context: ExportContext): TTimerEventListener {
        //todo
        return TTimerEventListener()
    }

    override fun getElementType() = TYPE
}
