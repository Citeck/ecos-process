package ru.citeck.ecos.process.domain.cmmn

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBElement
import ru.citeck.beans.BeanUtils
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement

object CmmnComparator {

    private val log = KotlinLogging.logger {}

    fun compare(
        expected: Any?,
        actual: Any?,
        excludedProps: Map<Class<*>, Set<String>>,
        idRefsProps: Map<Class<*>, Set<String>>
    ): Boolean {
        return compare(emptyList(), expected, actual, CompareContext(excludedProps, idRefsProps))
    }

    fun sortAllById(value: Any?) {

        value ?: return

        val descriptors = BeanUtils.getProperties(value.javaClass)

        for (descriptor in descriptors) {

            val readMethod = descriptor.getReadMethod() ?: continue

            val propValue = readMethod.invoke(value) ?: continue

            if (propValue is MutableList<*>) {

                val listValue = propValue as? MutableList<*> ?: continue
                listValue.sortBy { getId(it) }
                listValue.forEach { sortAllById(it) }
            } else if (propValue is Map<*, *>) {

                propValue.values.forEach { sortAllById(it) }
            }
        }
    }

    private fun compare(
        path: List<String>,
        rawExpected: Any?,
        rawActual: Any?,
        context: CompareContext
    ): Boolean {

        val expected = unwrap(rawExpected)
        val actual = unwrap(rawActual)

        if (expected == actual) {
            return true
        }

        val logNotEq = {
            log.error { "$path Not equals. Expected: $expected Actual: $actual" }
        }

        if (expected == null || actual == null) {
            logNotEq.invoke()
            return false
        }

        if (expected is Collection<*> && expected !is List<*>) {
            log.error { "Collection type is not supported: ${expected::class.java}" }
            return false
        }

        if (expected is String || expected is Number || expected is Boolean) {
            logNotEq.invoke()
            return false
        }

        if (expected is List<*>) {
            if (actual !is List<*>) {
                log.error { "$path expected list but found ${actual::class.java}" }
                return false
            }
            if (expected.size != actual.size) {
                log.error { "$path expected list size ${expected.size} but actual size: ${actual.size}" }
                return false
            }
            if (expected.size > 0) {

                var isAnyNotEquals = false
                for (i in 0 until expected.size) {
                    val compareResult = compare(
                        listOf(*path.toTypedArray(), "[$i:'${getId(expected[i])}']"),
                        expected[i],
                        actual[i],
                        context
                    )
                    isAnyNotEquals = isAnyNotEquals || !compareResult
                }
                if (!isAnyNotEquals) {
                    return true
                }
            }
        } else if (expected is Map<*, *>) {

            if (actual !is Map<*, *>) {
                log.error { "$path expected map but found ${actual::class.java}" }
                return false
            }
            if (expected.size != actual.size) {
                log.error { "$path expected map size ${expected.size} but actual size: ${actual.size}" }
            }
            val allKeys = setOf(*expected.keys.toTypedArray(), *actual.keys.toTypedArray())
            for (key in allKeys) {
                compare(
                    listOf(*path.toTypedArray(), key.toString()),
                    expected[key],
                    actual[key],
                    context
                )
            }
        } else {

            val descriptors = BeanUtils.getProperties(expected.javaClass)
            if (descriptors.isEmpty()) {
                logNotEq.invoke()
                return false
            }
            var notEqualsObjIsFound = false
            for (descriptor in descriptors) {

                val readMethod = descriptor.getReadMethod() ?: continue
                val propName = descriptor.getName()

                val propClass = readMethod.declaringClass
                val exludedByClass = context.excludedProps[propClass] ?: emptySet()
                val idRefsByClass = context.idRefsProps[propClass] ?: emptySet()

                if (exludedByClass.contains(propName)) {
                    continue
                }
                try {
                    val innerExpectedRaw = readMethod.invoke(expected)
                    val innerActualRaw = readMethod.invoke(actual)

                    val innerExpected = if (idRefsByClass.contains(propName)) {
                        CmmnXmlUtils.idRefToId(innerExpectedRaw)
                    } else {
                        innerExpectedRaw
                    }
                    val innerActual = if (idRefsByClass.contains(propName)) {
                        CmmnXmlUtils.idRefToId(innerActualRaw)
                    } else {
                        innerActualRaw
                    }

                    if (!compare(
                            listOf(*path.toTypedArray(), propName),
                            innerExpected,
                            innerActual,
                            context
                        )
                    ) {
                        notEqualsObjIsFound = true
                    }
                } catch (e: Exception) {
                    log.error {
                        "Failed to read property $propName " +
                            "by getter ${readMethod.name}. Msg: ${e.message}"
                    }
                }
            }

            if (!notEqualsObjIsFound) {
                logNotEq.invoke()
            }
        }

        return false
    }

    private fun getId(any: Any?): String {
        val value = unwrap(any) ?: return ""
        if (value is TCmmnElement) {
            return value.id
        }
        if (value is DiagramElement) {
            return value.id
        }
        return ""
    }

    private fun unwrap(any: Any?): Any? {
        any ?: return any
        if (any is JAXBElement<*>) {
            return any.value
        }
        if (any is List<*>) {
            return any.map { unwrap(it) }
        }
        return any
    }

    private class CompareContext(
        val excludedProps: Map<Class<*>, Set<String>>,
        val idRefsProps: Map<Class<*>, Set<String>>
    )
}
