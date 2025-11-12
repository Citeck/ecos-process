package ru.citeck.ecos.process.domain.proc.repo

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.patch.EcosDataMigrationState
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcInstanceAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceAdapter

@Primary
@Component
class ProcInstanceMultiRepoAdapter(
    private val edata: EcosDataProcInstanceAdapter,
    private val optionalMongoRepo: ObjectProvider<MongoProcInstanceAdapter>,
    private val migrationState: EcosDataMigrationState
) : ProcInstanceRepository {

    private val mongo: MongoProcInstanceAdapter? by lazy {
        optionalMongoRepo.getIfAvailable { null }
    }

    override fun findById(id: EntityUuid): ProcessInstanceEntity? {
        return if (migrationState.isEdataStoragePrimaryForProcInstances()) {
            edata.findById(id)
        } else {
            mongo!!.findById(id)
        }
    }

    override fun save(entity: ProcessInstanceEntity): ProcessInstanceEntity {
        return if (migrationState.isEdataStoragePrimaryForProcInstances()) {
            edata.save(entity)
        } else {
            mongo!!.save(entity)
        }
    }
}
