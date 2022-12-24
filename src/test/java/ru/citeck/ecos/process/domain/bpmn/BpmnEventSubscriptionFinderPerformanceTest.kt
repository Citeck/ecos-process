package ru.citeck.ecos.process.domain.bpmn

// TODO: uncomment
/*
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnEventSubscriptionFinderPerformanceTest {

    @Autowired
    private lateinit var camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @BeforeAll
    fun setUp() {

        val processData = ResourceUtils.getFile("classpath:test/bpmn/large-test-process-start-event.bpmn.xml")
            .readText(StandardCharsets.UTF_8)
        val signalName = "signal-start-event-0"
        val procId = "large-test-process-start-event"

        for (i in 0 until 100) {
            val newId = "$procId-$i"

            val bpmnData = processData
                .replace(signalName, "signal-start-event-$i")
                .replace(procId, newId)

            log.info { "Deploying process $newId" }

            saveAndDeployBpmnFromString(bpmnData, newId)
        }
    }

    @Test
    fun `find all deployed subscriptions should go without a memory leak `() {
        camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
    }
}*/
