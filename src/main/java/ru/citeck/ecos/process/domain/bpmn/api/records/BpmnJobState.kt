package ru.citeck.ecos.process.domain.bpmn.api.records

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

class JobStateRecord(
    private val state: BpmnJobState
) {

    @AttName("suspended")
    fun getIsSuspended(): Boolean {
        return state == BpmnJobState.SUSPENDED
    }

    @AttName(".disp")
    fun getDisp(): MLText {
        return state.disp
    }
}

enum class BpmnJobState(
    val disp: MLText
) {
    ACTIVE(
        MLText(
            I18nContext.ENGLISH to "Active",
            I18nContext.RUSSIAN to "Активно"
        )
    ),
    SUSPENDED(
        MLText(
            I18nContext.ENGLISH to "Suspended",
            I18nContext.RUSSIAN to "Приостановлено"
        )
    );
}
