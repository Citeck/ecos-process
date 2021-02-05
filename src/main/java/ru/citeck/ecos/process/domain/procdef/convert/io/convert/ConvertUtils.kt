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
            if (clazz.getAnnotation(ExtensionType::class.java) != null) {
                "ecos:"
            } else if (clazz.name.contains(".cmmn.")) {
                "cmmn:"
            } else if (clazz.name.contains(".bpmn.")) {
                "bpmn:"
            } else {
                error("Unknown type: $it")
            } + name
        }
    }
}
