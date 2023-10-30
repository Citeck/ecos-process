package ru.citeck.ecos.process.common.section.records

import ru.citeck.ecos.data.sql.records.DbRecordsControlAtts
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.model.lib.type.constants.TypeConstants
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Listener to update permissions for child sections.
 * This solution will be removed once the updating of children's permissions is performed in a more common way.
 */
class SectionChildrenPermsUpdater(
    eventsService: EventsService,
    private val recordsService: RecordsService,
    private val sectionType: SectionType
) {

    init {
        val filter = Predicates.eq(
            "record._type.${TypeConstants.ATT_IS_SUBTYPE_OF}.${sectionType.typeId}?bool!",
            true
        )
        eventsService.addListener<PermissionSettingsChangedEvent> {
            withEventType(PermissionSettingsChangedEvent.TYPE)
            withDataClass(PermissionSettingsChangedEvent::class.java)
            withFilter(filter)
            withAction { updatePermsForChildren(it.record, 0) }
        }
    }

    private fun updatePermsForChildren(ref: EntityRef, nestingIdx: Int) {

        var filter: Predicate = Predicates.eq("parentRef", ref)
        if (ref.getLocalId() == "ROOT") {
            filter = Predicates.or(
                filter,
                Predicates.and(
                    Predicates.empty("parentRef"),
                    Predicates.notEq("id", "ROOT")
                )
            )
        }

        val childrenRefs = recordsService.query(
            RecordsQuery.create {
                withSourceId(sectionType.sourceId)
                withQuery(filter)
                withMaxItems(1000)
            }
        ).getRecords()

        childrenRefs.forEach {
            recordsService.mutateAtt(it, DbRecordsControlAtts.UPDATE_PERMISSIONS, true)
            if (nestingIdx < 4) {
                updatePermsForChildren(it, nestingIdx + 1)
            }
        }
    }

    private class PermissionSettingsChangedEvent(
        @AttName("record?id!")
        val record: EntityRef
    ) {
        companion object {
            const val TYPE = "permission-settings-changed"
        }
    }
}
