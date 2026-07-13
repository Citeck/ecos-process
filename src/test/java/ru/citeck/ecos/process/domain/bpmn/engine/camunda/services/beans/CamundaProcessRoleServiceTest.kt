package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.role.dto.RoleDef
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef

class CamundaProcessRoleServiceTest {

    private val roleService: RoleService = mock()
    private val mailUtils: MailUtils = mock()
    private val recordsService: RecordsService = mock()

    private val service = CamundaRoleService(roleService, mailUtils, recordsService)

    private val doc = EntityRef.valueOf("emodel/doc@1")

    @Test
    fun `getEmailsByRole groups emails per role using a single batched assignees and emails lookup`() {
        val roles = listOf("author", "accountant")

        whenever(roleService.getAssignees(doc, roles)).thenReturn(
            mapOf(
                "author" to listOf("author-user"),
                "accountant" to listOf("acc-user")
            )
        )
        whenever(mailUtils.getEmailsByAuthority(setOf("author-user", "acc-user"))).thenReturn(
            mapOf(
                "author-user" to listOf("author@mail.com"),
                "acc-user" to listOf("acc@mail.com")
            )
        )

        val result = service.getEmailsByRole(doc, roles)

        assertThat(result).containsOnlyKeys("author", "accountant")
        assertThat(result["author"]).containsExactly("author@mail.com")
        assertThat(result["accountant"]).containsExactly("acc@mail.com")

        // batched: exactly one call to the collection overload, none to a per-role lookup
        verify(roleService).getAssignees(doc, roles)
        verify(mailUtils).getEmailsByAuthority(setOf("author-user", "acc-user"))
    }

    @Test
    fun `getEmailsByRole supports a role assigned to several authorities each resolving to emails`() {
        val roles = listOf("approvers")

        whenever(roleService.getAssignees(doc, roles)).thenReturn(
            mapOf("approvers" to listOf("user1", "user2"))
        )
        whenever(mailUtils.getEmailsByAuthority(setOf("user1", "user2"))).thenReturn(
            mapOf(
                "user1" to listOf("user1@mail.com"),
                "user2" to listOf("user2@mail.com")
            )
        )

        val result = service.getEmailsByRole(doc, roles)

        assertThat(result).containsOnlyKeys("approvers")
        assertThat(result["approvers"]).containsExactlyInAnyOrder("user1@mail.com", "user2@mail.com")
    }

    @Test
    fun `getEmailsByRole returns an empty list for a role with no assignees`() {
        val roles = listOf("author", "emptyRole")

        whenever(roleService.getAssignees(doc, roles)).thenReturn(
            mapOf(
                "author" to listOf("author-user"),
                "emptyRole" to emptyList()
            )
        )
        whenever(mailUtils.getEmailsByAuthority(setOf("author-user"))).thenReturn(
            mapOf("author-user" to listOf("author@mail.com"))
        )

        val result = service.getEmailsByRole(doc, roles)

        assertThat(result).containsOnlyKeys("author", "emptyRole")
        assertThat(result["author"]).containsExactly("author@mail.com")
        assertThat(result["emptyRole"]).isEmpty()
    }

    @Test
    fun `getEmailsByRole returns an empty list for a role whose assignee resolves to no email`() {
        val roles = listOf("author", "noEmailRole")

        whenever(roleService.getAssignees(doc, roles)).thenReturn(
            mapOf(
                "author" to listOf("author-user"),
                "noEmailRole" to listOf("disabled-user")
            )
        )
        whenever(mailUtils.getEmailsByAuthority(setOf("author-user", "disabled-user"))).thenReturn(
            mapOf(
                "author-user" to listOf("author@mail.com"),
                "disabled-user" to emptyList()
            )
        )

        val result = service.getEmailsByRole(doc, roles)

        assertThat(result).containsOnlyKeys("author", "noEmailRole")
        assertThat(result["author"]).containsExactly("author@mail.com")
        assertThat(result["noEmailRole"]).isEmpty()
    }

    @Test
    fun `getRoleName resolves role display name via type ref`() {
        whenever(recordsService.getAtt(eq(doc), eq("_type?id")))
            .thenReturn(DataValue.createStr("emodel/type@fin-request"))
        whenever(roleService.getRoleDef(EntityRef.valueOf("emodel/type@fin-request"), "initiator"))
            .thenReturn(
                RoleDef.create {
                    withId("initiator")
                    withName(MLText("Initiator"))
                }
            )

        val name = service.getRoleName(doc, "initiator")

        assertThat(MLText.getClosestValue(name, java.util.Locale.ENGLISH)).isEqualTo("Initiator")
    }

    @Test
    fun `getRoleName returns empty when type is blank`() {
        whenever(recordsService.getAtt(eq(doc), eq("_type?id"))).thenReturn(DataValue.createStr(""))

        val name = service.getRoleName(doc, "initiator")

        assertThat(name).isEqualTo(MLText.EMPTY)
    }

    @Test
    fun `getRoleNames resolves several roles fetching the document type only once`() {
        whenever(recordsService.getAtt(eq(doc), eq("_type?id")))
            .thenReturn(DataValue.createStr("emodel/type@fin-request"))
        val typeRef = EntityRef.valueOf("emodel/type@fin-request")
        whenever(roleService.getRoleDef(typeRef, "initiator")).thenReturn(
            RoleDef.create {
                withId("initiator")
                withName(MLText("Initiator"))
            }
        )
        whenever(roleService.getRoleDef(typeRef, "approver")).thenReturn(
            RoleDef.create {
                withId("approver")
                withName(MLText("Approver"))
            }
        )

        val names = service.getRoleNames(doc, listOf("initiator", "approver"))

        assertThat(names).containsOnlyKeys("initiator", "approver")
        assertThat(MLText.getClosestValue(names.getValue("initiator"), java.util.Locale.ENGLISH))
            .isEqualTo("Initiator")
        assertThat(MLText.getClosestValue(names.getValue("approver"), java.util.Locale.ENGLISH))
            .isEqualTo("Approver")
        // the document type is resolved once for the whole batch, not per role
        verify(recordsService, times(1)).getAtt(eq(doc), eq("_type?id"))
    }
}
