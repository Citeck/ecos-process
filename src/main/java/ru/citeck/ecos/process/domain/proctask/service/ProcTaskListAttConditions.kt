package ru.citeck.ecos.process.domain.proctask.service

import com.fasterxml.jackson.core.io.JsonStringEncoder
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records2.predicate.model.ValuePredicate

/**
 * Matching rules for multi-valued task attributes, which Camunda stores as a JSON list in
 * act_ge_bytearray. Kept apart from [ProcTaskSqlQueryBuilder]: these are pure functions of the
 * predicate and the value, the builder only places the resulting SQL into the query.
 */
internal object ProcTaskListAttConditions {

    /**
     * Multi-valued attributes are stored as a JSON list in act_ge_bytearray, so only textual
     * matching is possible: exact element match (EQ/IN on string-like types) and substring
     * match (CONTAINS/LIKE). Range predicates and exact match on non-textual element types
     * cannot be expressed over the serialized form.
     */
    fun isSupported(predicateType: ValuePredicate.Type, attType: AttributeType): Boolean {
        return when (predicateType) {
            ValuePredicate.Type.EQ,
            ValuePredicate.Type.IN -> isStringLikeAttType(attType)

            ValuePredicate.Type.CONTAINS,
            ValuePredicate.Type.LIKE -> true

            else -> false
        }
    }

    /**
     * LIKE pattern matching one whole element of a JSON-serialized list (["foo","bar"]): the value
     * is encoded as a JSON string with its quotes, so 'foo' does not match 'foobar', values with
     * quotes or backslashes match their stored (escaped) form, and LIKE wildcards inside the value
     * are literal.
     */
    fun elementLikePattern(value: Any?): String = "%${escapeSqlLikeWildcards(toJsonStringToken(value))}%"

    /**
     * PostgreSQL treats a backslash as the default LIKE escape character, so it is doubled first
     * and then the '%' and '_' wildcards are escaped to match literally.
     */
    private fun escapeSqlLikeWildcards(value: String): String = value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    /**
     * Encodes the value exactly like Jackson writes a JSON string element (Camunda Spin
     * serializes list variables through Jackson), including the surrounding quotes.
     */
    private fun toJsonStringToken(value: Any?): String {
        val str = value?.toString() ?: return "null"
        return "\"${String(JsonStringEncoder.getInstance().quoteAsString(str))}\""
    }

    private fun isStringLikeAttType(attType: AttributeType): Boolean = when (attType) {
        AttributeType.TEXT,
        AttributeType.ASSOC,
        AttributeType.PERSON,
        AttributeType.AUTHORITY,
        AttributeType.AUTHORITY_GROUP -> true
        else -> false
    }
}
