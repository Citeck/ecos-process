package ru.citeck.ecos.process.domain.cmmn.io

enum class CmmnFormat(val code: String) {
    ECOS_CMMN("ecos-cmmn"),
    LEGACY_CMMN("xml");

    companion object {
        fun getByCode(code: String): CmmnFormat {
            return values().find { it.code == code } ?: error("Unknown code: $code")
        }
    }
}
