package ru.citeck.ecos.process.domain.bpmn.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.dto.DbTableRef
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.time.Instant

@Profile("!test")
@Configuration
class BpmnProcessElementsConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val recordsService: RecordsService
) {

    companion object {
        const val BPMN_ELEMENTS_SOURCE_ID = "bpmn-process-elements"
        const val BPMN_ELEMENTS_REPO_SOURCE_ID = "$BPMN_ELEMENTS_SOURCE_ID-repo"
    }

    @Bean
    fun bpmnElementsDao(): RecordsDao {
        return RecordsDaoProxy(BPMN_ELEMENTS_SOURCE_ID, BPMN_ELEMENTS_REPO_SOURCE_ID)
    }

    @Bean
    fun bpmnActivitiesRepoDao(eventsService: EventsService): RecordsDao {

        val accessPerms = object : DbRecordPerms {
            override fun getAuthoritiesWithReadPermission(): Set<String> {
                return setOf("EVERYONE")
            }
            override fun isCurrentUserHasWritePerms(): Boolean {
                return false
            }
        }
        val permsComponent = object : DbPermsComponent {
            override fun getRecordPerms(recordRef: RecordRef): DbRecordPerms {
                return accessPerms
            }
        }

        val typeRef = TypeUtils.getTypeRef("bpmn-process-element")
        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(DbRecordsDaoConfig.create {
                    withId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                    withTypeRef(typeRef)
                })
                .withDataService(DbDataServiceConfig.create {
                    withAuthEnabled(false)
                    withTableRef(DbTableRef("ecos_data", "bpmn_process_elements"))
                    withTransactional(false)
                    withStoreTableMeta(true)
                })
                .build()
        ).withPermsComponent(permsComponent).build()

        eventsService.addListener<TaskEvent> {
            withEventType("bpmn-user-task-create")
            withDataClass(TaskEvent::class.java)
            withAction {
                createTaskElement(it, false)
            }
        }

        eventsService.addListener<TaskEvent> {
            withEventType("bpmn-user-task-complete")
            withDataClass(TaskEvent::class.java)
            withAction {
                val existingElement = recordsService.queryOne(RecordsQuery.create {
                    withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                    withQuery(Predicates.and(
                        Predicates.eq("engine", it.engine),
                        Predicates.eq("elementId", it.taskId)
                    ))
                })
                if (existingElement == null) {
                    createTaskElement(it, true)
                } else {
                    val data = ObjectData.create()
                    data.set("completed", it.time)
                    data.set("outcome", it.outcome)
                    data.set("outcomeName", it.outcomeName)
                    data.set("comment", it.comment)
                    recordsService.mutate(existingElement, data)
                }
            }
        }

        eventsService.addListener<FlowElementTakeEvent> {
            withEventType("bpmn-flow-element-take")
            withDataClass(FlowElementTakeEvent::class.java)
            withAction {
                createFlowElement(it)
            }
        }

        return recordsDao
    }

    private fun createTaskElement(event: TaskEvent, completedEvent: Boolean) {
        val data = ObjectData.create(event)
        if (completedEvent) {
            data.set("started", event.time)
            data.set("completed", event.time)
        } else {
            data.remove("outcome")
            data.remove("outcomeName")
            data.remove("comment")
        }
        data.set("elementId", event.taskId)
        data.set("elementType", "UserTask")
        recordsService.create(BPMN_ELEMENTS_REPO_SOURCE_ID, data)
    }

    private fun createFlowElement(event: FlowElementTakeEvent) {
        val data = ObjectData.create(event)
        data.set("created", event.time)
        data.set("completed", event.time)
        recordsService.create(BPMN_ELEMENTS_REPO_SOURCE_ID, data)
    }

    private class FlowElementTakeEvent(
        var engine: String? = null,
        var procDefId: String? = null,
        var elementType: String? = null,
        var elementDefId: String? = null,
        var procDefVersion: String? = null,
        var procInstanceId: String? = null,
        var executionId: String? = null,
        @AttName("\$event.time")
        var time: String? = null,
        var document: RecordRef? = null,
        @AttName("document._type?id")
        var documentTypeRef: RecordRef? = null,
    )

    private class TaskEvent(
        var taskId: String? = null,
        var engine: String? = null,
        var assignee: String? = null,
        var procDefId: String? = null,
        var procDefVersion: Int? = null,
        var procInstanceId: String? = null,
        var elementDefId: String? = null,
        var created: String? = null,
        var dueDate: Instant? = null,
        var description: String? = null,
        var priority: Int? = null,
        var executionId: String? = null,
        var name: MLText? = null,
        var comment: String? = null,
        var outcome: String? = null,
        var outcomeName: MLText? = null,
        var document: RecordRef? = null,
        @AttName("document._type?id")
        var documentTypeRef: RecordRef? = null,
        @AttName("\$event.time")
        var time: String
    )
}
