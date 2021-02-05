package ru.citeck.ecos.process.domain.procdef.convert.io.convert

import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

interface EcosOmgConverter<EcosT, OmgT> {

    fun import(element: OmgT, context: ImportContext): EcosT

    fun export(element: EcosT, context: ExportContext): OmgT
}
