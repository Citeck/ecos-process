//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for tServiceTask complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tServiceTask">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tTask">
 *       &lt;attribute name="implementation" type="{http://www.omg.org/spec/BPMN/20100524/MODEL}tImplementation" default="##WebService" />
 *       &lt;attribute name="operationRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tServiceTask")
public class TServiceTask
    extends TTask
{

    @XmlAttribute(name = "implementation")
    protected String implementation;
    @XmlAttribute(name = "operationRef")
    protected QName operationRef;

    /**
     * Gets the value of the implementation property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getImplementation() {
        if (implementation == null) {
            return "##WebService";
        } else {
            return implementation;
        }
    }

    /**
     * Sets the value of the implementation property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setImplementation(String value) {
        this.implementation = value;
    }

    /**
     * Gets the value of the operationRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getOperationRef() {
        return operationRef;
    }

    /**
     * Sets the value of the operationRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setOperationRef(QName value) {
        this.operationRef = value;
    }

}
