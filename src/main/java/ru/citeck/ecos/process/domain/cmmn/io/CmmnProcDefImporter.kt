package ru.citeck.ecos.process.domain.cmmn.io

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.CMMN_TYPE
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.Definitions
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.records2.RecordRef

@Component
class CmmnProcDefImporter {

    fun getDataToImport(definition: String, fileName: String): NewProcessDefDto {
        return getDataToImport(CmmnXmlUtils.readFromString(definition), fileName)
    }

    fun getDataToImport(bytes: ByteArray, fileName: String): NewProcessDefDto {
        return getDataToImport(CmmnXmlUtils.readFromString(String(bytes, Charsets.UTF_8)), fileName)
    }

    fun getDataToImport(definition: Definitions, fileName: String): NewProcessDefDto {

        val format = CmmnXmlUtils.getFormat(definition)
        val id = getProcDefId(definition, fileName)
        val caseAdditionalAtts = definition.case[0].otherAttributes.entries.associate {
            it.key.localPart to it.value
        }

        return NewProcessDefDto(
            id = id,
            name = getName(definition),
            procType = CMMN_TYPE,
            format = format.code,
            data = let {
                if (format == CmmnFormat.ECOS_CMMN) {
                    Json.mapper.toBytes(CmmnIO.importEcosCmmn(definition)) ?: error("Incorrect format. File: $fileName")
                } else {
                    definition.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID] = id
                    CmmnXmlUtils.writeToString(definition).toByteArray(Charsets.UTF_8)
                }
            },
            ecosTypeRef = getEcosType(definition, format, caseAdditionalAtts),
            alfType = if (format == CmmnFormat.LEGACY_CMMN) getLegacyAlfType(caseAdditionalAtts) else null,
            image = null,
            enabled = true
        )
    }

    private fun getName(definition: Definitions): MLText {
        val name = definition.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] ?: definition.name ?: ""
        if (name.isNotEmpty() && name[0] == '{') {
            return DataValue.create(name).getAs(MLText::class.java) ?: MLText()
        }
        return MLText(name)
    }

    private fun getEcosType(
        definition: Definitions,
        format: CmmnFormat,
        caseAdditionalAtts: Map<String, String>
    ): RecordRef {

        var ecosType = definition.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE]
        if (ecosType.isNullOrBlank() && format == CmmnFormat.LEGACY_CMMN) {
            ecosType = getLegacyCmmnEcosType(caseAdditionalAtts)
        }

        return RecordRef.valueOf(ecosType)
    }

    private fun getLegacyAlfType(caseAdditionalAtts: Map<String, String>): String? {
        return caseAdditionalAtts["caseType"]
    }

    private fun getLegacyCmmnEcosType(caseAdditionalAtts: Map<String, String>): String {

        var type = caseAdditionalAtts["caseEcosType"]
        type = if (!type.isNullOrBlank()) {
            type.replace("workspace-SpacesStore-", "")
        } else {
            return ""
        }
        var kind = caseAdditionalAtts["caseEcosKind"]
        if (!kind.isNullOrBlank()) {
            kind = kind.replace("workspace-SpacesStore-", "")
        }
        val typeId = type + if (StringUtils.isNotBlank(kind)) "/$kind" else ""
        return if (typeId.startsWith("emodel/type@")) {
            typeId
        } else {
            "emodel/type@$typeId"
        }
    }

    private fun getProcDefId(definition: Definitions, fileName: String): String {
        var procDefId = definition.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID]
        if (procDefId.isNullOrBlank() && fileName.isNotBlank()) {
            procDefId = fileName
        }
        return procDefId ?: ""
    }
}
