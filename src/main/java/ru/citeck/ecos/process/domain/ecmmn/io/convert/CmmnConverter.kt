package ru.citeck.ecos.process.domain.ecmmn.io.convert

import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext

interface CmmnConverter<OmgT, EcosT> {

    fun import(element: OmgT, context: ImportContext): EcosT

    fun export(element: EcosT, context: ExportContext): OmgT

    fun getElementType(): String

    fun isExtensionType(): Boolean = false
}
