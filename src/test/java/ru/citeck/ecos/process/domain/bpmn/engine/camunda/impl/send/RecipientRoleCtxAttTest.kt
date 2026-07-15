package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import java.util.Locale

/**
 * Verifies records-lib ctx-attribute resolution for the recipient role keys placed by
 * [SendNotificationDelegate] into `Notification.additionalMeta`: given those keys as ctx atts, a
 * template model declaring `"recipientRoleId": "$recipientRoleId"` and
 * `"recipientRoleName": "$recipientRoleName"` resolves them correctly.
 *
 * This does NOT exercise `NotificationServiceImpl` - it only locks the records-lib contract that
 * `NotificationServiceImpl.fillModel` relies on when it passes `additionalMeta` to
 * `RequestContext.withCtxAtts`.
 */
class RecipientRoleCtxAttTest {

    @Test
    fun `recipient role keys are resolvable as ctx attributes`() {
        val factory = RecordsServiceFactory()
        val recordsService = factory.recordsService

        val additionalMeta = mapOf(
            "recipientRoleId" to "initiator",
            "recipientRoleName" to MLText(
                Locale.ENGLISH to "Initiator",
                Locale.forLanguageTag("ru") to "Initiator-ru"
            )
        )
        val record = mapOf("someField" to "x")

        RequestContext.doWithCtx(factory, { it.withCtxAtts(additionalMeta) }) {
            val res = recordsService.getAtts(
                record,
                listOf("\$recipientRoleId", "\$recipientRoleName", "\$recipientRoleName?json")
            )

            // role id is a plain string -> usable directly in template conditions
            assertThat(res.getAtt("\$recipientRoleId").asText()).isEqualTo("initiator")
            // role name resolves to a localized display string by default
            assertThat(res.getAtt("\$recipientRoleName").asText()).isEqualTo("Initiator")
            // ...and the full MLText is still reachable via ?json
            assertThat(res.getAtt("\$recipientRoleName?json").get("ru").asText()).isEqualTo("Initiator-ru")
        }
    }
}
