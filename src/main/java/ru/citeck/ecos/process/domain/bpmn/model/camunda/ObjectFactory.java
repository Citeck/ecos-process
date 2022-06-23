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

    private final static QName _CamundaTimeDate_QNAME = new QName(
        "http://camunda.org/schema/1.0/bpmn", "timeDate"
    );

    private final static QName _CamundaTimeCycle_QNAME = new QName(
        "http://camunda.org/schema/1.0/bpmn", "timeCycle"
    );

    private final static QName _CamundaTimeDuration_QNAME = new QName(
        "http://camunda.org/schema/1.0/bpmn", "timeDuration"
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

    public CamundaTimeDate createCamundaTimeDate() {
        return new CamundaTimeDate();
    }

    public CamundaTimeCycle createCamundaTimeCycle() {
        return new CamundaTimeCycle();
    }

    public CamundaTimeDuration createCamundaTimeDuration() {
        return new CamundaTimeDuration();
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "failedJobRetryTimeCycle")
    public JAXBElement<CamundaFailedJobRetryTimeCycle> createCamundaFailedJobRetryTimeCycle(
        CamundaFailedJobRetryTimeCycle value) {
        return new JAXBElement<CamundaFailedJobRetryTimeCycle>(
            _CamundaFailedJobRetryTimeCycle_QNAME,
            CamundaFailedJobRetryTimeCycle.class, null, value
        );
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "timeDate")
    public JAXBElement<CamundaTimeDate> createCamundaTimeDate(
        CamundaTimeDate value) {
        return new JAXBElement<CamundaTimeDate>(
            _CamundaTimeDate_QNAME,
            CamundaTimeDate.class, null, value
        );
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "timeCycle")
    public JAXBElement<CamundaTimeCycle> createCamundaTimeCycle(
        CamundaTimeCycle value) {
        return new JAXBElement<CamundaTimeCycle>(
            _CamundaTimeCycle_QNAME,
            CamundaTimeCycle.class, null, value
        );
    }

    @XmlElementDecl(namespace = "http://camunda.org/schema/1.0/bpmn", name = "timeDuration")
    public JAXBElement<CamundaTimeDuration> createCamundaTimeDuration(
        CamundaTimeDuration value) {
        return new JAXBElement<CamundaTimeDuration>(
            _CamundaTimeDuration_QNAME,
            CamundaTimeDuration.class, null, value
        );
    }

}
