package ru.citeck.ecos.process.domain.bpmn.model.ecos.common

enum class RefBinding(val value: String) {
    DEPLOYMENT("deployment"),
    LATEST("latest"),
    VERSION("version"),
    VERSION_TAG("versionTag")
}
