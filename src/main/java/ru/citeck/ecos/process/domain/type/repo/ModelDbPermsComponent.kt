package ru.citeck.ecos.process.domain.type.repo

import org.springframework.stereotype.Component
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.records.perms.DefaultDbPermsComponent
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService

//todo: will be removed soon
@Component
class ModelDbPermsComponent(
    recordsService: RecordsService
) : DbPermsComponent {

    private val defaultPerms = DefaultDbPermsComponent(recordsService)

    override fun getRecordPerms(recordRef: RecordRef): DbRecordPerms {
        return defaultPerms.getRecordPerms(recordRef)
    }
}
