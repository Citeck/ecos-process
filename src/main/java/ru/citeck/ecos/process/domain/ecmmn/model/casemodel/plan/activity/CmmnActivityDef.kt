package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity

import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.MandatoryParam
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnEntryCriterion
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnExitCriterion

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = CmmnActivityDef.Builder::class)
class CmmnActivityDef(

    val id: String,
    val name: MLText,
    val planItemId: String,

    val requiredRule: Boolean?,
    val repetitionRule: Boolean?,
    val manualActivationRule: Boolean?,

    val entryCriteria: List<CmmnEntryCriterion>,
    val exitCriteria: List<CmmnExitCriterion>,

    val type: String,
    val data: ObjectData
) {

    companion object {

        @JvmStatic
        fun create(builder: Builder.() -> Unit): CmmnActivityDef {
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

    fun copy(builder: Builder.() -> Unit): CmmnActivityDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var id: String = ""
        var name: MLText = MLText()
        var planItemId: String = ""

        var requiredRule: Boolean? = null
        var repetitionRule: Boolean? = null
        var manualActivationRule: Boolean? = null

        var entryCriteria: List<CmmnEntryCriterion> = emptyList()
        var exitCriteria: List<CmmnExitCriterion> = emptyList()

        var type: String = ""
        var data: ObjectData = ObjectData.create()

        constructor(base: CmmnActivityDef) : this() {
            id = base.id
            name = base.name
            planItemId = base.planItemId
            requiredRule = base.requiredRule
            repetitionRule = base.repetitionRule
            manualActivationRule = base.manualActivationRule
            entryCriteria = DataValue.create(base.entryCriteria).asList(CmmnEntryCriterion::class.java)
            exitCriteria = DataValue.create(base.exitCriteria).asList(CmmnExitCriterion::class.java)
            type = base.type
            data = ObjectData.deepCopy(base.data)!!
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

        fun withRequiredRule(requiredRule: Boolean?): Builder {
            this.requiredRule = requiredRule
            return this
        }

        fun withRepetitionRule(repetitionRule: Boolean?): Builder {
            this.repetitionRule = repetitionRule
            return this
        }

        fun withManualActivationRule(manualActivationRule: Boolean?): Builder {
            this.manualActivationRule = manualActivationRule
            return this
        }

        fun withEntryCriteria(entryCriteria: List<CmmnEntryCriterion>): Builder {
            this.entryCriteria = entryCriteria
            return this
        }

        fun withExitCriteria(exitCriteria: List<CmmnExitCriterion>): Builder {
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

        fun build(): CmmnActivityDef {
            MandatoryParam.checkString("id", id)
            MandatoryParam.checkString("type", type)
            return CmmnActivityDef(
                id,
                name,
                planItemId,
                requiredRule,
                repetitionRule,
                manualActivationRule,
                entryCriteria,
                exitCriteria,
                type,
                data
            )
        }
    }

}
