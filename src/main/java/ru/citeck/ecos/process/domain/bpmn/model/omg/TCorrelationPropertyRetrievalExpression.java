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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for tCorrelationPropertyRetrievalExpression complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCorrelationPropertyRetrievalExpression">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tBaseElement">
 *       &lt;sequence>
 *         &lt;element name="messagePath" type="{http://www.omg.org/spec/BPMN/20100524/MODEL}tFormalExpression"/>
 *       &lt;/sequence>
 *       &lt;attribute name="messageRef" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCorrelationPropertyRetrievalExpression", propOrder = {
    "messagePath"
})
public class TCorrelationPropertyRetrievalExpression
    extends TBaseElement
{

    @XmlElement(required = true)
    protected TFormalExpression messagePath;
    @XmlAttribute(name = "messageRef", required = true)
    protected QName messageRef;

    /**
     * Gets the value of the messagePath property.
     *
     * @return
     *     possible object is
     *     {@link TFormalExpression }
     *
     */
    public TFormalExpression getMessagePath() {
        return messagePath;
    }

    /**
     * Sets the value of the messagePath property.
     *
     * @param value
     *     allowed object is
     *     {@link TFormalExpression }
     *
     */
    public void setMessagePath(TFormalExpression value) {
        this.messagePath = value;
    }

    /**
     * Gets the value of the messageRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getMessageRef() {
        return messageRef;
    }

    /**
     * Sets the value of the messageRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setMessageRef(QName value) {
        this.messageRef = value;
    }

}
