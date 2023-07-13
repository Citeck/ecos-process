package ru.citeck.ecos.process.domain.bpmn.model.ecos.common

data class VariablesMappingPropagation(
    val propagateAllVariable: Boolean = false,
    val local: Boolean = false,
    val variables: List<Variables> = emptyList()
)

data class Variables(
    val source: String,
    val target: String,
    val local: Boolean = false
) {

    init {
        if (source.isNotEmpty() && target.isBlank()) {
            throw IllegalArgumentException("Target cannot be blank on variables mapping propagation")
        }
    }

}
