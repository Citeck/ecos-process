package ru.citeck.ecos.process.domain.proctask.attssync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
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
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.*
import kotlin.system.measureTimeMillis

@Component
class ProcTaskAttsSynchronizer(
    @Lazy
    private val procTaskService: ProcTaskService,
    private val recordsService: RecordsService,
    private val ecosTypesRegistry: EcosTypesRegistry,
    private val procTaskAttsSyncService: ProcTaskAttsSyncService,
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

    init {
        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            outcomeChannel = channel
            channel.declareQueue(TASK_ATTS_SYNC_QUEUE, true)
        }

        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            channel.addAckedConsumer(TASK_ATTS_SYNC_QUEUE, TaskAttsSyncQueueMessage::class.java) { event, _ ->
                onSyncRequestReceived(event.getContent())
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
            val msg = TaskAttsSyncQueueMessage(type, mode)
            outcomeChannel.publishMsg(TASK_ATTS_SYNC_QUEUE, msg)
        }
    }

    private fun onSyncRequestReceived(msg: TaskAttsSyncQueueMessage) {
        val typeRef = msg.documentTypeRef
        val syncSettingsTyped = procTaskAttsSyncService.findMappedByType(typeRef) ?: return

        val batchSize = 50
        var skipCount = 0
        var totalFound = 0

        log.info { "Start full sync for typeRef <$typeRef>" }

        val syncTime = measureTimeMillis {

            var tasks = procTaskService.getTaskIdsByDocumentType(
                typeRef.getLocalId(),
                skipCount,
                batchSize
            )

            while (tasks.isNotEmpty()) {
                log.info { "Found ${tasks.size} for full sync. Skip: $skipCount, total: $totalFound" }

                val time = measureTimeMillis {
                    tasks.forEach { task ->
                        val document = procTaskService.getVariable(task, BPMN_DOCUMENT_REF) ?: return@forEach
                        val documentRef = EntityRef.valueOf(document)

                        val newTaskAtts = getNewAttributesForSync(documentRef, syncSettingsTyped, msg.syncMode)

                        log.trace { "New atts for $task: $newTaskAtts" }

                        procTaskService.setVariablesLocal(task, newTaskAtts)
                    }

                    totalFound += tasks.size
                    skipCount += batchSize

                    tasks = procTaskService.getTaskIdsByDocumentType(
                        typeRef.getLocalId(),
                        skipCount,
                        batchSize
                    )
                }

                log.info { "Processed ${tasks.size} tasks for $time ms" }
            }
        }

        log.info {
            "Full sync for type <$typeRef> finished. " +
                "Updated $totalFound tasks. Time: $syncTime ms"
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
                    val (attSchema, clazz) = when (modelAtt.type) {
                        AttributeType.ASSOC -> "$requestAtt?id" to String::class.java

                        AttributeType.PERSON,
                        AttributeType.AUTHORITY,
                        AttributeType.AUTHORITY_GROUP -> "$requestAtt.authorityName" to String::class.java

                        AttributeType.TEXT -> requestAtt to String::class.java

                        AttributeType.BOOLEAN -> "$requestAtt?bool!" to Boolean::class.java

                        AttributeType.DATE,
                        AttributeType.DATETIME -> requestAtt to Date::class.java

                        AttributeType.NUMBER -> "$requestAtt?num" to Double::class.java
                        else -> error(
                            "Task atts sync on document: $document with type: ${syncSettingsTyped.typeRef} error, " +
                                "because attribute <$requestAtt> have unsupported type ${modelAtt.type}"
                        )
                    }

                    attsToRequest.add(
                        RequestAtt(
                            taskAtt = taskAtt,
                            requestAtt = requestAtt,
                            requestAttSchema = attSchema,
                            requestAttClass = clazz
                        )
                    )
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

private data class TaskAttsSyncQueueMessage(
    val documentTypeRef: EntityRef,
    val syncMode: SyncMode
)

private enum class SyncMode {
    FULL,
    TYPE,
    RECORD
}
