package ru.citeck.ecos.process.domain.procdef.convert.io.convert

import ru.citeck.ecos.process.domain.cmmn.model.ExtensionType
import java.util.concurrent.ConcurrentHashMap

object ConvertUtils {

    private val typeIdByClass = ConcurrentHashMap<Class<*>, String>()

    fun getTypeByClass(clazz: Class<*>): String {
        return typeIdByClass.computeIfAbsent(clazz) {
            var name = it.simpleName
            if (name.endsWith("Def")) {
                name = name.substring(0, name.length - 3)
            }
            val prefix = when {
                it.getAnnotation(ExtensionType::class.java) != null -> {
                    "ecos:"
                }
                it.name.contains(".cmmn.") -> {
                    "cmmn:"
                }
                it.name.contains(".bpmn.") -> {
                    "bpmn:"
                }
                else -> {
                    error("Unknown type: $it")
                }
            }
            prefix + name
        }
    }
}
