package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.common.patch.EcosDataMigrationState
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefAdapter
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepoAdapter
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.time.Instant

@Primary
@Component
class ProcDefMultiRepoAdapter(
    private val edata: EcosDataProcDefAdapter,
    private val optionalMongoRepo: ObjectProvider<MongoProcDefRepoAdapter>,
    private val migrationState: EcosDataMigrationState
) : ProcDefRepository {

    private val mongo: MongoProcDefRepoAdapter? by lazy {
        optionalMongoRepo.getIfAvailable { null }
    }

    override fun delete(entity: ProcDefEntity) {
        if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.delete(entity)
        } else {
            mongo!!.delete(entity)
        }
    }

    override fun deleteAll() {
        if (!AuthContext.isRunAsSystem()) {
            error("Permission denied")
        }
        if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.deleteAll()
        } else {
            mongo!!.deleteAll()
        }
    }

    override fun save(entity: ProcDefEntity): ProcDefEntity {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.save(entity)
        } else {
            mongo!!.save(entity)
        }
    }

    override fun findAll(workspaces: List<String>, predicate: Predicate, pageable: Pageable): Page<ProcDefEntity> {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.findAll(workspaces, predicate, pageable)
        } else {
            mongo!!.findAll(workspaces, predicate, pageable)
        }
    }

    override fun findFirstEnabledByEcosType(workspace: String, type: String, ecosTypeRef: String): ProcDefEntity? {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.findFirstEnabledByEcosType(workspace, type, ecosTypeRef)
        } else {
            mongo!!.findFirstEnabledByEcosType(workspace, type, ecosTypeRef)
        }
    }

    override fun findByIdInWs(workspace: String, type: String, extId: String): ProcDefEntity? {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.findByIdInWs(workspace, type, extId)
        } else {
            mongo!!.findByIdInWs(workspace, type, extId)
        }
    }

    override fun getCount(workspaces: List<String>, predicate: Predicate): Long {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.getCount(workspaces, predicate)
        } else {
            mongo!!.getCount(workspaces, predicate)
        }
    }

    override fun getCount(workspaces: List<String>): Long {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.getCount(workspaces)
        } else {
            mongo!!.getCount(workspaces)
        }
    }

    override fun getLastModifiedDate(): Instant {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.getLastModifiedDate()
        } else {
            mongo!!.getLastModifiedDate()
        }
    }

    override fun findFirstByProcTypeAndAlfType(workspace: String, type: String, alfType: String): ProcDefEntity? {
        return if (migrationState.isEdataStoragePrimaryForProcDefs()) {
            edata.findFirstByProcTypeAndAlfType(workspace, type, alfType)
        } else {
            mongo!!.findFirstByProcTypeAndAlfType(workspace, type, alfType)
        }
    }
}
