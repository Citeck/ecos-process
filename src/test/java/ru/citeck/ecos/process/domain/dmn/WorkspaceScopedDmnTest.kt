package ru.citeck.ecos.process.domain.dmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
class WorkspaceScopedDmnTest {

    @Autowired
    private lateinit var helper: BpmnProcHelper
    @Autowired
    private lateinit var records: RecordsService
    @Autowired
    private lateinit var workspaceApi: CustomWorkspaceApi

    @Test
    fun dmnDecisionLatestRecordsTest() {

        val testDef = ResourceUtils.getFile("classpath:test/dmn/simple-dmn-test.dmn.xml").readText()
        fun deployDef(defId: String, ws: String) {
            helper.saveAndDeployDmnFromString(
                testDef.replace("ecos:defId=\"simple-dmn-test\"", "ecos:defId=\"$defId\"")
                    .replace("Decision_simple-dmn", "Decision_$defId"),
                defId,
                ws
            )
        }

        deployDef("dmn-wo-ws", "")
        deployDef("dmn-ws-0", "ws-0")
        deployDef("dmn-ws-1", "ws-1")

        val baseQuery = RecordsQuery.create()
            .withSourceId("dmn-decision-latest")
            .withQuery(Predicates.alwaysTrue())
            .withMaxItems(100)
            .build()

        fun queryInWs(workspacesToQuery: List<String>, vararg expected: String) {
            val res = records.query(baseQuery.copy().withWorkspaces(workspacesToQuery).build(), listOf("id"))
            assertThat(res.getRecords().map { it["id"].asText() }).containsExactlyInAnyOrderElementsOf(expected.toList())
        }

        val dwoWsId = "Decision_dmn-wo-ws"
        val dws0Id = "Decision_dmn-ws-0"
        val dws1Id = "Decision_dmn-ws-1"

        queryInWs(emptyList(), dwoWsId, dws0Id, dws1Id)
        queryInWs(listOf("ws-0"), dws0Id)
        queryInWs(listOf("ws-1"), dws1Id)
        queryInWs(listOf("ws-0","ws-1"), dws0Id, dws1Id)
        queryInWs(listOf("","ws-1"), dwoWsId, dws1Id)
        listOf("", "default", "admin\$workspace").forEach {
            queryInWs(listOf(it), dwoWsId)
        }

        fun runAsUser0(action: () -> Unit) = AuthContext.runAs("user0", listOf("GROUP_all"), action)

        runAsUser0 {
            queryInWs(emptyList(), dwoWsId)
            queryInWs(listOf("ws-0","ws-1"))
            workspaceApi.setUserWorkspaces("user0", setOf("ws-1"))
            queryInWs(listOf("ws-0","ws-1"), dws1Id)
            queryInWs(listOf("","ws-1"), dwoWsId, dws1Id)
        }

        for (i in 0 until 10) {
            deployDef("def-id-$i", "ws-1")
        }
        val queryRes0 = records.query(baseQuery.copy()
            .withWorkspaces(listOf("ws-1"))
            .withMaxItems(3)
            .withSkipCount(4)
            .build())

        assertThat(queryRes0.getRecords()).hasSize(3)
        assertThat(queryRes0.getTotalCount()).isEqualTo(11)

        val queryRes1 = records.query(baseQuery.copy()
            .withWorkspaces(listOf("ws-1"))
            .withMaxItems(3)
            .withSkipCount(5)
            .build())

        assertThat(queryRes1.getRecords()).hasSize(3)
        assertThat(queryRes1.getTotalCount()).isEqualTo(11)

        assertThat(queryRes0.getRecords()[1]).isEqualTo(queryRes1.getRecords()[0])
    }

    @AfterEach
    fun tearDown() {
        workspaceApi.cleanUp()
    }
}
