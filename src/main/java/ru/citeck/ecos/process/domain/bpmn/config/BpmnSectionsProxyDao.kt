package ru.citeck.ecos.process.domain.bpmn.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaUtils
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

@Component
class BpmnSectionsProxyDao(
    private val ecosWebAppContext: EcosWebAppContext,
) : RecordsDaoProxy(
    id = SOURCE_ID,
    targetId = TARGET_SOURCE_ID
) {

    companion object {
        const val SOURCE_ID = "bpmn-section"
        const val TARGET_SOURCE_ID = BpmnSectionConfig.BPMN_SECTION_REPO_SOURCE_ID
    }

    private val sourceIdMapping = mapOf(TARGET_SOURCE_ID to SOURCE_ID)
    private val querySectionsFromAlf = RecordsQuery.create {
        withSourceId("alfresco/")
        withLanguage("children")
        withQuery(
            mapOf(
                "parent" to "workspace://SpacesStore/ecos-bpm-category-root",
                "assocName" to "cm:subcategories"
            )
        )
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
        val targetQuery = recsQuery.copy().withSourceId(getTargetId()).build()

        val contextAtts = getContextAtts()
        return if (contextAtts.isEmpty()) {
            val result = withSourceIdMapping {
                recordsService.query(targetQuery)
            }

            if (isAlfrescoAvailable()) {
                val queryResFromAlfresco = queryBpmnSectionsFromAlfresco()
                replaceRecordsIdForAlfRecords(queryResFromAlfresco)
                result.merge(queryResFromAlfresco)
            }

            result
        } else {
            val queryRes = withSourceIdMapping {
                recordsService.query(targetQuery, contextAtts)
            }
            val queryResWithAtts = RecsQueryRes<RecordAtts>()
            queryResWithAtts.setHasMore(queryRes.getHasMore())
            queryResWithAtts.setTotalCount(queryRes.getTotalCount())
            queryResWithAtts.setRecords(queryRes.getRecords())

            if (isAlfrescoAvailable()) {
                val queryResWithAttsFromAlfresco = queryBpmnSectionsFromAlfresco(contextAtts)
                replaceRecordsAttsRefIdForAlfRecords(queryResWithAttsFromAlfresco)
                queryResWithAtts.merge(queryResWithAttsFromAlfresco)
            }

            queryResWithAtts
        }
    }

    override fun getRecordsAtts(recordsId: List<String>): List<*> {
        val eprocRecords = mutableListOf<RecordRef>()
        val alfRecords = mutableListOf<RecordRef>()
        splitRecordRefs(recordsId, eprocRecords, alfRecords)

        val contextAtts = getContextAtts()
        val attsFromTarget = withSourceIdMapping {
            recordsService.getAtts(eprocRecords, contextAtts)
        }
        val attsFromAlf = recordsService.getAtts(alfRecords, contextAtts)

        return attsFromTarget + attsFromAlf
    }

    private fun splitRecordRefs(
        recordsId: List<String>,
        eprocRecords: MutableList<RecordRef>,
        alfRecords: MutableList<RecordRef>
    ) {
        recordsId.forEach {
            if (it.startsWith("alfresco$")) {
                val alfRecordId = it.substring(9)
                alfRecords.add(toTargetRef(alfRecordId, "alfresco/"))
            } else {
                eprocRecords.add(toTargetRef(it, getTargetId()))
            }
        }
    }

    override fun delete(recordsId: List<String>): List<DelStatus> {
        val eprocRecords = mutableListOf<RecordRef>()
        val alfRecords = mutableListOf<RecordRef>()
        splitRecordRefs(recordsId, eprocRecords, alfRecords)

        withSourceIdMapping {
            recordsService.delete(eprocRecords)
        }
        recordsService.delete(alfRecords)
        return listOf(DelStatus.OK)
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {
        throw NotImplementedError()
    }

    private fun getContextAtts(): Map<String, String> {
        val schemaAtts = AttSchemaUtils.simplifySchema(AttContext.getCurrentSchemaAtt().inner)
        val writer = serviceFactory.attSchemaWriter
        val result = LinkedHashMap<String, String>()

        schemaAtts.forEach { att ->
            result[att.getAliasForValue()] = writer.write(att)
        }

        return result
    }

    private inline fun <T> withSourceIdMapping(crossinline action: () -> T): T {
        return RequestContext.doWithCtx({
            it.withSourceIdMapping(sourceIdMapping)
        }) {
            action.invoke()
        }
    }

    private fun isAlfrescoAvailable(): Boolean {
        return ecosWebAppContext.getWebAppsApi().isAppAvailable("alfresco")
    }

    private fun queryBpmnSectionsFromAlfresco(): RecsQueryRes<RecordRef> {
        return recordsService.query(querySectionsFromAlf)
    }

    private fun queryBpmnSectionsFromAlfresco(atts: Map<String, *>): RecsQueryRes<RecordAtts> {
        return recordsService.query(querySectionsFromAlf, atts)
    }

    private fun replaceRecordsIdForAlfRecords(recsQueryRes: RecsQueryRes<RecordRef>) {
        recsQueryRes.setRecords(
            recsQueryRes.getRecords().map {
                RecordRef.create(getId(), "alfresco$" + it.id)
            }
        )
    }

    private fun replaceRecordsAttsRefIdForAlfRecords(recsQueryRes: RecsQueryRes<RecordAtts>) {
        recsQueryRes.getRecords().forEach {
            it.setId(RecordRef.create(getId(), "alfresco$" + it.getId().id))
        }
    }

    private fun toTargetRef(recordId: String, sourceId: String): RecordRef {
        return RecordRef.valueOf("$sourceId@$recordId")
    }
}
