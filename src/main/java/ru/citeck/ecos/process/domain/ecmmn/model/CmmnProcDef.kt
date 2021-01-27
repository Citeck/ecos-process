package ru.citeck.ecos.process.domain.ecmmn.model

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.ecmmn.model.artifact.CmmnArtifact
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.CmmnCaseDef
import ru.citeck.ecos.process.domain.ecmmn.model.di.CmmnDiDef
import ru.citeck.ecos.records2.RecordRef

class CmmnProcDef(

    val id: String,
    val definitionsId: String,
    val name: MLText,
    val ecosType: RecordRef,

    val cases: List<CmmnCaseDef>,
    val artifacts: List<CmmnArtifact>,
    val cmmnDi: CmmnDiDef
)
