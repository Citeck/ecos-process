//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00 
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for tMessageFlowAssociation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tMessageFlowAssociation">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tBaseElement">
 *       &lt;attribute name="innerMessageFlowRef" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="outerMessageFlowRef" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tMessageFlowAssociation")
public class TMessageFlowAssociation
    extends TBaseElement
{

    @XmlAttribute(name = "innerMessageFlowRef", required = true)
    protected QName innerMessageFlowRef;
    @XmlAttribute(name = "outerMessageFlowRef", required = true)
    protected QName outerMessageFlowRef;

    /**
     * Gets the value of the innerMessageFlowRef property.
     * 
     * @return
     *     possible object is
     *     {@link QName }
     *     
     */
    public QName getInnerMessageFlowRef() {
        return innerMessageFlowRef;
    }

    /**
     * Sets the value of the innerMessageFlowRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link QName }
     *     
     */
    public void setInnerMessageFlowRef(QName value) {
        this.innerMessageFlowRef = value;
    }

    /**
     * Gets the value of the outerMessageFlowRef property.
     * 
     * @return
     *     possible object is
     *     {@link QName }
     *     
     */
    public QName getOuterMessageFlowRef() {
        return outerMessageFlowRef;
    }

    /**
     * Sets the value of the outerMessageFlowRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link QName }
     *     
     */
    public void setOuterMessageFlowRef(QName value) {
        this.outerMessageFlowRef = value;
    }

}