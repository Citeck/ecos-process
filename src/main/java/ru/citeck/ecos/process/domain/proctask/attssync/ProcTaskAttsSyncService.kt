package ru.citeck.ecos.process.domain.proctask.attssync

import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_TYPE_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_SOURCE_ID
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_DUE_DATE
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

typealias TaskAttsSyncByType = Map<EntityRef, TaskAttByRequestedAtt>
typealias TaskAttByRequestedAtt = Map<String, String>

@Service
class ProcTaskAttsSyncService(
    private val recordsService: RecordsService,
    private val procTaskSyncCache: ProcTaskSyncCache,
    private val ecosTypesRegistry: EcosTypesRegistry
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun removeSyncSettings(ref: EntityRef) {
        recordsService.delete(ref)
        procTaskSyncCache.evictTaskSyncAttributesCache()
    }

    fun getSyncSettings(ref: EntityRef): TaskAttsSyncSettingsMeta? {
        return try {
            recordsService.getAtts(ref, TaskAttsSyncSettingsMeta::class.java)
        } catch (e: Exception) {
            log.debug("Failed to get sync settings: $ref", e)
            return null
        }
    }

    fun getTaskAttTypeOrTextDefault(attName: String): AttributeType {
        if (attName == ATT_DUE_DATE) {
            return AttributeType.DATETIME
        }

        val attWithoutPrefix =
            attName.removePrefix(TASK_DOCUMENT_TYPE_ATT_PREFIX).removePrefix(TASK_DOCUMENT_ATT_PREFIX)
        return procTaskSyncCache.getTaskSyncAttribute(attWithoutPrefix)?.type ?: AttributeType.TEXT
    }

    fun findMappedByType(documentType: EntityRef): TaskAttsSyncSettingsTyped? {
        val onRecordSettings = findEnabledSyncSettingsForSource(TaskAttsSyncSource.RECORD)
        val onTypeSettings = findEnabledSyncSettingsForSource(TaskAttsSyncSource.TYPE)

        if (onRecordSettings.isEmpty() && onTypeSettings.isEmpty()) {
            return null
        }

        val documentTypeParents = ecosTypesRegistry.getParents(documentType)
        val onRecordTyped = onRecordSettings.toTypesByAttributes()
        val onTypeTyped = onTypeSettings.toTypesByAttributes()

        return TaskAttsSyncSettingsTyped(
            documentType,
            findSyncAttsByTypeHierarchy(documentType, documentTypeParents, onRecordTyped) ?: emptyMap(),
            findSyncAttsByTypeHierarchy(documentType, documentTypeParents, onTypeTyped) ?: emptyMap()
        )
    }

    private fun findSyncAttsByTypeHierarchy(
        typeRef: EntityRef,
        typeParent: List<EntityRef>,
        typesByAttributes: TaskAttsSyncByType
    ): TaskAttByRequestedAtt? {
        if (typesByAttributes.containsKey(typeRef)) {
            return typesByAttributes[typeRef]
        }

        for (parent in typeParent) {
            if (typesByAttributes.containsKey(parent)) {
                return typesByAttributes[parent]
            }
        }

        return null
    }

    fun findEnabledSyncSettingsForSource(source: TaskAttsSyncSource): List<TaskAttsSyncSettingsMeta> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(PROC_TASK_ATTS_SYNC_SOURCE_ID)
                withQuery(
                    Predicates.and(
                        Predicates.eq("source", source.name),
                        Predicates.eq("enabled", true)
                    )
                )
            },
            TaskAttsSyncSettingsMeta::class.java
        ).getRecords()
    }
}

@Component
class ProcTaskSyncCache(
    private val recordsService: RecordsService
) {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val ATTS_SYNC_CACHE_NAME = "taskAttsSyncAttributes"
    }

    @Cacheable(ATTS_SYNC_CACHE_NAME)
    fun getTaskSyncAttribute(attName: String): TaskSyncAttribute? {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(PROC_TASK_ATTS_SYNC_SOURCE_ID)
            },
            TaskAttsSyncSettingsMeta::class.java
        ).getRecords()
            .flatMap { it.attributesSync }
            .find { it.id == attName }
    }

    @CacheEvict(ATTS_SYNC_CACHE_NAME, allEntries = true)
    fun evictTaskSyncAttributesCache() {
        log.debug { "Evicting cache for task attributes sync" }
    }
}

fun Collection<TaskAttsSyncSettingsMeta>.toTypesByAttributes(): TaskAttsSyncByType {
    return this
        .flatMap { syncSetting ->
            syncSetting.attributesSync.flatMap { attSync ->
                attSync.ecosTypes.mapNotNull { ecosType ->
                    val att = when (syncSetting.source) {
                        TaskAttsSyncSource.RECORD -> ecosType.attribute
                        TaskAttsSyncSource.TYPE -> ecosType.recordExpressionAttribute
                    }
                    att?.let { Triple(ecosType.typeRef, attSync.id, it) }
                }
            }
        }
        .groupBy({ it.first }, { Pair(it.second, it.third) })
        .mapValues { entry ->
            entry.value.associate { it.first to it.second }
        }
}

data class TaskAttsSyncSettingsTyped(
    val typeRef: EntityRef,
    val recordSyncAtts: Map<String, String>,
    val typeSyncAtts: Map<String, String>,
)

data class TaskAttsSyncSettingsMeta(
    @AttName("id")
    val id: EntityRef,

    @AttName("enabled?bool!")
    val enabled: Boolean,

    @AttName("name!")
    val name: String,

    @AttName("source")
    val source: TaskAttsSyncSource,

    @AttName("attributesSync?json")
    val attributesSync: List<TaskSyncAttribute> = emptyList()
)

enum class TaskAttsSyncSource {
    RECORD, TYPE
}

data class TaskSyncAttribute(
    val id: String,
    val type: AttributeType,

    @AttName("ecosTypes?json")
    val ecosTypes: List<TaskSyncAttributeType> = emptyList(),
)

data class TaskSyncAttributeType(
    @AttName("typeRef?id")
    val typeRef: EntityRef,
    val attribute: String? = null,
    val recordExpressionAttribute: String? = null
)
