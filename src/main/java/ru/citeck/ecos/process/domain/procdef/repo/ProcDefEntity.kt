package ru.citeck.ecos.process.domain.procdef.repo

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

@Document(collection = "process_def")
@CompoundIndexes(
    CompoundIndex(name = "proc_def_tnt_proc_type_ext_id_idx", def = "{'id.tnt': 1, 'procType' : 1, 'extId': 1}"),
    CompoundIndex(
        name = "proc_def_tnt_proc_type_ecos_type_idx",
        def = "{'id.tnt': 1, 'procType' : 1, 'ecosTypeRef': 1}"
    ),
    CompoundIndex(name = "proc_def_tnt_proc_type_alf_type_idx", def = "{'id.tnt': 1, 'procType' : 1, 'alfType': 1}")
)
class ProcDefEntity {

    @Id
    var id: EntityUuid? = null

    var procType: String? = null
    var name: String? = null
    var extId: String? = null
    var ecosTypeRef: String? = null
    var formRef: String? = null
    var workingCopySourceRef: String? = null
    var alfType: String? = null
    var created: Instant? = null
    var modified: Instant? = null
    var enabled: Boolean? = null
    var autoStartEnabled: Boolean? = null
    var autoDeleteEnabled: Boolean? = null
    var sectionRef: String? = null
    var workspace: String? = null

    @DBRef(lazy = true)
    var lastRev: ProcDefRevEntity? = null

    fun copyWithId(id: String): ProcDefEntity {
        val copy = copy()
        copy.id = EntityUuid(0, UUID.fromString(id))
        return copy
    }

    fun copy(): ProcDefEntity {
        val copy = ProcDefEntity()
        copy.id = this.id
        copy.procType = this.procType
        copy.name = this.name
        copy.extId = this.extId
        copy.ecosTypeRef = this.ecosTypeRef
        copy.formRef = this.formRef
        copy.workingCopySourceRef = this.workingCopySourceRef
        copy.alfType = this.alfType
        copy.created = this.created
        copy.modified = this.modified
        copy.enabled = this.enabled
        copy.autoStartEnabled = this.autoStartEnabled
        copy.autoDeleteEnabled = this.autoDeleteEnabled
        copy.sectionRef = this.sectionRef
        copy.lastRev = this.lastRev
        copy.workspace = this.workspace
        return copy
    }

    @JsonValue
    fun getAsJson(): DataValue {
        return DataValue.createObj()
            .set("id", id?.id?.toString())
            .set("extId", extId)
            .set("name", name)
            .set("procType", procType)
            .set("ecosTypeRef", ecosTypeRef)
            .set("formRef", formRef)
            .set("workingCopySourceRef", workingCopySourceRef)
            .set("alfType", alfType)
            .set("_created", created)
            .set("_modified", modified)
            .set("enabled", enabled)
            .set("autoStartEnabled", autoStartEnabled)
            .set("autoDeleteEnabled", autoDeleteEnabled)
            .set("sectionRef", sectionRef)
            .set("lastRev", EcosDataProcDefRevAdapter.toRef(lastRev))
            .set("_workspace",
                if (workspace.isNullOrBlank()) {
                    null
                } else {
                    EntityRef.create(AppName.EMODEL, "workspace", workspace)
                }
            )
    }

    override fun toString(): String {
        return getAsJson().toString()
    }
}
