package ru.citeck.ecos.process.domain.proctask.attssync

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.status.constants.StatusConstants
import ru.citeck.ecos.model.lib.type.dto.TypeInfo
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.rabbitmq.RabbitMqChannel
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.lock.EcosAppLockService
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.sql.Timestamp
import java.time.Duration
import java.util.*
import kotlin.system.measureTimeMillis

@Component
class ProcTaskAttsSynchronizer(
    @Lazy
    private val procTaskService: ProcTaskService,
    private val recordsService: RecordsService,
    private val ecosTypesRegistry: EcosTypesRegistry,
    private val procTaskAttsSyncService: ProcTaskAttsSyncService,
    private val syncJobs: ProcTaskSyncJobs,
    private val locks: EcosAppLockService,
    @Lazy
    private val engineConfiguration: ProcessEngineConfigurationImpl,
    webAppApi: EcosWebAppApi,
    @Qualifier("bpmnRabbitmqConnection")
    private val bpmnRabbitmqConnection: RabbitMqConnection,
) {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val TASK_ATTS_SYNC_QUEUE = "bpmn-task-atts-sync"

        const val TASK_DOCUMENT_ATT_PREFIX = "_doc_"
        const val TASK_DOCUMENT_TYPE_ATT_PREFIX = "_doc_t_"
    }

    private lateinit var outcomeChannel: RabbitMqChannel

    @Volatile private var workerReady = false

    init {
        webAppApi.doWhenAppReady { workerReady = true }
    }

    @PostConstruct
    fun initChannels() {
        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            outcomeChannel = channel
            channel.declareQueue(TASK_ATTS_SYNC_QUEUE, true)
        }

        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            channel.addAckedConsumer(TASK_ATTS_SYNC_QUEUE, TaskAttsSyncQueueMessage::class.java) { event, _ ->
                // ACK follows only after this independent, durable registration commits.
                try {
                    syncJobs.register(event.getContent())
                } catch (e: Exception) {
                    // Propagating a callback exception makes the Java client close this consumer's channel.
                    // Explicit NACK also prevents the wrapper's subsequent ACK from accepting the delivery.
                    log.warn(e) { "Failed to register task sync request; returning delivery to RabbitMQ" }
                    event.nack()
                }
            }
        }
    }

    fun syncOnTaskCreate(delegateTask: DelegateTask) {
        val document = delegateTask.getDocumentRef()
        val documentType = delegateTask.getVariable(BPMN_DOCUMENT_TYPE) as String?
        if (document.isEmpty() || documentType.isNullOrBlank()) {
            return
        }
        val documentTypeRef = EntityRef.valueOf("${AppName.EMODEL}/type@$documentType")
        val syncSettingsTyped = procTaskAttsSyncService.findMappedByType(documentTypeRef) ?: return
        val newTaskAtts = getNewAttributesForSync(document, syncSettingsTyped, SyncMode.FULL)

        log.trace { "New atts for ${delegateTask.id}: $newTaskAtts" }

        delegateTask.variablesLocal = newTaskAtts
    }

    fun syncOnDocumentChanged(docUpdatedInfo: DocumentUpdatedInfo) {
        with(docUpdatedInfo) {
            val syncSettings = procTaskAttsSyncService.findMappedByType(typeRef) ?: return
            if (syncSettings.recordSyncAtts.isEmpty() || docUpdatedInfo.newAttributes.isEmpty()) {
                return
            }

            val typeInfo = ecosTypesRegistry.getTypeInfo(typeRef) ?: error("Type not found: $typeRef")

            val atts = mutableMapOf<String, Any?>()

            syncSettings.recordSyncAtts.forEach { (taskAtt, requestAtt) ->
                if (!newAttributes.containsKey(requestAtt)) {
                    return@forEach
                }

                var afterValue = newAttributes[requestAtt]
                if (isDateAtt(requestAtt, typeInfo) && afterValue is String) {
                    afterValue = Json.mapper.convert(afterValue, Date::class.java)
                }

                atts[TASK_DOCUMENT_ATT_PREFIX + taskAtt] = afterValue
            }

            procTaskService.getTasksByDocument(documentRef.toString())
                .forEach { task ->
                    procTaskService.setVariablesLocal(task.id, atts)
                }
        }
    }

    fun fullSyncForTypeAsync(typeRef: EntityRef) {
        sendSyncRequestForTypeToQueue(typeRef, SyncMode.TYPE)
    }

    fun fullSyncForSettingsAsync(settingsRef: EntityRef) {
        val syncSettings = procTaskAttsSyncService.getSyncSettings(settingsRef) ?: return
        if (syncSettings.enabled.not()) {
            return
        }

        listOf(syncSettings)
            .toTypesByAttributes().map { it.key }
            .forEach { typeRef ->
                sendSyncRequestForTypeToQueue(typeRef, SyncMode.FULL)
            }
    }

    /**
     * Send to a separate queue from “eproc events” to avoid blocking the queue by long processing
     */
    private fun sendSyncRequestForTypeToQueue(documentTypeRef: EntityRef, mode: SyncMode) {
        val types = ecosTypesRegistry.getChildren(documentTypeRef) + documentTypeRef

        types.forEach { type ->
            val msg = TaskAttsSyncQueueMessage(type, mode, UUID.randomUUID().toString())
            outcomeChannel.publishMsg(TASK_ATTS_SYNC_QUEUE, msg)
        }
    }

    /** One bounded page per pending job; no Rabbit delivery is held while task variables are written. */
    fun processPendingSyncs() {
        if (!workerReady) return
        locks.doInSyncOrSkip("task-atts-sync-worker", Duration.ofMinutes(5)) { lock ->
            AuthContext.runAsSystem {
                syncJobs.pending().forEach { snapshot ->
                    val job = snapshot.job
                    try {
                        processSyncPage(snapshot) { check(lock.isLocked()) { "Task sync worker lock was lost" } }
                    } catch (e: Exception) {
                        // Keep the previous checkpoint. Already committed writes in this page may repeat.
                        log.warn(e) { "Task sync job ${job.ref} failed after ${job.processed} tasks; retrying from ${job.lastTaskId}" }
                        if (lock.isLocked()) {
                            try {
                                syncJobs.save(job, snapshot.entity, error = true)
                            } catch (retryError: Exception) {
                                // A takeover can also make this error checkpoint stale. Never refresh its version.
                                log.warn(retryError) { "Could not save retry checkpoint for ${job.ref}; persisted progress is retained" }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processSyncPage(snapshot: TaskAttsSyncJobSnapshot, ensureLock: () -> Unit) {
        val job = snapshot.job
        ensureLock()
        val settings = procTaskAttsSyncService.findMappedByType(job.documentType)
        if (settings == null) {
            syncJobs.save(job.copy(completed = true), snapshot.entity)
            return
        }
        val tasks = findTaskIds(job)
        val elapsed = measureTimeMillis {
            tasks.forEach { task ->
                ensureLock()
                val document = procTaskService.getVariable(task, BPMN_DOCUMENT_REF) ?: return@forEach
                val atts = getNewAttributesForSync(EntityRef.valueOf(document), settings, job.syncMode)
                procTaskService.setVariablesLocal(task, atts)
            }
        }
        val next = job.copy(
            lastTaskId = tasks.lastOrNull() ?: job.lastTaskId,
            processed = job.processed + tasks.size,
            completed = tasks.size < 50
        )
        ensureLock()
        syncJobs.save(next, snapshot.entity)
        log.info {
            "Task sync job ${job.ref} type=${job.documentType} mode=${job.syncMode}: " +
                "processed=${next.processed}, page=${tasks.size}, time=${elapsed}ms, completed=${next.completed}"
        }
    }

    /** Read IDs directly: NativeTaskQuery with an ID-only projection pollutes Camunda's entity cache. */
    private fun findTaskIds(job: TaskAttsSyncJob): List<String> {
        val prefix = engineConfiguration.databaseTablePrefix
        val sql = """
            SELECT T.ID_ FROM ${prefix}ACT_RU_TASK T
            WHERE T.ID_ > ? AND T.CREATE_TIME_ <= ?
              AND EXISTS (
                SELECT 1 FROM ${prefix}ACT_RU_VARIABLE V
                WHERE V.PROC_INST_ID_ = T.PROC_INST_ID_ AND V.TASK_ID_ IS NULL
                  AND V.NAME_ = 'documentType' AND V.TYPE_ = 'string' AND V.TEXT_ = ?
              )
            ORDER BY T.ID_ ASC LIMIT 50
        """.trimIndent()
        val dataSource = engineConfiguration.dataSource
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, job.lastTaskId)
                statement.setTimestamp(2, Timestamp.from(job.createdBefore))
                statement.setString(3, job.documentType.getLocalId())
                statement.executeQuery().use { rows ->
                    val ids = mutableListOf<String>()
                    while (rows.next()) ids.add(rows.getString(1))
                    return ids
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    private fun getNewAttributesForSync(
        document: EntityRef,
        syncSettingsTyped: TaskAttsSyncSettingsTyped,
        syncMode: SyncMode
    ): Map<String, Any?> {
        val typeInfo = ecosTypesRegistry.getTypeInfo(syncSettingsTyped.typeRef)
            ?: error("Type not found: ${syncSettingsTyped.typeRef}")
        val newTaskAtts = mutableMapOf<String, Any?>()

        val fillRecordAtts = fun() {
            val attsToRequest = mutableListOf<RequestAtt>()

            syncSettingsTyped.recordSyncAtts.forEach { (taskAtt, requestAtt) ->
                if (requestAtt == StatusConstants.ATT_STATUS) {
                    attsToRequest.add(
                        RequestAtt(
                            taskAtt = taskAtt,
                            requestAtt = StatusConstants.ATT_STATUS,
                            requestAttSchema = StatusConstants.ATT_STATUS_STR,
                            requestAttClass = String::class.java
                        )
                    )
                    return@forEach
                }

                if (requestAtt == RecordConstants.ATT_CREATED) {
                    attsToRequest.add(
                        RequestAtt(
                            taskAtt = taskAtt,
                            requestAtt = RecordConstants.ATT_CREATED,
                            requestAttSchema = RecordConstants.ATT_CREATED,
                            requestAttClass = Date::class.java
                        )
                    )
                    return@forEach
                }

                typeInfo.model.attributes.find { it.id == requestAtt }?.let { modelAtt ->
                    var isMultiple = false
                    if (modelAtt.multiple) {
                        isMultiple = procTaskAttsSyncService.getTaskSyncAttribute(taskAtt)?.multiple ?: false
                    }
                    try {
                        var (attSchema, clazz) = getAttSchemaAndClass(modelAtt.type)
                        if (isMultiple) {
                            attSchema = "$requestAtt[]$attSchema"
                            clazz = List::class.java
                        } else {
                            attSchema = "$requestAtt$attSchema"
                        }
                        attsToRequest.add(
                            RequestAtt(
                                taskAtt = taskAtt,
                                requestAtt = requestAtt,
                                requestAttSchema = attSchema,
                                requestAttClass = clazz
                            )
                        )
                    } catch (_: Exception) {
                        error(
                            "Task atts sync on document: $document with type: ${syncSettingsTyped.typeRef} error, " +
                                "because attribute <$requestAtt> have unsupported type ${modelAtt.type}"
                        )
                    }
                }
            }

            val result = recordsService.getAtts(
                document,
                attsToRequest.associate {
                    it.requestAtt to it.requestAttSchema
                }
            ).getAtts()

            attsToRequest.forEach { (taskAtt, requestAtt, _, clazz) ->
                val resultValue = result[requestAtt]
                val asObj = Json.mapper.convert(resultValue, clazz)

                newTaskAtts[TASK_DOCUMENT_ATT_PREFIX + taskAtt] = asObj
            }
        }

        val fillTypeAtts = fun() {
            val attsToRequest = mutableListOf<RequestAtt>()

            syncSettingsTyped.typeSyncAtts.forEach { (taskAtt, requestAtt) ->
                val convertToClass = if (requestAtt.endsWith("?num")) {
                    Double::class.java
                } else {
                    String::class.java
                }

                attsToRequest.add(
                    RequestAtt(
                        taskAtt = taskAtt,
                        requestAtt = requestAtt,
                        requestAttSchema = requestAtt,
                        requestAttClass = convertToClass
                    )
                )
            }

            val result = recordsService.getAtts(
                syncSettingsTyped.typeRef,
                attsToRequest.map {
                    it.requestAttSchema
                }
            ).getAtts()

            attsToRequest.forEach { (taskAtt, requestAtt, _, clazz) ->
                val resultValue = result[requestAtt]
                val asObj = Json.mapper.convert(resultValue, clazz)

                newTaskAtts[TASK_DOCUMENT_TYPE_ATT_PREFIX + taskAtt] = asObj
            }
        }

        when (syncMode) {
            SyncMode.FULL -> {
                fillRecordAtts()
                fillTypeAtts()
            }

            SyncMode.TYPE -> fillTypeAtts()
            SyncMode.RECORD -> fillRecordAtts()
        }

        return newTaskAtts.toMap()
    }

    private fun getAttSchemaAndClass(attType: AttributeType): Pair<String, Class<*>> {
        return when (attType) {
            AttributeType.ASSOC ->
                "?id" to String::class.java

            AttributeType.PERSON,
            AttributeType.AUTHORITY,
            AttributeType.AUTHORITY_GROUP ->
                ".authorityName" to String::class.java

            AttributeType.TEXT ->
                "" to String::class.java

            AttributeType.BOOLEAN -> "?bool!" to Boolean::class.java

            AttributeType.DATE,
            AttributeType.DATETIME -> "" to Date::class.java

            AttributeType.NUMBER -> "?num" to Double::class.java

            else -> error(
                "Failed to find attribute scheme for type $attType"
            )
        }
    }

    private fun isDateAtt(att: String, typeInfo: TypeInfo): Boolean {
        val modelType = typeInfo.model.attributes.find { it.id == att }?.type
        return modelType == AttributeType.DATE || modelType == AttributeType.DATETIME
    }

    private data class RequestAtt(
        val taskAtt: String,
        val requestAtt: String,
        val requestAttSchema: String,
        val requestAttClass: Class<out Any>
    )
}

data class DocumentUpdatedInfo(
    val documentRef: EntityRef,
    val typeRef: EntityRef,
    val newAttributes: Map<String, Any?> = emptyMap()
) {

    init {
        check(documentRef.getLocalId().isNotEmpty()) { "Document ref should have local id" }
        check(typeRef.getLocalId().isNotEmpty()) { "Type ref should have local id" }
    }
}

data class TaskAttsSyncQueueMessage(
    val documentTypeRef: EntityRef,
    val syncMode: SyncMode = SyncMode.FULL,
    val requestId: String = ""
)

enum class SyncMode {
    FULL,
    TYPE,
    RECORD
}
