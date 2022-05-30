package ru.citeck.ecos.process.domain.bpmn.model.camunda;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

    private final static QName _CamundaField_QNAME = new QName("http://camunda.org/schema/1.0/bpmn",
        "field");

    private final static QName _CamundaFailedJobRetryTimeCycle_QNAME = new QName(
        "http://camunda.org/schema/1.0/bpmn", "failedJobRetryTimeCycle"
    );

    public CamundaField createCamundaField() {
        return new CamundaField();
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "field")
    public JAXBElement<CamundaField> createCamundaField(CamundaField value) {
        return new JAXBElement<CamundaField>(_CamundaField_QNAME, CamundaField.class, null, value);
    }

    public CamundaFailedJobRetryTimeCycle createCamundaFailedJobRetryTimeCycle() {
        return new CamundaFailedJobRetryTimeCycle();
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "failedJobRetryTimeCycle")
    public JAXBElement<CamundaFailedJobRetryTimeCycle> createCamundaFailedJobRetryTimeCycle(
        CamundaFailedJobRetryTimeCycle value) {
        return new JAXBElement<CamundaFailedJobRetryTimeCycle>(
            _CamundaFailedJobRetryTimeCycle_QNAME,
            CamundaFailedJobRetryTimeCycle.class, null, value
        );
    }

}
