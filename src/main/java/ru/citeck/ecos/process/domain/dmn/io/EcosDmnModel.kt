package ru.citeck.ecos.process.domain.dmn.io

import ru.citeck.ecos.process.domain.bpmn.io.NS_ECOS_BPMN
import javax.xml.namespace.QName

const val NS_DMN = "https://www.omg.org/spec/DMN/20191111/MODEL/"
const val NS_ECOS_DMN = "http://www.citeck.ru/ecos/dmn/1.0"

val DMN_PROP_NAME_ML = QName(NS_ECOS_DMN, "name_ml")
val DMN_PROP_MODEL = QName(NS_ECOS_DMN, "model")
val DMN_PROP_SECTION_REF = QName(NS_ECOS_DMN, "sectionRef")
val DMN_PROP_DEF_ID = QName(NS_ECOS_DMN, "defId")

val DMN_PROP_ECOS_DMN_TYPE = QName(NS_ECOS_BPMN, "dmnType")
