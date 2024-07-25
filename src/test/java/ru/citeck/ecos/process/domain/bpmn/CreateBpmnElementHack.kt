package ru.citeck.ecos.process.domain.bpmn

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao.Companion.BPMN_ELEMENTS_SOURCE_ID
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import java.time.Instant
import javax.annotation.PostConstruct

/**
 * We need to initialize process of creating DB table for bpmn elements before running tests, because async
 * processing of creating bpmn elements can lead to error of 'table not found'.
 * We can remove this hack after implement retrying transaction in https://jira.citeck.ru/browse/ECOSENT-3064
 */
@Component
class CreateBpmnElementHack(
    private val recordsService: RecordsService,
    private val ecosWebAppApi: EcosWebAppApi
) {

    @PostConstruct
    fun init() {
        ecosWebAppApi.doWhenAppReady {
            TxnContext.doInTxn {
                val data = ObjectData.create()
                data["id"] = "hack-id"
                data["procInstanceId"] = "hack-proc-instance-id"
                data["elementDefId"] = "hack-element-def-id"
                data["created"] = Instant.now()

                val createdElement = recordsService.create(BPMN_ELEMENTS_SOURCE_ID, data)
                recordsService.delete(createdElement)
            }
        }
    }
}
