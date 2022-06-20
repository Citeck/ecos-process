package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.proctask.service.aggregate.ProcTaskAggregator
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

@Component
class AggregationProcTaskRecords(
    private val procTaskAggregator: ProcTaskAggregator
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "aggregation-proc-task"

        val ALF_TO_ERPOC_TASK_ATTS = mapOf(
            "wfm:document" to "documentRef",
            "bpm:dueDate" to "dueDate",
            "bpm:priority" to "priority",
            "bpm:startDate" to "created",
            "cm:title" to ".disp"
        )

        val EPROC_TO_ALF_TASK_ATTS = ALF_TO_ERPOC_TASK_ATTS.entries.associateBy({ it.value }) { it.key }
    }

    override fun getId(): String {
        return ID
    }

    override fun getRecordsAtts(recordsId: List<String>): List<*>? {
        return recordsId.map { AggregationRecord(it) }
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        return procTaskAggregator.queryTasks(recsQuery)
    }

    inner class AggregationRecord(
        val id: String,
        var atts: RecordAtts = RecordAtts()
    ) {

        private val isAlfTask = fun(): Boolean {
            return id.startsWith("workspace")
        }

        init {
            val attsMap = AttContext.getInnerAttsMap()
                .map {
                    val mapping = if (isAlfTask()) EPROC_TO_ALF_TASK_ATTS else ALF_TO_ERPOC_TASK_ATTS
                    if (mapping.containsKey(it.key)) {
                        val fixedAtt = mapping[it.key]!!
                        return@map it.key.replaceFirst(it.key, fixedAtt) to it.value.replaceFirst(it.key, fixedAtt)
                    }
                    it.key to it.value
                }
                .toMap()

            val fullOriginalRef = let {
                if (isAlfTask()) {
                    return@let RecordRef.create("alfresco", "", id)
                }
                return@let RecordRef.create("eproc", ProcTaskRecords.ID, id)
            }

            atts = recordsService.getAtts(fullOriginalRef, attsMap)
        }

        fun getAtt(name: String): Any? {
            val mapping = if (isAlfTask()) EPROC_TO_ALF_TASK_ATTS else ALF_TO_ERPOC_TASK_ATTS
            if (mapping.containsKey(name)) {
                val fixedAttName = mapping[name]
                return atts.getAtt(fixedAttName)
            }

            return atts.getAtt(name)
        }

    }
}
