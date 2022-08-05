package ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.event

import mu.KotlinLogging
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event.PlanItemOnPartConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfDefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemOnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemTransitionEnum
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class AlfPlanItemOnPartConverter : EcosOmgConverter<PlanItemOnPartDef, TPlanItemOnPart> {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private val standardConverter = PlanItemOnPartConverter()

    override fun import(element: TPlanItemOnPart, context: ImportContext): PlanItemOnPartDef {
        error("Not supported")
    }

    override fun export(element: PlanItemOnPartDef, context: ExportContext): TPlanItemOnPart {

        val onPart = standardConverter.export(element, context)

        val nodeType = when (element.standardEvent) {
            PlanItemTransitionEnum.CREATE -> "{http://www.citeck.ru/model/icaseEvent/1.0}caseCreated"
            PlanItemTransitionEnum.START -> "{http://www.citeck.ru/model/icaseEvent/1.0}activityStartedEvent"
            PlanItemTransitionEnum.COMPLETE -> "{http://www.citeck.ru/model/icaseEvent/1.0}activityStoppedEvent"
            else -> {
                log.error { "UNKNOWN TYPE: ${element.standardEvent}" }
                "{http://www.citeck.ru/model/iEvent/1.0}userAction"
                // error("Unknown event: ${element.standardEvent}")
            }
        }
        onPart.otherAttributes[AlfDefinitionsConverter.PROP_NODE_TYPE] = nodeType

        // todo: ifpart

        return onPart
    }
}
