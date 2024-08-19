package ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.event

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event.SentryConverter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.SentryDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class AlfSentryConverter : EcosOmgConverter<SentryDef, Sentry> {

    companion object {
        private val PROP_ORIGINAL_EVENT = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "originalEvent")

        private val log = KotlinLogging.logger {}
    }

    private val standardConverter = SentryConverter()

    override fun import(element: Sentry, context: ImportContext): SentryDef {
        error("Unsupported")
    }

    override fun export(element: SentryDef, context: ExportContext): Sentry {

        val cmmnSentry = standardConverter.export(element, context)

        val onPart = cmmnSentry.onPart[0].value as TPlanItemOnPart

        // todo "stage-children-stopped" -> complete
        // "case-properties-changed" -> resume
        // "user-action" -> occur ns8:confirmationMessage="????"
        val originalEvent = when (onPart.standardEvent) {
            PlanItemTransition.CREATE -> "case-created"
            PlanItemTransition.START -> "activity-started"
            PlanItemTransition.COMPLETE -> "activity-stopped"
            else -> {
                // error("Unknown event: ${onPart.standardEvent}")
                log.error { "UNKNOWN TYPE ${onPart.standardEvent}" }
                "user-action"
            }
        }
        cmmnSentry.otherAttributes[PROP_ORIGINAL_EVENT] = originalEvent

        return cmmnSentry
    }
}
/*<cmmn:processTask xmlns:ns12="http://www.citeck.ru/model/workflow/case-perform/1.0" xmlns:ns13="http://www.citeck.ru/model/icaseTask/1.0" isBlocking="true" name="activiti$case-perform" id="id-78" ns9:actualEndDate="" ns9:manualStopped="false" ns9:manualStarted="false" ns12:performOutcomes="" ns9:index="2" ns13:workflowInstanceId="" ns8:startCompletnessLevels="" ns9:typeVersion="" ns8:performersRoles="id-2" ns10:owner="admin" ns8:nodeType="{http://www.citeck.ru/model/workflow/case-perform/1.0}performCaseTask" ns12:outcomesWithMandatoryComment="Reject,Rework" ns12:syncRolesToWorkflow="true" ns13:workflowDefinitionName="activiti$case-perform" ns12:syncWorkflowToRoles="true" ns10:inherits="true" ns9:plannedEndDate="2019-05-07T13:34:57.140+07:00" ns11:state="Not started" ns12:formKey="ctrwf:confirmTask" ns12:abortOutcomes="Reject" ns9:repeatable="true" ns9:actualStartDate="" ns13:priority="2" ns8:stopCompletnessLevels="" ns8:title="Согласовать"/>*/
