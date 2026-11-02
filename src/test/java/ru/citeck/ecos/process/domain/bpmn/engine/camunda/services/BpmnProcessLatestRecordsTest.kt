package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import org.camunda.bpm.engine.RepositoryService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

class BpmnProcessLatestRecordsTest {

    private val workspaces = mock(WorkspaceService::class.java)
    private val definitions = mock(ProcDefService::class.java)
    private val extension = mock(CamundaMyBatisExtension::class.java)

    private val dao = BpmnProcessLatestRecords(
        mock(RepositoryService::class.java),
        definitions,
        extension,
        workspaces
    )

    @Test
    fun resolvesOnlyFirstFiftyEnabledNameOrIdentifierMatchesInRequestedWorkspaces() {

        `when`(workspaces.getAvailableWorkspacesToQuery(any(), anyList())).thenReturn(setOf("sales"))
        `when`(workspaces.getWorkspaceSystemId("sales")).thenReturn("sales-sys")
        `when`(workspaces.isWorkspaceWithGlobalEntities("")).thenReturn(true)

        val global = ProcDefDto().apply {
            id = "global-process"
            workspace = ""
        }
        val scoped = ProcDefDto().apply {
            id = "scoped-process"
            workspace = "sales"
        }

        var lookups = 0
        doAnswer { invocation ->
            lookups++
            assertEquals(listOf("sales"), invocation.getArgument<List<String>>(0))
            assertEquals(50, invocation.getArgument<Int>(2))
            assertEquals(0, invocation.getArgument<Int>(3))

            val predicate = invocation.getArgument<AndPredicate>(1).getPredicates()
            assertTrue(predicate.contains(Predicates.eq("enabled", true)))
            assertTrue(predicate.contains(Predicates.eq("procType", "bpmn")))

            val nameOrIdentifier = Predicates.or(
                Predicates.contains("name", "Поруч"),
                Predicates.contains("moduleId", "Поруч")
            )
            assertTrue(predicate.contains(nameOrIdentifier))

            listOf(global, scoped)
        }.`when`(definitions).findAll(anyList(), any(), anyInt(), anyInt())

        var receivedFilter: BpmnProcessLatestFilter? = null
        doAnswer { invocation ->
            assertEquals(listOf("sales-sys"), invocation.getArgument<List<String>>(0))
            receivedFilter = invocation.getArgument(1)
            0L
        }.`when`(extension).getCountOfLatestBpmnDefsByWsSysId(anyList(), any())

        val result = dao.queryRecords(
            RecordsQuery.create()
                .withWorkspaces(listOf("sales"))
                .withQuery(Predicates.contains("definition", "Поруч"))
                .build()
        )

        assertEquals(1, lookups)
        assertEquals(listOf("global-process.bpmn", "sales-sys..scoped-process.bpmn"), receivedFilter!!.params)
        assertTrue(result.getRecords().isEmpty())
    }

    @Test
    fun deniedWorkspaceScopeDoesNotQueryDefinitionsOrEngine() {

        `when`(workspaces.getAvailableWorkspacesToQuery(any(), anyList())).thenReturn(null)

        val query = RecordsQuery.create()
            .withQuery(Predicates.contains("definition", "assign"))
            .build()
        val result = dao.queryRecords(query)

        assertTrue(result.getRecords().isEmpty())
        verifyNoInteractions(definitions, extension)
    }

    @Test
    fun systemAllWorkspaceScopeReachesEngineWithoutWorkspaceRestriction() {

        `when`(workspaces.getAvailableWorkspacesToQuery(any(), anyList())).thenReturn(emptySet())
        `when`(extension.getCountOfLatestBpmnDefsByWsSysId(anyList(), any())).thenReturn(0L)

        val result = dao.queryRecords(RecordsQuery.create().withQuery(Predicates.alwaysTrue()).build())

        verify(extension).getCountOfLatestBpmnDefsByWsSysId(eq(emptyList()), any())
        verifyNoInteractions(definitions)
        assertTrue(result.getRecords().isEmpty())
    }

    @Test
    fun identifierFilterDoesNotLoadDefinitionNames() {

        `when`(workspaces.getAvailableWorkspacesToQuery(any(), anyList())).thenReturn(setOf(""))
        `when`(workspaces.getWorkspaceSystemId("")).thenReturn("")

        doAnswer { invocation ->
            val filter = invocation.getArgument<BpmnProcessLatestFilter>(1)
            assertEquals(listOf("%assign%"), filter.params)
            0L
        }.`when`(extension).getCountOfLatestBpmnDefsByWsSysId(anyList(), any())

        val query = RecordsQuery.create()
            .withQuery(Predicates.contains("id", "assign"))
            .build()

        dao.queryRecords(query)
        verifyNoInteractions(definitions)
    }
}
