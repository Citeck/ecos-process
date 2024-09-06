package ru.citeck.ecos.process.domain.proctask.attssync

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_SOURCE_ID
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef

@Component
class ProcTaskAttsSyncProxyDao : RecordsDaoProxy(
    PROC_TASK_ATTS_SYNC_SOURCE_ID,
    PROC_TASK_ATTS_SYNC_REPO_SOURCE_ID
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {

        log.debug { "Mutate task atts sync: \n${Json.mapper.toPrettyString(records)}" }

        val currentRequestAttributesIds = mutableSetOf<String>()
        val existsAttributes = findExistsAttributesWithoutIds(
            records.map {
                it.id.ifEmpty {
                    it.getAtt("id").asText()
                }
            }
        )

        for (record in records) {
            if (record.hasAtt("attributesSync")) {
                val attsSync = record.getAtt("attributesSync")
                for (attSync in attsSync) {
                    val attSyncId = attSync["id"].asText()
                    if (attSyncId.isBlank()) {
                        continue
                    }

                    val existAttribute = existsAttributes[attSyncId]

                    require(existAttribute == null) {
                        "Attribute id should be unique. Attribute id '$attSyncId' already exists " +
                            "on record(s) ${existAttribute?.joinToString()}"
                    }
                    require(currentRequestAttributesIds.contains(attSyncId).not()) {
                        "Attribute id should be unique. " +
                            "Attribute id '$attSyncId' already exists in current synchronization"
                    }

                    currentRequestAttributesIds.add(attSyncId)

                    // Front-end send to server meta-data of async component (typeData), so we need to remove it
                    if (attSync.has("ecosTypes")) {
                        val ecosTypes = attSync["ecosTypes"]
                        if (ecosTypes.isArray()) {
                            for (ecosType in ecosTypes) {
                                ecosType.remove("typeData")
                            }
                        }
                    }
                }
            }
        }

        return super.mutate(records)
    }

    private fun findExistsAttributesWithoutIds(syncSettingsIds: List<String>): Map<String, List<EntityRef>> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(PROC_TASK_ATTS_SYNC_SOURCE_ID)
            },
            TaskAttsSyncSettingsMeta::class.java
        ).getRecords()
            .filter { it.id.getLocalId() !in syncSettingsIds }
            .flatMap { syncSetting ->
                syncSetting.attributesSync.flatMap { attSync ->
                    attSync.ecosTypes.mapNotNull { ecosType ->
                        val att = when (syncSetting.source) {
                            TaskAttsSyncSource.RECORD -> ecosType.attribute
                            TaskAttsSyncSource.TYPE -> ecosType.recordExpressionAttribute
                        }
                        att?.let { attSync.id to syncSetting.id.toString() }
                    }
                }
            }
            .toSet()
            .groupBy({ it.first }, { it.second.toEntityRef() })
    }
}
