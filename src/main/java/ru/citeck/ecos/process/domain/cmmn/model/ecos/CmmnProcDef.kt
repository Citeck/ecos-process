package ru.citeck.ecos.process.domain.cmmn.model.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.ArtifactDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.CaseDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.DiagramInterchangeDef
import ru.citeck.ecos.records2.RecordRef

class CmmnProcDef(

    val id: String,
    val definitionsId: String,
    val name: MLText,
    val ecosType: RecordRef,

    val cases: List<CaseDef>,
    val artifacts: List<ArtifactDef>,
    val cmmnDi: DiagramInterchangeDef
)
