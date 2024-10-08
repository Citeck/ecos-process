//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tConditionalEventDefinition complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tConditionalEventDefinition">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tEventDefinition">
 *       &lt;sequence>
 *         &lt;element name="condition" type="{http://www.omg.org/spec/BPMN/20100524/MODEL}tExpression"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tConditionalEventDefinition", propOrder = {
    "condition"
})
public class TConditionalEventDefinition
    extends TEventDefinition
{

    @XmlElement(required = true)
    protected TExpression condition;

    /**
     * Gets the value of the condition property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getCondition() {
        return condition;
    }

    /**
     * Sets the value of the condition property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setCondition(TExpression value) {
        this.condition = value;
    }

}
