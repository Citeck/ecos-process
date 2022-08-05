package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.listener.TimerEventListenerDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TTimerEventListener
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class TimerEventListenerConverter : EcosOmgConverter<TimerEventListenerDef, TTimerEventListener> {

    override fun import(element: TTimerEventListener, context: ImportContext): TimerEventListenerDef {
        // todo
        return TimerEventListenerDef()
    }

    override fun export(element: TimerEventListenerDef, context: ExportContext): TTimerEventListener {
        // todo
        return TTimerEventListener()
    }
}
