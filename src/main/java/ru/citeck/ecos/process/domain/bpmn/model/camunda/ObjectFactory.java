package ru.citeck.ecos.process.domain.bpmn.model.camunda;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

    private final static QName _CamundaField_QNAME = new QName("http://camunda.org/schema/1.0/bpmn",
        "field");

    public CamundaField createCamundaField() {
        return new CamundaField();
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "field")
    public JAXBElement<CamundaField> createBPMNEdge(CamundaField value) {
        return new JAXBElement<CamundaField>(_CamundaField_QNAME, CamundaField.class, null, value);
    }

}
