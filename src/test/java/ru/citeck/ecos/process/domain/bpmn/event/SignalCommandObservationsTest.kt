package ru.citeck.ecos.process.domain.bpmn.event

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.camunda.bpm.engine.impl.cfg.CommandChecker
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.interceptor.CommandContext
import org.camunda.bpm.engine.impl.persistence.deploy.cache.DeploymentCache
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionManager
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.micrometer.EcosMicrometerContext
import ru.citeck.ecos.micrometer.obs.EcosObs
import ru.citeck.ecos.micrometer.obs.EcosObsContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_EVENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation.BpmnObsConvention
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation.BpmnObsCtx
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation.BpmnObsCtx.SignalPhase
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.SendSignalEventByIdCmd
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

class SignalCommandObservationsTest {

    private val registry = ObservationRegistry.create()
    private val stopped = mutableListOf<Observation.Context>()
    private val parents = mutableMapOf<SignalPhase, String?>()
    private val checker = mock<CommandChecker>()
    private val manager = mock<EventSubscriptionManager>()
    private val cache = mock<DeploymentCache>()
    private val configuration = mock<ProcessEngineConfigurationImpl> {
        on { deploymentCache } doReturn cache
        on { commandCheckers } doReturn listOf(checker)
    }
    private val commandContext = mock<CommandContext> {
        on { eventSubscriptionManager } doReturn manager
        on { processEngineConfiguration } doReturn configuration
    }
    private val procDefService = mock<ProcDefService>()
    private val dataProvider = mock<ProcDefRevDataProvider>()
    private val bpmnIO = mock<BpmnIO>()
    private val event = BpmnDataValue.create(
        DataValue.create(
            mapOf(
                "record" to "emodel/activity@record-1",
                "recordType" to "emodel/type@activity",
                "secret" to "private-payload"
            )
        )
    )
    private val meter = object : EcosMicrometerContext {
        override fun createObs(name: String): EcosObs {
            return createObs(EcosObsContext(name))
        }

        override fun createObs(context: EcosObsContext): EcosObs {
            return EcosObs(Observation.createNotStarted(context.name, { context }, registry))
        }
    }

    @BeforeEach
    fun setUp() {

        Context.setProcessEngineConfiguration(configuration)
        registry.observationConfig()
            .observationConvention(BpmnObsConvention())
            .observationHandler(object : ObservationHandler<Observation.Context> {
                override fun supportsContext(context: Observation.Context): Boolean {
                    return context is BpmnObsCtx.SignalCommand
                }

                override fun onStart(context: Observation.Context) {

                    val phase = (context as BpmnObsCtx.SignalCommand).phase
                    parents[phase] = context.parentObservation?.contextView?.name
                }

                override fun onStop(context: Observation.Context) {
                    stopped.add(context)
                }
            })
    }

    @AfterEach
    fun tearDown() {
        Context.removeProcessEngineConfiguration()
    }

    @Test
    fun activeCatchSignalRetainsParentIdentityAndAuthorizationOrder() {

        val subscription = catchSubscription()
        Observation.createNotStarted("signal", registry).observe {
            command().execute(commandContext)
        }

        inOrder(checker, subscription) {
            verify(checker).checkUpdateProcessInstanceById("instance-1")
            verify(subscription).eventReceived(
                check { assertSame(event, (it as Map<*, *>)[BPMN_EVENT]) },
                eq(false)
            )
        }
        assertEquals("signal", parents[SignalPhase.COMMAND])
        assertEquals(SignalPhase.COMMAND.observationName, parents[SignalPhase.SUBSCRIPTIONS_FIND])
        assertEquals(SignalPhase.NOTIFY_EXECUTIONS.observationName, parents[SignalPhase.EVENT_RECEIVED])
        val receive = span(SignalPhase.EVENT_RECEIVED)
        assertSame(subscription, receive.subscription)
        assertEquals("subscription-1", receive.getHighCardinalityKeyValue("subscription.id")!!.value)
        assertTrue(
            stopped.all { context ->
                context.highCardinalityKeyValues.map { it.key } == listOf("subscription.id")
            }
        )
        assertNull(registry.currentObservation)
    }

    @Test
    fun inactiveCatchSignalIsNotDelivered() {

        val subscription = catchSubscription(ended = true)
        command().execute(commandContext)

        verify(subscription, never()).eventReceived(any(), any())
        assertNotNull(span(SignalPhase.EXECUTION_CHECK))
        assertFalse(parents.containsKey(SignalPhase.EVENT_RECEIVED))
        assertNull(registry.currentObservation)
    }

    @Test
    fun missingSubscriptionFinishesCommandWithoutDelivery() {

        command().execute(commandContext)

        assertNotNull(span(SignalPhase.SUBSCRIPTIONS_FIND))
        assertNotNull(span(SignalPhase.COMMAND))
        verifyNoInteractions(checker, procDefService)
        assertFalse(parents.containsKey(SignalPhase.EVENT_RECEIVED))
        assertFalse(parents.containsKey(SignalPhase.INSTANCE_START))
        assertNull(registry.currentObservation)
    }

    @Test
    fun authorizationFailurePreventsDeliveryAndClosesScopes() {

        val subscription = catchSubscription()
        val failure = IllegalStateException("denied")
        whenever(checker.checkUpdateProcessInstanceById("instance-1")).thenThrow(failure)

        assertSame(failure, assertThrows(IllegalStateException::class.java) { command().execute(commandContext) })
        assertSame(failure, span(SignalPhase.AUTHORIZE_CATCH).error)
        assertSame(failure, span(SignalPhase.COMMAND).error)
        verify(subscription, never()).eventReceived(any(), any())
        assertNull(registry.currentObservation)
    }

    @Test
    fun deliveryFailureIsRecordedWithoutWrappingTheException() {

        val subscription = catchSubscription()
        val failure = IllegalStateException("delivery failed")
        whenever(subscription.eventReceived(any(), eq(false))).thenThrow(failure)

        assertSame(failure, assertThrows(IllegalStateException::class.java) { command().execute(commandContext) })
        for (phase in listOf(SignalPhase.EVENT_RECEIVED, SignalPhase.NOTIFY_EXECUTIONS, SignalPhase.COMMAND)) {
            assertSame(failure, span(phase).error)
        }
        assertFalse(parents.containsKey(SignalPhase.START_INSTANCES))
        assertNull(registry.currentObservation)
    }

    @Test
    fun startSignalSeparatesCreationFromStartAndKeepsVariables() {

        val instance = startSubscription()
        command().execute(commandContext)

        verify(instance).businessKey = "emodel/activity@record-1"
        verify(instance).start(check {
            assertSame(event, it[BPMN_EVENT])
            assertEquals("emodel/activity@record-1", it[BPMN_DOCUMENT_REF])
            assertEquals("activity", it[BPMN_DOCUMENT_TYPE])
        })
        assertEquals(SignalPhase.START_INSTANCES.observationName, parents[SignalPhase.INSTANCE_CREATE])
        assertEquals(SignalPhase.START_INSTANCES.observationName, parents[SignalPhase.INSTANCE_START])
        assertNull(registry.currentObservation)
    }

    @Test
    fun noOpContextStillDeliversTheSignal() {

        val subscription = catchSubscription()
        command(EcosMicrometerContext.NOOP).execute(commandContext)

        verify(subscription).eventReceived(any(), eq(false))
        assertTrue(stopped.isEmpty())
    }

    private fun catchSubscription(ended: Boolean = false): EventSubscriptionEntity {

        val execution = mock<ExecutionEntity> {
            on { isEnded } doReturn ended
            on { isCanceled } doReturn false
        }
        val subscription = mock<EventSubscriptionEntity> {
            on { id } doReturn "subscription-1"
            on { executionId } doReturn "execution-1"
            on { processInstanceId } doReturn "instance-1"
            on { getExecution() } doReturn execution
        }
        whenever(manager.findEventSubscriptionById("subscription-1")).thenReturn(subscription)
        return subscription
    }

    private fun startSubscription(): ExecutionEntity {

        val activity = mock<ActivityImpl>()
        val instance = mock<ExecutionEntity>()
        val definition = mock<ProcessDefinitionEntity> {
            on { deploymentId } doReturn "deployment-1"
            on { findActivity("activity-1") } doReturn activity
            on { createProcessInstanceForInitial(activity) } doReturn instance
        }
        val subscription = mock<EventSubscriptionEntity> {
            on { id } doReturn "subscription-1"
            on { configuration } doReturn "definition-1"
            on { activityId } doReturn "activity-1"
        }
        val revision = mock<ProcDefRevDto> {
            on { loadData(dataProvider) } doReturn "definition".toByteArray()
        }
        val model = mock<BpmnDefinitionDef> {
            on { enabled } doReturn true
        }
        whenever(manager.findEventSubscriptionById("subscription-1")).thenReturn(subscription)
        whenever(cache.findDeployedProcessDefinitionById("definition-1")).thenReturn(definition)
        whenever(procDefService.getProcessDefRevByDeploymentId("deployment-1")).thenReturn(revision)
        whenever(bpmnIO.importEcosBpmn("definition")).thenReturn(model)
        return instance
    }

    private fun command(context: EcosMicrometerContext = meter): SendSignalEventByIdCmd {

        return SendSignalEventByIdCmd(
            "subscription-1",
            event,
            procDefService,
            dataProvider,
            bpmnIO,
            context
        )
    }

    private fun span(phase: SignalPhase): BpmnObsCtx.SignalCommand {
        return stopped.filterIsInstance<BpmnObsCtx.SignalCommand>().single { it.phase == phase }
    }
}
