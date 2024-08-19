package ru.citeck.ecos.process.domain.bpmn.model.camunda;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "out", namespace = "http://camunda.org/schema/1.0/bpmn",
    propOrder = {"source", "sourceExpression", "variables", "target", "local"})
public class CamundaOut {

    @XmlAttribute(name = "source")
    private String source;

    @XmlAttribute(name = "sourceExpression")
    private String sourceExpression;

    @XmlAttribute(name = "variables")
    private String variables;

    @XmlAttribute(name = "target")
    private String target;

    @XmlAttribute(name = "local")
    private String local;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceExpression() {
        return sourceExpression;
    }

    public void setSourceExpression(String sourceExpression) {
        this.sourceExpression = sourceExpression;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

}
