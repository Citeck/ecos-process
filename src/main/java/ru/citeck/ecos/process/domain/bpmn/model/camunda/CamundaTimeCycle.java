package ru.citeck.ecos.process.domain.bpmn.model.camunda;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "timeCycle", namespace = "http://camunda.org/schema/1.0/bpmn")
public class CamundaTimeCycle {

    @XmlValue
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
