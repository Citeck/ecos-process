package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.MandatoryParam
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.PlanItemControlDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.EntryCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = ActivityDef.Builder::class)
class ActivityDef(

    val id: String,
    val name: MLText,
    val planItemId: String,

    val control: PlanItemControlDef?,

    val entryCriteria: List<EntryCriterionDef>,
    val exitCriteria: List<ExitCriterionDef>,

    val type: String,
    val data: ObjectData,

    val otherData: ObjectData
) {

    companion object {

        @JvmStatic
        fun create(builder: Builder.() -> Unit): ActivityDef {
            return Builder().apply(builder).build()
        }

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): ActivityDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var id: String = ""
        var name: MLText = MLText()
        var planItemId: String = ""

        var control: PlanItemControlDef? = null

        var entryCriteria: List<EntryCriterionDef> = emptyList()
        var exitCriteria: List<ExitCriterionDef> = emptyList()

        var type: String = ""
        var data: ObjectData = ObjectData.create()

        var otherData: ObjectData = ObjectData.create()

        constructor(base: ActivityDef) : this() {
            id = base.id
            name = base.name
            control = base.control
            planItemId = base.planItemId
            entryCriteria = DataValue.create(base.entryCriteria).asList(EntryCriterionDef::class.java)
            exitCriteria = DataValue.create(base.exitCriteria).asList(ExitCriterionDef::class.java)
            type = base.type
            data = ObjectData.deepCopy(base.data)!!
            otherData = ObjectData.deepCopy(base.otherData)!!
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withPlanItemId(planItemId: String): Builder {
            this.planItemId = planItemId
            return this
        }

        fun withName(name: MLText): Builder {
            this.name = name
            return this
        }

        fun withControl(control: PlanItemControlDef?): Builder {
            this.control = control
            return this
        }

        fun withEntryCriteria(entryCriteria: List<EntryCriterionDef>): Builder {
            this.entryCriteria = entryCriteria
            return this
        }

        fun withExitCriteria(exitCriteria: List<ExitCriterionDef>): Builder {
            this.exitCriteria = exitCriteria
            return this
        }

        fun withType(type: String): Builder {
            this.type = type
            return this
        }

        fun withData(data: ObjectData): Builder {
            this.data = data
            return this
        }

        fun withOtherData(otherData: ObjectData): Builder {
            this.otherData = otherData
            return this
        }

        fun build(): ActivityDef {
            MandatoryParam.checkString("id", id)
            MandatoryParam.checkString("type", type)
            return ActivityDef(
                id,
                name,
                planItemId,
                control,
                entryCriteria,
                exitCriteria,
                type,
                data,
                otherData
            )
        }
    }
}
