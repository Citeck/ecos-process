package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.common.patch.EcosDataMigrationState
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRevRepoAdapter

@Primary
@Component
class ProcDefRevMultiRepoAdapter(
    private val edata: EcosDataProcDefRevAdapter,
    private val optionalMongoRepo: ObjectProvider<MongoProcDefRevRepoAdapter>,
    private val migrationState: EcosDataMigrationState
) : ProcDefRevRepository {

    private val mongo: MongoProcDefRevRepoAdapter? by lazy {
        optionalMongoRepo.getIfAvailable { null }
    }

    override fun save(entity: ProcDefRevEntity): ProcDefRevEntity {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.save(entity)
        } else {
            mongo!!.save(entity)
        }
    }

    override fun findById(id: EntityUuid): ProcDefRevEntity? {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.findById(id)
        } else {
            mongo!!.findById(id)
        }
    }

    override fun findAllById(ids: Iterable<EntityUuid>): List<ProcDefRevEntity> {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.findAllById(ids)
        } else {
            mongo!!.findAllById(ids)
        }
    }

    override fun findAllByProcessDef(processDef: ProcDefEntity): List<ProcDefRevEntity> {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.findAllByProcessDef(processDef)
        } else {
            mongo!!.findAllByProcessDef(processDef)
        }
    }

    override fun findByDeploymentId(deploymentId: String): ProcDefRevEntity? {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.findByDeploymentId(deploymentId)
        } else {
            mongo!!.findByDeploymentId(deploymentId)
        }
    }

    override fun findByDeploymentIdIsIn(deploymentIds: List<String>): List<ProcDefRevEntity> {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.findByDeploymentIdIsIn(deploymentIds)
        } else {
            mongo!!.findByDeploymentIdIsIn(deploymentIds)
        }
    }

    override fun queryAllByDeploymentIdIsNotNull(pageable: Pageable): Slice<ProcDefRevEntity> {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.queryAllByDeploymentIdIsNotNull(pageable)
        } else {
            mongo!!.queryAllByDeploymentIdIsNotNull(pageable)
        }
    }

    override fun deleteAll(entities: List<ProcDefRevEntity>) {
        return if (migrationState.isEdataStoragePrimary()) {
            edata.deleteAll(entities)
        } else {
            mongo!!.deleteAll(entities)
        }
    }

    override fun deleteAll() {
        if (!AuthContext.isRunAsSystem()) {
            error("Permission denied")
        }
        return if (migrationState.isEdataStoragePrimary()) {
            edata.deleteAll()
        } else {
            mongo!!.deleteAll()
        }
    }
}
