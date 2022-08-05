package ru.citeck.ecos.process.domain.bpmn.model.camunda;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "field", namespace = "http://camunda.org/schema/1.0/bpmn", propOrder = {"name", "stringValue"})
public class CamundaField {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "string", namespace = "http://camunda.org/schema/1.0/bpmn", required = true)
    private CamundaString stringValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CamundaString getStringValue() {
        return stringValue;
    }

    public void setStringValue(CamundaString stringValue) {
        this.stringValue = stringValue;
    }

}
