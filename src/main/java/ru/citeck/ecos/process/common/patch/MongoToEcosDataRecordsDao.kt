package ru.citeck.ecos.process.common.patch

import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.record.dao.mutate.ValueMutateDao

/*
Manual migration script:

var rec = Records.getRecordToEdit('eproc/mongo-to-edata-actions@');
rec.att('action', 'run-migration');
await rec.save('?json');
*/
@Component
class MongoToEcosDataRecordsDao(
    private val migration: ObjectProvider<MongoToEcosDataMigrationConfig.MongoToEcosDataMigration>
) : ValueMutateDao<MongoToEcosDataRecordsDao.ActionDto> {

    override fun getId(): String {
        return "mongo-to-edata-actions"
    }

    override fun mutate(value: ActionDto): Any? {
        if (AuthContext.isNotRunAsSystemOrAdmin()) {
            error("permission denied")
        }
        if (value.action == "run-migration") {
            val migration = migration.getIfAvailable()
            if (migration == null) {
                error("Migration is not registered. You doesn't disable mongo repo?")
            } else {
                return migration.run(value.resetMigratedStateFor)
            }
        }
        error("Action doesn't supported: '${value.action}'")
    }

    class ActionDto(
        val action: String,
        val resetMigratedStateFor: List<String> = emptyList()
    )
}
