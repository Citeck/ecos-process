package ru.citeck.ecos.process.common

fun generateElementId(prefix: String): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return "${prefix}_" + (1..7)
        .map { allowedChars.random() }
        .joinToString("")
}
