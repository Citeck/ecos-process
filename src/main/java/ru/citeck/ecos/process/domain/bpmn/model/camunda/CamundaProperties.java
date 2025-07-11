package ru.citeck.ecos.process.domain.bpmn.model.camunda;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "properties", namespace = "http://camunda.org/schema/1.0/bpmn", 
    propOrder = {"properties"})
public class CamundaProperties {

    @XmlElement(name = "property", namespace = "http://camunda.org/schema/1.0/bpmn")
    private List<CamundaProperty> properties;

    public List<CamundaProperty> getProperties() {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        return properties;
    }

    public void setProperties(List<CamundaProperty> properties) {
        this.properties = properties;
    }
} 