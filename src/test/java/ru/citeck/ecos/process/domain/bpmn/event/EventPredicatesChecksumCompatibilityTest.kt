package ru.citeck.ecos.process.domain.bpmn.event

import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.digest.DigestUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import kotlin.test.assertEquals

class EventPredicatesChecksumCompatibilityTest {

    @Test
    fun `check same predicates checksum`() {
        val pr1: Predicate = Predicates.eq("key1", "value1")
        val d1 = DigestUtils.getSha256(Json.mapper.toBytes(pr1) ?: ByteArray(0)).hash

        val pr2: Predicate = Predicates.eq("key1", "value1")
        val d2 = DigestUtils.getSha256(Json.mapper.toBytes(pr2) ?: ByteArray(0)).hash

        val pr3: Predicate = Predicates.or(Predicates.eq("key1", "value1"))
        val d3 = DigestUtils.getSha256(Json.mapper.toBytes(pr3) ?: ByteArray(0)).hash

        assertEquals(pr1, pr2)

        assertEquals(d1, d2)
        assertEquals(d2, d3)
        assertEquals(d1, d3)
    }

}
