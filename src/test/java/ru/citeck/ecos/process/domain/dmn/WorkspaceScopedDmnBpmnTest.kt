package ru.citeck.ecos.process.domain.dmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.CustomWorkspaceApi
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class WorkspaceScopedDmnBpmnTest {

    enum class ProcDefType(
        val latestDefsSrcId: String,
        val getExpectedId: (id: String, ws: String) -> String
    ) {
        DMN("dmn-decision-latest", { id, _ -> "DMN_$id" }),
        BPMN("bpmn-proc-latest", { id, ws ->
            if (ws.isEmpty()) id else "$ws-sys-id..$id"
        })
    }

    @Autowired
    private lateinit var helper: BpmnProcHelper
    @Autowired
    private lateinit var records: RecordsService
    @Autowired
    private lateinit var workspaceApi: CustomWorkspaceApi

    @EnumSource(ProcDefType::class)
    @ParameterizedTest
    fun dmnBpmnLatestRecordsTest(procType: ProcDefType) {

        helper.cleanDeployments()

        val deployDef: (defId: String, ws: String) -> Unit = when (procType) {
            ProcDefType.DMN -> {
                val testDef = ResourceUtils.getFile("classpath:test/dmn/simple-dmn-test.dmn.xml").readText()
                (
                    { defId: String, ws: String ->
                        helper.saveAndDeployDmnFromString(
                            testDef.replace("ecos:defId=\"simple-dmn-test\"", "ecos:defId=\"$defId\"")
                                .replace("Decision_simple-dmn", "${procType}_$defId"),
                            defId,
                            ws
                        )
                    }
                    )
            }
            ProcDefType.BPMN -> {
                (
                    { defId: String, ws: String ->
                        val testDef = ResourceUtils.getFile("classpath:test/bpmn/simple-vaild-process.bpmn.xml").readText()
                        helper.saveAndDeployBpmnFromString(
                            testDef.replace("simple-vaild-process", defId),
                            defId,
                            ws
                        )
                    }
                    )
            }
        }

        val baseQuery = RecordsQuery.create()
            .withSourceId(procType.latestDefsSrcId)
            .withQuery(Predicates.alwaysTrue())
            .withMaxItems(1000)
            .build()

        // Temp hack to avoid interference from leftover data from other tests.
        val definitionsBeforeTestStart = records.query(baseQuery, listOf("id"))
            .getRecords()
            .mapTo(HashSet()) { it["id"].asText() }

        deployDef("wo-ws", "")
        deployDef("ws-0", "ws-0")
        deployDef("ws-1", "ws-1")

        fun queryInWs(workspacesToQuery: List<String>, vararg expected: String) {
            val res = records.query(baseQuery.copy().withWorkspaces(workspacesToQuery).build(), listOf("id"))
            assertThat(
                res.getRecords().map { it["id"].asText() }
                    .filter { !definitionsBeforeTestStart.contains(it) }
            )
                .containsExactlyInAnyOrderElementsOf(expected.toList())
        }

        val dwoWsId = procType.getExpectedId("wo-ws", "")
        val dws0Id = procType.getExpectedId("ws-0", "ws-0")
        val dws1Id = procType.getExpectedId("ws-1", "ws-1")

        queryInWs(emptyList(), dwoWsId, dws0Id, dws1Id)
        queryInWs(listOf("ws-0"), dws0Id)
        queryInWs(listOf("ws-1"), dws1Id)
        queryInWs(listOf("ws-0", "ws-1"), dws0Id, dws1Id)
        queryInWs(listOf("", "ws-1"), dwoWsId, dws1Id)
        listOf("", "default", "admin\$workspace").forEach {
            queryInWs(listOf(it), dwoWsId)
        }

        fun runAsUser0(action: () -> Unit) = AuthContext.runAs("user0", listOf("GROUP_all"), action)

        runAsUser0 {
            queryInWs(emptyList(), dwoWsId)
            queryInWs(listOf("ws-0", "ws-1"))
            workspaceApi.setUserWorkspaces("user0", setOf("ws-1"))
            queryInWs(listOf("ws-0", "ws-1"), dws1Id)
            queryInWs(listOf("", "ws-1"), dwoWsId, dws1Id)
        }

        for (i in 0 until 10) {
            deployDef("def-id-$i", "ws-1")
        }
        val queryRes0 = records.query(
            baseQuery.copy()
                .withWorkspaces(listOf("ws-1"))
                .withMaxItems(3)
                .withSkipCount(4)
                .build()
        )

        assertThat(queryRes0.getRecords()).hasSize(3)
        assertThat(queryRes0.getTotalCount()).isEqualTo(11)

        val queryRes1 = records.query(
            baseQuery.copy()
                .withWorkspaces(listOf("ws-1"))
                .withMaxItems(3)
                .withSkipCount(5)
                .build()
        )

        assertThat(queryRes1.getRecords()).hasSize(3)
        assertThat(queryRes1.getTotalCount()).isEqualTo(11)

        assertThat(queryRes0.getRecords()[1]).isEqualTo(queryRes1.getRecords()[0])

        helper.cleanDefinitions()
        helper.cleanDeployments()
    }

    @AfterEach
    fun tearDown() {
        workspaceApi.cleanUp()
    }
}
