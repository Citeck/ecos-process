package ru.citeck.ecos.process.domain.bpmn.process.delayed

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthUser
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.Instant

@Service
class BpmnDelayedStartService(
    private val recordsService: RecordsService,
    private val bpmnProcessService: BpmnProcessService,
    private val authoritiesApi: EcosAuthoritiesApi,
    private val workspaceService: WorkspaceService,

    @Value("\${ecos-process.bpmn.async-start-process.delayed-retry.delay}")
    private val delayConfig: String,

    @Value("\${ecos-process.bpmn.async-start-process.delayed-retry.last-delay-doubling:false}")
    private val lastDelayDoubling: Boolean
) {

    companion object {
        private val log = KotlinLogging.logger {}

        fun parseDelays(delayStr: String): List<Duration> {
            if (delayStr.isBlank()) return emptyList()
            return delayStr.trim().split("\\s+".toRegex()).map { token ->
                val value = token.dropLast(1).toLong()
                when (token.last()) {
                    's' -> Duration.ofSeconds(value)
                    'm' -> Duration.ofMinutes(value)
                    'h' -> Duration.ofHours(value)
                    'd' -> Duration.ofDays(value)
                    else -> error("Unknown duration unit in '$token'. Supported: s, m, h, d")
                }
            }
        }

        fun getDelay(retryCount: Int, delays: List<Duration>, lastDelayDoubling: Boolean): Duration {
            if (delays.isEmpty()) return Duration.ofMinutes(1)
            if (retryCount < delays.size) return delays[retryCount]
            var delay = delays.last()
            if (!lastDelayDoubling) return delay
            val doublings = (retryCount - delays.size + 1).coerceAtMost(20)
            repeat(doublings) { delay = delay.multipliedBy(2) }
            return delay
        }
    }

    private val delays: List<Duration> = parseDelays(delayConfig)

    fun saveForDelayedRetry(request: StartProcessRequest, completed: Boolean = false) {

        val atts = RecordAtts(BpmnDelayedStartCmdDesc.getRef(""))

        atts[BpmnDelayedStartCmdDesc.ATT_PROCESS_ID] = request.processId
        atts[BpmnDelayedStartCmdDesc.ATT_WORKSPACE] = request.workspace
        atts[BpmnDelayedStartCmdDesc.ATT_BUSINESS_KEY] = request.businessKey ?: ""
        atts[BpmnDelayedStartCmdDesc.ATT_VARIABLES] = request.variables
        atts[BpmnDelayedStartCmdDesc.ATT_RETRY_COUNT] = 0
        atts[BpmnDelayedStartCmdDesc.ATT_LAST_ERROR] = ""

        val nextRetryTime: Instant
        if (completed) {
            nextRetryTime = Instant.EPOCH
            atts[BpmnDelayedStartCmdDesc.ATT_COMPLETED_AT] = Instant.now()
        } else {
            val firstDelay = getDelay(0)
            nextRetryTime = Instant.now().plus(firstDelay)
        }
        atts[BpmnDelayedStartCmdDesc.ATT_NEXT_RETRY_TIME] = nextRetryTime

        AuthContext.runAsSystem {
            recordsService.mutate(atts)
        }

        if (completed) {
            log.info {
                "Saved completed start command for process '${request.processId}', " +
                    "businessKey='${request.businessKey}'"
            }
        } else {
            log.info {
                "Saved delayed start command for process '${request.processId}', " +
                    "businessKey='${request.businessKey}', next retry at $nextRetryTime"
            }
        }
    }

    fun processDelayedCommands() {
        val now = Instant.now()
        val processedRefs = mutableSetOf<EntityRef>()
        var totalProcessed = 0

        while (true) {
            val records = AuthContext.runAsSystem {
                recordsService.query(
                    RecordsQuery.create {
                        withSourceId(BpmnDelayedStartCmdDesc.BPMN_DELAYED_START_CMD_SOURCE_ID)
                        withLanguage(PredicateService.LANGUAGE_PREDICATE)
                        withQuery(
                            Predicates.and(
                                Predicates.le(BpmnDelayedStartCmdDesc.ATT_NEXT_RETRY_TIME, now),
                                Predicates.empty(BpmnDelayedStartCmdDesc.ATT_COMPLETED_AT),
                                Predicates.or(
                                    Predicates.empty(BpmnDelayedStartCmdDesc.ATT_LAST_PROC_RUN_TIME),
                                    Predicates.notEq(BpmnDelayedStartCmdDesc.ATT_LAST_PROC_RUN_TIME, now)
                                )
                            )
                        )
                        withMaxItems(1000)
                    },
                    DelayedStartCommandAtts::class.java
                ).getRecords()
            }.filter { it.ref !in processedRefs }

            if (records.isEmpty()) {
                break
            }

            log.debug { "Found ${records.size} delayed start commands to process (total so far: $totalProcessed)" }

            for (record in records) {
                processedRefs.add(record.ref)
                processCommand(record, now)
            }

            totalProcessed += records.size
        }

        if (totalProcessed > 0) {
            log.info { "Processed $totalProcessed delayed start commands" }
        }
    }

    private fun processCommand(cmd: DelayedStartCommandAtts, runStartTime: Instant) {
        val request = StartProcessRequest(
            workspace = cmd.workspace,
            processId = cmd.processId,
            businessKey = cmd.businessKey.ifEmpty { null },
            variables = cmd.variables.asMap(String::class.java, Any::class.java)
        )

        try {
            startProcessWithAuth(request)

            AuthContext.runAsSystem {
                val atts = RecordAtts(cmd.ref)
                atts[BpmnDelayedStartCmdDesc.ATT_COMPLETED_AT] = Instant.now()
                recordsService.mutate(atts)
            }

            val elapsed = Duration.between(cmd.created, Instant.now())
            log.info {
                "Delayed start succeeded for process '${request.processId}', ref=${cmd.ref}, " +
                    "attempts=${cmd.retryCount + 1}, elapsed=$elapsed"
            }
        } catch (e: Exception) {
            val nextRetryCount = cmd.retryCount + 1
            val nextDelay = getDelay(nextRetryCount)
            val nextRetryTime = Instant.now().plus(nextDelay)

            log.warn(e) {
                "Delayed start failed for process '${request.processId}', ref=${cmd.ref}. " +
                    "Retry $nextRetryCount, next attempt in $nextDelay (at $nextRetryTime)"
            }

            try {
                AuthContext.runAsSystem {
                    val atts = RecordAtts(cmd.ref)
                    atts[BpmnDelayedStartCmdDesc.ATT_RETRY_COUNT] = nextRetryCount
                    atts[BpmnDelayedStartCmdDesc.ATT_NEXT_RETRY_TIME] = nextRetryTime
                    atts[BpmnDelayedStartCmdDesc.ATT_LAST_PROC_RUN_TIME] = runStartTime
                    atts[BpmnDelayedStartCmdDesc.ATT_LAST_ATTEMPT_TIME] = Instant.now()
                    atts[BpmnDelayedStartCmdDesc.ATT_LAST_ERROR] = getRootCauseStackTrace(e)
                    recordsService.mutate(atts)
                }
            } catch (updateEx: Exception) {
                log.error(updateEx) {
                    "Failed to update delayed start command ref=${cmd.ref} after process start failure"
                }
            }
        }
    }

    fun startProcessWithAuth(request: StartProcessRequest) {
        val workflowInitiator = request.variables[BPMN_WORKFLOW_INITIATOR]?.toString()
        TxnContext.doInNewTxn {
            if (workflowInitiator.isNullOrBlank() || workflowInitiator == AuthUser.SYSTEM) {
                runAsSystemForWorkspace(request.workspace) {
                    bpmnProcessService.startProcess(request)
                }
            } else {
                val initiatorAuthorities = authoritiesApi.getUserAuthorities(workflowInitiator)
                AuthContext.runAsFull(workflowInitiator, initiatorAuthorities) {
                    bpmnProcessService.startProcess(request)
                }
            }
        }
    }

    private fun <T> runAsSystemForWorkspace(workspace: String, action: () -> T): T {
        return if (workspaceService.isWorkspaceWithGlobalEntities(workspace)) {
            AuthContext.runAsSystem(action)
        } else {
            workspaceService.runAsWsSystem(workspace, action)
        }
    }

    private fun getRootCauseStackTrace(e: Throwable): String {
        var root: Throwable = e
        while (root.cause != null && root.cause !== root) {
            root = root.cause!!
        }
        val sw = StringWriter()
        root.printStackTrace(PrintWriter(sw))
        return sw.toString().take(10_000)
    }

    private fun getDelay(retryCount: Int): Duration {
        return getDelay(retryCount, delays, lastDelayDoubling)
    }

    data class DelayedStartCommandAtts(
        @AttName("?id")
        val ref: EntityRef = EntityRef.EMPTY,
        val processId: String = "",
        val workspace: String = "",
        val businessKey: String = "",
        val variables: ObjectData = ObjectData.create(),
        val retryCount: Int = 0,
        val nextRetryTime: Instant = Instant.EPOCH,
        @AttName("_created")
        val created: Instant = Instant.EPOCH
    )
}
