package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan

import ru.citeck.ecos.process.domain.cmmn.model.TTimerEventListener
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type.listener.CmmnTimerEventListener

class TimerEventListenerConverter : CmmnConverter<TTimerEventListener, CmmnTimerEventListener> {

    companion object {
        const val TYPE = "TimerEventListener"
    }

    override fun import(element: TTimerEventListener, context: ImportContext): CmmnTimerEventListener {
        //todo
        return CmmnTimerEventListener()
    }

    override fun export(element: CmmnTimerEventListener, context: ExportContext): TTimerEventListener {
        //todo
        return TTimerEventListener()
    }

    override fun getElementType() = TYPE
}
