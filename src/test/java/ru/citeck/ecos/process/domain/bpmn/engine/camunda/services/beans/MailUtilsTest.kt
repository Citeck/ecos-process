package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

class MailUtilsTest {

    private val authorityService: EcosAuthoritiesApi = mock()
    private val recordsService: RecordsService = mock()

    private val mailUtils = MailUtils(authorityService, recordsService)

    /**
     * A recipient that looks like a ref but is not a canonical emodel authority ref resolves to two
     * candidate refs: the literal one and the canonical one returned by the authority lookup. Only
     * the canonical ref answers `email`, so querying just the literal ref would silently drop the
     * recipient from the mail.
     */
    @Test
    fun `non canonical authority ref keeps the recipient via its canonical ref`() {
        val input = "alfresco/people@admin"
        val literalRef = EntityRef.valueOf(input)
        val canonicalRef = EntityRef.valueOf("emodel/person@admin")

        whenever(authorityService.getAuthorityRefs(listOf(input))).thenReturn(listOf(canonicalRef))
        whenever(recordsService.getAtts(listOf(literalRef, canonicalRef), UserInfo::class.java))
            .thenReturn(
                listOf(
                    // the literal ref resolves to nothing
                    UserInfo(email = null, disabled = null),
                    UserInfo(email = "admin@mail.com", disabled = false)
                )
            )

        assertThat(mailUtils.getEmailsByAuthority(listOf(input)))
            .containsExactly(org.assertj.core.api.Assertions.entry(input, listOf("admin@mail.com")))
        assertThat(mailUtils.getEmails(listOf(input))).containsExactly("admin@mail.com")
    }

    /**
     * A canonical ref resolves to a single candidate ref (literal == resolved), so nothing is
     * queried twice and a disabled user is still dropped.
     */
    @Test
    fun `disabled user is dropped`() {
        val input = "emodel/person@ivan"
        val ref = EntityRef.valueOf(input)

        whenever(authorityService.getAuthorityRefs(listOf(input))).thenReturn(listOf(ref))
        whenever(recordsService.getAtts(listOf(ref), UserInfo::class.java))
            .thenReturn(listOf(UserInfo(email = "ivan@mail.com", disabled = true)))

        assertThat(mailUtils.getEmailsByAuthority(listOf(input))[input]).isEmpty()
        assertThat(mailUtils.getEmails(listOf(input))).isEmpty()
    }
}
