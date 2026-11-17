package ru.citeck.ecos.process.domain.proctask.service

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.sql.ResultSet
import java.time.Instant

/** Scalar JDBC only. Never route ID/variable projections through an engine entity mapper. */
internal object HistoricTaskSql {
    fun <T> query(
        configuration: ProcessEngineConfigurationImpl,
        sql: String,
        params: Map<String, Any?>,
        row: (ResultSet) -> T
    ): List<T> {
        val values = mutableListOf<Any?>()
        val jdbcSql = sql.replace(Regex("#\\{(\\w+)}")) {
            val name = it.groupValues[1]
            require(params.containsKey(name)) { "Missing historic query parameter: $name" }
            values.add(params[name])
            "?"
        }
        val dataSource = configuration.dataSource
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            connection.prepareStatement(jdbcSql).use { statement ->
                values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                statement.executeQuery().use { result ->
                    return buildList {
                        while (result.next()) add(row(result))
                    }
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    /**
     * Spin JSON object serializer writes UTF-8 JSON into BYTEARRAY_ID_ and the canonical
     * Java type into TEXT2_. CASE must depend on the variable row: PostgreSQL must not
     * push decoding down to a scan of unrelated deployment/Java-serialized byte arrays.
     */
    fun jsonListExpression(prefix: String): String = "(SELECT CASE WHEN v.VAR_TYPE_ = 'spin://application/json' AND " +
        "v.TEXT2_ LIKE 'java.util.%List%' THEN " +
        "CASE WHEN octet_length(b.BYTES_) > 0 THEN " +
        "CASE WHEN get_byte(b.BYTES_, 0) = 91 THEN convert_from(b.BYTES_, 'UTF8') END END END " +
        "FROM ${prefix}ACT_GE_BYTEARRAY b WHERE b.ID_ = v.BYTEARRAY_ID_)"
}

/** ECOS-only historical loading, independent of Camunda authentication, authorization and tenant checks. */
@Component
class HistoricTaskLoader(
    private val configuration: ProcessEngineConfigurationImpl,
    private val historyService: HistoryService,
    private val syncService: ProcTaskAttsSyncService
) {
    fun tasks(ids: List<String>): List<HistoricTaskInstance> {
        if (ids.isEmpty() || configuration.historyLevel.id == 0) return emptyList()
        var query = historyService.createNativeHistoricTaskInstanceQuery()
            .sql("SELECT t.* FROM ${configuration.databaseTablePrefix}ACT_HI_TASKINST t WHERE t.ID_ IN (${ids.indices.joinToString { "#{p$it}" }})")
        ids.forEachIndexed { index, id -> query = query.parameter("p$index", id) }
        return query.list()
    }

    fun variables(task: HistoricTaskInstance): Pair<Map<String, Any?>, Map<String, Any?>> {
        if (configuration.historyLevel.id == 0) return emptyMap<String, Any?>() to emptyMap()
        val params = mapOf("task" to task.id, "process" to task.processInstanceId)
        val rows = HistoricTaskSql.query(
            configuration,
            "SELECT v.*, CASE WHEN v.VAR_TYPE_ = 'spin://application/json' THEN b.BYTES_ END AS SAVED_BYTES_ " +
                "FROM ${configuration.databaseTablePrefix}ACT_HI_VARINST v " +
                "LEFT JOIN ${configuration.databaseTablePrefix}ACT_GE_BYTEARRAY b ON b.ID_ = v.BYTEARRAY_ID_ " +
                "WHERE (v.STATE_ IS NULL OR v.STATE_ <> 'DELETED') AND v.PROC_INST_ID_ = #{process} " +
                "AND ((v.TASK_ID_ = #{task} AND (v.NAME_ IN ('nameMl','taskComment','lastComment','completedBy') " +
                "OR LEFT(v.NAME_, 5) = '_doc_')) OR (v.TASK_ID_ IS NULL AND v.EXECUTION_ID_ = #{process} " +
                "AND v.NAME_ IN ('documentRef','documentType')))",
            params
        ) { result ->
            val name = result.getString("NAME_")
            val local = result.getString("TASK_ID_") == task.id
            val permitted = if (local) {
                name in setOf("nameMl", "taskComment", "lastComment", "completedBy") ||
                    (name.startsWith("_doc_") && syncService.getTaskSyncAttribute(name) != null)
            } else {
                name in setOf("documentRef", "documentType")
            }
            if (!permitted) return@query null
            val value = when (result.getString("VAR_TYPE_")) {
                "string" -> result.getString("TEXT_")
                "short", "integer", "long" -> result.getObject("LONG_")
                "double" -> result.getObject("DOUBLE_")
                "boolean" -> (result.getObject("LONG_") as? Number)?.let { it.toLong() != 0L }
                "date" -> (result.getObject("LONG_") as? Number)?.let { Instant.ofEpochMilli(it.toLong()) }
                "spin://application/json" -> {
                    val bytes = result.getBytes("SAVED_BYTES_")
                    val objectType = result.getString("TEXT2_") ?: ""
                    readJsonVariable(name, objectType, bytes)
                }
                else -> null
            }
            Triple(local, name, value)
        }
        val permitted = rows.filterNotNull()
        return permitted.filter { !it.first }.associate { it.second to it.third } to
            permitted.filter { it.first }.associate { it.second to it.third }
    }

    private fun readJsonVariable(name: String, objectType: String, bytes: ByteArray?): Any? {
        bytes ?: return null
        val targetType = when {
            name == "nameMl" && objectType == MLText::class.java.name -> MLText::class.java
            name == "nameMl" && objectType.startsWith("java.util.") && objectType.substringBefore('<').endsWith("Map") -> Map::class.java
            name != "nameMl" && objectType.startsWith("java.util.") && objectType.contains("List") -> List::class.java
            else -> return null
        }
        val text = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        return if (targetType == MLText::class.java) {
            // The platform Spin MLText codec writes binary JSON as a Base64 JSON string.
            // Only this whitelisted type is restored; objectType never selects a Java class.
            Json.mapper.readNotNull(Json.mapper.readNotNull(text, ByteArray::class.java), MLText::class.java)
        } else {
            Json.mapper.read(text, targetType)
        }
    }
}
