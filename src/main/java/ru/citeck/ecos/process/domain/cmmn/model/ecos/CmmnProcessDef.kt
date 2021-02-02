package ru.citeck.ecos.process.domain.cmmn.model.ecos

import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.MandatoryParam
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.ArtifactDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.CaseDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.DiagramInterchangeDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram.DiagramDef
import ru.citeck.ecos.records2.RecordRef

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = CmmnProcessDef.Builder::class)
class CmmnProcessDef(

    val id: String,
    val definitionsId: String,
    val name: MLText,
    val ecosType: RecordRef,

    val cases: List<CaseDef>,
    val artifacts: List<ArtifactDef>,
    val cmmnDi: DiagramInterchangeDef,

    val otherData: ObjectData
) {

    companion object {

        @JvmStatic
        fun create(builder: Builder.() -> Unit): CmmnProcessDef {
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

    fun copy(builder: Builder.() -> Unit): CmmnProcessDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var id: String = ""
        var definitionsId: String = ""
        var name: MLText = MLText()
        var ecosType: RecordRef = RecordRef.EMPTY

        var cases: List<CaseDef> = emptyList()
        var artifacts: List<ArtifactDef> = emptyList()
        var cmmnDi = DiagramInterchangeDef(emptyList())

        var otherData: ObjectData = ObjectData.create()

        constructor(base: CmmnProcessDef) : this() {
            id = base.id

            definitionsId = base.definitionsId
            name = MLText.copy(base.name) ?: MLText()
            ecosType = base.ecosType

            cases = DataValue.create(base.cases).asList(CaseDef::class.java)
            artifacts = DataValue.create(base.artifacts).asList(ArtifactDef::class.java)
            cmmnDi = DiagramInterchangeDef(DataValue.create(base.cmmnDi.diagrams).asList(DiagramDef::class.java))
            otherData = ObjectData.deepCopy(base.otherData)!!
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withDefinitionsId(definitionsId: String): Builder {
            this.definitionsId = definitionsId
            return this
        }

        fun withName(name: MLText): Builder {
            this.name = name
            return this
        }

        fun withEcosType(ecosType: RecordRef): Builder {
            this.ecosType = ecosType
            return this
        }

        fun withCases(cases: List<CaseDef>): Builder {
            this.cases = cases
            return this
        }

        fun withArtifacts(artifacts: List<ArtifactDef>): Builder {
            this.artifacts = artifacts
            return this
        }

        fun withCmmnDi(cmmnDi: DiagramInterchangeDef): Builder {
            this.cmmnDi = cmmnDi
            return this
        }

        fun withOtherData(otherData: ObjectData): Builder {
            this.otherData = otherData
            return this
        }

        fun build(): CmmnProcessDef {
            MandatoryParam.checkString("id", id)
            return CmmnProcessDef(id, definitionsId, name, ecosType, cases, artifacts, cmmnDi, otherData)
        }
    }
}
