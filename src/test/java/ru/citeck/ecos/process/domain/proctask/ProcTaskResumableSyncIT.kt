package ru.citeck.ecos.process.domain.proctask

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.DockerClientFactory
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.proctask.attssync.*
import ru.citeck.ecos.process.domain.proctask.config.TASK_SYNC_JOB_SOURCE
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.lock.EcosLock
import ru.citeck.ecos.webapp.api.lock.EcosLockApi
import ru.citeck.ecos.webapp.lib.lock.EcosAppLockService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Real ecos-data/PostgreSQL, Rabbit consumer, Records projections and Camunda task writes. */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class, ProcTaskResumableSyncIT.TestConfig::class])
@Timeout(180)
class ProcTaskResumableSyncIT {
    @Autowired private lateinit var records: RecordsService
    @Autowired private lateinit var runtime: RuntimeService
    @Autowired private lateinit var repository: RepositoryService
    @Autowired private lateinit var tasks: TaskService
    @Autowired private lateinit var helper: BpmnProcHelper
    @Autowired private lateinit var types: EcosTypesRegistry
    @Autowired private lateinit var sync: ProcTaskAttsSynchronizer
    @Autowired private lateinit var jobs: ProcTaskSyncJobs
    @Autowired private lateinit var locks: EcosAppLockService
    @Autowired private lateinit var engine: ProcessEngineConfigurationImpl
    @Autowired private lateinit var procTasks: ProcTaskService
    @Autowired private lateinit var settings: ProcTaskAttsSyncService
    @Autowired private lateinit var webApp: EcosWebAppApi
    @Autowired private lateinit var probe: FailureProbe
    @Autowired
    @Qualifier("bpmnRabbitmqConnection")
    private lateinit var rabbit: RabbitMqConnection

    private lateinit var type: String
    private lateinit var typeRef: EntityRef
    private lateinit var deployment: String

    @BeforeEach
    fun prepare() = AuthContext.runAsSystem {
        type = "resumable-task-sync-${UUID.randomUUID()}"
        typeRef = EntityRef.valueOf("emodel/type@$type")
        // Commit initial ecos-data DDL before an async registration races a query in this fixture.
        jobs.register(TaskAttsSyncQueueMessage(typeRef, SyncMode.FULL, UUID.randomUUID().toString()))
        helper.cleanTaskAttsSyncSettings()
        allJobs().forEach { records.delete(it.ref) }
        probe.beforeCheckpoint.set(null)
        probe.writes.clear()
        probe.trackedTasks.clear()
        probe.pageAttempts.set(0)
        probe.targetType = typeRef
        probe.total.set(0)
        probe.failAt.set(-1)
        probe.failCheckpoint.set(false)
        probe.registerAttempts.clear()
        probe.failBeforeRegister.set(false)
        probe.failAfterRegister.set("")
        types.setValue(
            type,
            TypeDef.create().withId(type).withModel(
                TypeModelDef.create {
                    withAttributes(
                        listOf(
                            AttributeDef.create {
                                withId("payload")
                                withType(AttributeType.TEXT)
                            }
                        )
                    )
                }
            ).build()
        )
        records.register(DocumentDao())
        deployment = repository.createDeployment().addModelInstance(
            "resumable.bpmn",
            Bpmn.createExecutableProcess(type).startEvent().userTask("wait").endEvent().done()
        ).deploy().id
    }

    @AfterEach
    fun cleanup() = AuthContext.runAsSystem {
        helper.cleanTaskAttsSyncSettings()
        repository.deleteDeployment(deployment, true)
        allJobs().forEach { records.delete(it.ref) }
    }

    @Test
    fun `broker acknowledges durable job before any task work and fresh worker resumes after task removal`(): Unit = AuthContext.runAsSystem {
        createTasks(120)
        jobs.register(TaskAttsSyncQueueMessage(typeRef, SyncMode.TYPE, UUID.randomUUID().toString()))
        createSettings()
        await().atMost(Duration.ofSeconds(20)).until { allJobs().size == 1 }
        assertBrokerAcknowledged()
        assertThat(probe.total.get()).isZero()
        advancePage(sync)
        val first = allJobs().single()
        assertThat(first.processed).isEqualTo(50)
        assertThat(probe.total.get()).isEqualTo(50)
        assertThat(first.completed).isFalse()
        // OFFSET would skip ten remaining tasks after deleting ten already-visited tasks.
        probe.writes.keys.sorted().take(10).forEach { tasks.complete(it) }
        val recovered = freshWorker()
        advancePage(recovered)
        advancePage(recovered)
        assertThat(allJobs().single().processed).isEqualTo(120)
        assertThat(allJobs().single().completed).isTrue()
        assertThat(probe.writes).hasSize(120)
        assertThat(probe.writes.values.map { it.get() }).containsOnly(1)
        tasks.createTaskQuery().processVariableValueEquals(BPMN_DOCUMENT_TYPE, type).list().forEach {
            assertThat(tasks.getVariableLocal(it.id, "_doc_payload")).isEqualTo("synced")
        }
    }

    @Test
    fun `failure inside a page repeats only its committed prefix after worker recreation`(): Unit = AuthContext.runAsSystem {
        createTasks(125)
        createSettings()
        await().atMost(Duration.ofSeconds(20)).until { allJobs().size == 1 }
        advancePage(sync)
        val saved = allJobs().single()
        probe.failAt.set(74)
        advancePage(sync)
        assertThat(probe.total.get()).isEqualTo(74)
        assertThat(allJobs().single().lastTaskId).isEqualTo(saved.lastTaskId)
        assertThat(allJobs().single().processed).isEqualTo(50)
        makeDue(saved)
        val recovered = freshWorker()
        advancePage(recovered)
        advancePage(recovered)
        assertThat(allJobs().single().completed).isTrue()
        assertThat(allJobs().single().processed).isEqualTo(125)
        assertThat(probe.total.get()).isEqualTo(149)
        assertThat(probe.writes.values.count { it.get() == 2 }).isEqualTo(24)
        assertThat(probe.writes.filterKeys { it <= saved.lastTaskId }.values.map { it.get() }).containsOnly(1)
    }

    @Test
    fun `failure saving checkpoint does not lose a page or restart the completed prefix`(): Unit = AuthContext.runAsSystem {
        createTasks(120)
        createSettings()
        await().atMost(Duration.ofSeconds(20)).until { allJobs().size == 1 }
        advancePage(sync)
        val saved = allJobs().single()
        probe.failCheckpoint.set(true)
        advancePage(sync)
        assertThat(allJobs().single().processed).isEqualTo(50)
        assertThat(probe.total.get()).isEqualTo(100)
        makeDue(saved)
        val recovered = freshWorker()
        advancePage(recovered)
        advancePage(recovered)
        assertThat(allJobs().single().processed).isEqualTo(120)
        assertThat(allJobs().single().completed).isTrue()
        assertThat(probe.total.get()).isEqualTo(170)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `registration failure requeues without losing consumer and legacy messages are accepted`(beforeCommit: Boolean): Unit = AuthContext.runAsSystem {
        val id = UUID.randomUUID().toString()
        probe.failAfterRegister.set(id)
        probe.failBeforeRegister.set(beforeCommit)
        rabbit.doWithNewChannel { channel ->
            channel.publishMsg("bpmn-task-atts-sync", TaskAttsSyncQueueMessage(typeRef, SyncMode.FULL, id))
            channel.publishMsg("bpmn-task-atts-sync", TaskAttsSyncQueueMessage(typeRef, SyncMode.FULL, id))
            val legacy = mapOf("documentTypeRef" to typeRef.toString(), "syncMode" to "FULL")
            channel.publishMsg("bpmn-task-atts-sync", legacy)
            channel.publishMsg("bpmn-task-atts-sync", legacy)
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted {
            assertThat(allJobs()).hasSize(3)
            assertThat(allJobs().map { it.requestId }).contains(id)
            assertThat(allJobs().count { it.requestId.startsWith("legacy:") }).isEqualTo(2)
            assertThat(probe.failAfterRegister.get()).isEmpty()
        }
        assertBrokerAcknowledged()
        assertThat(probe.registerAttempts[id]!!.get()).isGreaterThanOrEqualTo(3)
        assertThat(allJobs()).hasSize(3)
        assertThat(allJobs().map { it.requestId }).doesNotHaveDuplicates()
        repeat(3) { advancePage(sync) }
        assertThat(allJobs().map { it.completed }).containsOnly(true)
        // Identical legacy payloads carry no delivery identity: a later request must run again.
        rabbit.doWithNewChannel { channel ->
            channel.publishMsg("bpmn-task-atts-sync", mapOf("documentTypeRef" to typeRef.toString(), "syncMode" to "FULL"))
        }
        await().atMost(Duration.ofSeconds(20)).until { allJobs().size == 4 }
        assertBrokerAcknowledged()
        assertThat(allJobs().count { !it.completed }).isEqualTo(1)
        advancePage(sync)
        assertThat(allJobs().map { it.completed }).containsOnly(true)
        // A new intentional request must still be allowed after an earlier completed run.
        val next = id + "-new"
        jobs.register(TaskAttsSyncQueueMessage(typeRef, SyncMode.FULL, next))
        assertThat(allJobs()).hasSize(5)
    }

    @Test
    fun `independent registration requests keep coordination state bounded`(): Unit = AuthContext.runAsSystem {
        val keys = ConcurrentHashMap.newKeySet<String>()
        val boundedLocks = EcosAppLockService(object : EcosLockApi {
            override fun getLock(key: String): EcosLock {
                keys.add(key)
                return locks.getLock(key)
            }
        })
        val repository = ProcTaskSyncJobs(records, boundedLocks)
        repeat(20) { repository.register(TaskAttsSyncQueueMessage(typeRef, SyncMode.FULL, UUID.randomUUID().toString())) }
        assertThat(allJobs()).hasSize(20)
        // The production ZooKeeper lock API caches every distinct key for the life of the JVM.
        assertThat(keys).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `paused old worker cannot roll back takeover progress on success or error`(oldPageFails: Boolean): Unit = AuthContext.runAsSystem {
        createTasks(120)
        createSettings()
        await().atMost(Duration.ofSeconds(20)).until { allJobs().size == 1 }
        var completedCursor = ""
        probe.beforeCheckpoint.set {
            // Model a new owner while the paused old lease still reports acquired, as in Curator.
            // Only ownership is injected; both workers use the actual DB, Records and Camunda.
            val takeover = freshWorker(availableWorkerLocks())
            repeat(3) { advancePage(takeover) }
            val completed = allJobs().single()
            assertThat(completed.completed).isTrue()
            assertThat(completed.processed).isEqualTo(120)
            completedCursor = completed.lastTaskId
            if (oldPageFails) error("injected old-owner failure after takeover")
        }
        advancePage(sync)
        val afterOldWorker = allJobs().single()
        assertThat(afterOldWorker.completed).isTrue()
        assertThat(afterOldWorker.processed).isEqualTo(120)
        assertThat(afterOldWorker.lastTaskId).isEqualTo(completedCursor)
        assertThat(probe.writes).hasSize(120)
        assertThat(probe.total.get()).isEqualTo(170)
    }

    private fun availableWorkerLocks() = EcosAppLockService(object : EcosLockApi {
        override fun getLock(key: String) = object : EcosLock {
            override fun getKey() = key
            override fun acquire(timeout: Duration) = true
            override fun isAcquiredInThisProcess() = true
            override fun release() {}
            override fun dispose() {}
        }
    })

    private fun createTasks(count: Int) {
        repeat(count) {
            val process = runtime.startProcessInstanceByKey(
                type,
                mapOf(BPMN_DOCUMENT_TYPE to type, BPMN_DOCUMENT_REF to "eproc/resumable-sync-doc@doc-$it")
            )
            probe.trackedTasks.add(tasks.createTaskQuery().processInstanceId(process.id).singleResult().id)
        }
    }

    private fun createSettings() {
        helper.createAttsSync(
            type,
            true,
            TaskAttsSyncSource.RECORD,
            "Resumable sync",
            listOf(TaskSyncAttribute("payload", AttributeType.TEXT, ecosTypes = listOf(TaskSyncAttributeType(typeRef, "payload"))))
        )
    }

    private fun allJobs(): List<TaskAttsSyncJob> = records.query(
        RecordsQuery.create {
            withSourceId(TASK_SYNC_JOB_SOURCE)
            withQuery(Predicates.and(Predicates.eq("documentType", typeRef.toString()), Predicates.eq("syncMode", "FULL")))
            withMaxItems(100)
        },
        TaskAttsSyncJob::class.java
    ).getRecords()

    private fun advancePage(worker: ProcTaskAttsSynchronizer) {
        val attempts = probe.pageAttempts.get()
        val writes = probe.total.get()
        val checkpoints = allJobs().associate { it.ref to Triple(it.processed, it.lastTaskId, it.completed) }
        // Other types and modes legitimately share the durable queue.
        await().pollInSameThread().pollInterval(Duration.ofMillis(50)).atMost(Duration.ofSeconds(30)).until {
            worker.processPendingSyncs()
            probe.pageAttempts.get() > attempts ||
                probe.total.get() > writes ||
                allJobs().any { checkpoints[it.ref] != Triple(it.processed, it.lastTaskId, it.completed) }
        }
    }

    private fun makeDue(job: TaskAttsSyncJob) {
        records.mutate(RecordAtts(job.ref).apply { setAtt("nextAttempt", Instant.EPOCH) })
    }

    private fun freshWorker(workerLocks: EcosAppLockService = locks): ProcTaskAttsSynchronizer = ProcTaskAttsSynchronizer(
        procTasks, records, types, settings, ProcTaskSyncJobs(records, locks), workerLocks, engine, webApp, rabbit
    )

    /** Ask the actual test broker, not a mocked ACK method or Ready-only AMQP counter. */
    private fun assertBrokerAcknowledged() {
        var port = 0
        rabbit.doWithNewChannel { port = it.getOriginalChannel().connection.port }
        val docker = DockerClientFactory.instance().client()
        val container = docker.listContainersCmd().exec().single { c ->
            c.ports.any { it.privatePort == 5672 && it.publicPort == port }
        }
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val output = ByteArrayOutputStream()
            val command = docker.execCreateCmd(container.id).withAttachStdout(true).withAttachStderr(true)
                .withCmd("rabbitmqctl", "list_queues", "-q", "name", "messages_ready", "messages_unacknowledged", "--formatter=json").exec()
            val done = CountDownLatch(1)
            val callback = object : ResultCallback.Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    output.write(frame.payload)
                }
                override fun onComplete() {
                    done.countDown()
                }
                override fun onError(error: Throwable) {
                    done.countDown()
                    super.onError(error)
                }
            }
            callback.use {
                docker.execStartCmd(command.id).exec(callback)
                assertThat(done.await(20, TimeUnit.SECONDS)).isTrue()
            }
            assertThat(docker.inspectExecCmd(command.id).exec().exitCodeLong).isZero()
            val rows = Json.mapper.read(output.toString(), Array<QueueInfo>::class.java)!!
            val queue = rows.single { it.name == "bpmn-task-atts-sync" }
            assertThat(queue.messages_ready).isZero()
            assertThat(queue.messages_unacknowledged).isZero()
        }
    }

    data class QueueInfo(val name: String = "", val messages_ready: Int = -1, val messages_unacknowledged: Int = -1)

    @Aspect
    class FailureProbe {
        val writes = ConcurrentHashMap<String, AtomicInteger>()
        val trackedTasks = ConcurrentHashMap.newKeySet<String>()
        val pageAttempts = AtomicInteger()
        var targetType: EntityRef = EntityRef.EMPTY
        val total = AtomicInteger()
        val failAt = AtomicInteger(-1)
        val failCheckpoint = AtomicBoolean()
        val beforeCheckpoint = java.util.concurrent.atomic.AtomicReference<(() -> Unit)?>(null)
        val failAfterRegister = java.util.concurrent.atomic.AtomicReference("")
        val failBeforeRegister = AtomicBoolean()
        val registerAttempts = ConcurrentHashMap<String, AtomicInteger>()

        @Around("execution(* ru.citeck.ecos.process.domain.proctask.service.ProcTaskServiceImpl.setVariablesLocal(..))")
        fun write(point: ProceedingJoinPoint): Any? {
            if (point.args[0] !in trackedTasks || !(point.args[1] as Map<*, *>).containsKey("_doc_payload")) return point.proceed()
            if (total.get() == failAt.get()) {
                failAt.set(-1)
                error("injected task write failure")
            }
            val result = point.proceed()
            writes.computeIfAbsent(point.args[0] as String) { AtomicInteger() }.incrementAndGet()
            total.incrementAndGet()
            return result
        }

        @Around("execution(* ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskSyncJobs.save(..))")
        fun checkpoint(point: ProceedingJoinPoint): Any? {
            val job = point.args[0] as TaskAttsSyncJob
            if (job.documentType == targetType && job.syncMode == SyncMode.FULL) {
                pageAttempts.incrementAndGet()
                beforeCheckpoint.getAndSet(null)?.invoke()
                if (failCheckpoint.compareAndSet(true, false)) error("injected checkpoint failure")
            }
            return point.proceed()
        }

        @Around("execution(* ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskSyncJobs.register(..))")
        fun register(point: ProceedingJoinPoint): Any? {
            val request = point.args[0] as TaskAttsSyncQueueMessage
            registerAttempts.computeIfAbsent(request.requestId) { AtomicInteger() }.incrementAndGet()
            val expected = failAfterRegister.get()
            val inject = expected.isNotEmpty() && expected == request.requestId && failAfterRegister.compareAndSet(expected, "")
            if (inject && failBeforeRegister.get()) error("injected failure before durable registration")
            val result = point.proceed()
            if (inject) error("injected failure after durable registration before ACK")
            return result
        }
    }

    @TestConfiguration
    class TestConfig {
        @Bean fun syncFailureProbe() = FailureProbe()
    }

    class DocumentDao :
        AbstractRecordsDao(),
        RecordsAttsDao {
        override fun getId() = "resumable-sync-doc"
        override fun getRecordsAtts(recordIds: List<String>): List<Any> = recordIds.map { Payload() }
    }
    class Payload(@AttName("payload") val payload: String = "synced")
}
