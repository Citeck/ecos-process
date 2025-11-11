package ru.citeck.ecos.process.domain.proc.repo

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.patch.EcosDataMigrationState
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcStateAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateAdapter
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity

@Primary
@Component
class ProcStateMultiRepoAdapter(
    private val edata: EcosDataProcStateAdapter,
    private val optionalMongoRepo: ObjectProvider<MongoProcStateAdapter>,
    private val migrationState: EcosDataMigrationState
) : ProcStateRepository {

    private val mongo: MongoProcStateAdapter? by lazy {
        optionalMongoRepo.getIfAvailable { null }
    }

    override fun findById(id: EntityUuid): ProcessStateEntity? {
        return if (migrationState.isEdataStoragePrimaryForProcInstances()) {
            edata.findById(id)
        } else {
            mongo!!.findById(id)
        }
    }

    override fun save(entity: ProcessStateEntity): ProcessStateEntity {
        return if (migrationState.isEdataStoragePrimaryForProcInstances()) {
            edata.save(entity)
        } else {
            mongo!!.save(entity)
        }
    }

    override fun findFirstByProcDefRevIn(procDefRev: List<ProcDefRevEntity>): ProcessStateEntity? {
        return if (migrationState.isEdataStoragePrimaryForProcInstances()) {
            edata.findFirstByProcDefRevIn(procDefRev)
        } else {
            mongo!!.findFirstByProcDefRevIn(procDefRev)
        }
    }
}
