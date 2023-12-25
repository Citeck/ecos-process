package ru.citeck.ecos.process.domain.bpmnla.api.records

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.license.EcosLicense
import ru.citeck.ecos.process.domain.bpmnla.dto.LazyApprovalResponseToNotificationDto
import ru.citeck.ecos.process.domain.bpmnla.services.BpmnLazyApprovalService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.mutate.ValueMutateDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

@Component
class LazyApprovalRecordsDao(
    private val recordsService: RecordsService,
    private val bpmnLazyApprovalService: BpmnLazyApprovalService
) : ValueMutateDao<DataValue> {

    private val isEnt = EcosLicense.getForWebApp { it.isEnterprise() }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return "lazy-approval"
    }

    override fun mutate(value: DataValue): Any? {

//        if (!isEnt()) {
//            log.info("The lazy approval functionality is only available for the enterprise version.")
//            return null
//        }

        val response = value.getAs(LazyApprovalResponseToNotificationDto::class.java)

        if (response == null) {
            log.debug { "Lazy approval response to notification is null!" }
            return null
        }

        val userRefs = recordsService.query(
            RecordsQuery.create {
                withSourceId("emodel/person")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(Predicates.eq("email", response.email))
            })

        if (userRefs.getRecords().isEmpty()) {
            log.debug { "User with email = ${response.email} not found in the system!" }
            return null
        } else if (userRefs.getRecords().size > 1) {
            log.debug { "There are more than 2 users with email = ${response.email} in the system!" }
        }

        val userId = recordsService.getAtt(userRefs.getRecords().first(), RecordConstants.ATT_LOCAL_ID).asText()

        bpmnLazyApprovalService.approveTask(
            taskId = response.taskId,
            taskOutcome = response.outcome,
            userId = userId,
            token = response.token,
            comment = response.comment)

        return null
    }
}

