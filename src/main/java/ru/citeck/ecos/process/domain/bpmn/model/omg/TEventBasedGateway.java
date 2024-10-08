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


/**
 * <p>Java class for tEventBasedGateway complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tEventBasedGateway">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tGateway">
 *       &lt;attribute name="instantiate" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="eventGatewayType" type="{http://www.omg.org/spec/BPMN/20100524/MODEL}tEventBasedGatewayType" default="Exclusive" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tEventBasedGateway")
public class TEventBasedGateway
    extends TGateway
{

    @XmlAttribute(name = "instantiate")
    protected Boolean instantiate;
    @XmlAttribute(name = "eventGatewayType")
    protected TEventBasedGatewayType eventGatewayType;

    /**
     * Gets the value of the instantiate property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public boolean isInstantiate() {
        if (instantiate == null) {
            return false;
        } else {
            return instantiate;
        }
    }

    /**
     * Sets the value of the instantiate property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setInstantiate(Boolean value) {
        this.instantiate = value;
    }

    /**
     * Gets the value of the eventGatewayType property.
     *
     * @return
     *     possible object is
     *     {@link TEventBasedGatewayType }
     *
     */
    public TEventBasedGatewayType getEventGatewayType() {
        if (eventGatewayType == null) {
            return TEventBasedGatewayType.EXCLUSIVE;
        } else {
            return eventGatewayType;
        }
    }

    /**
     * Sets the value of the eventGatewayType property.
     *
     * @param value
     *     allowed object is
     *     {@link TEventBasedGatewayType }
     *
     */
    public void setEventGatewayType(TEventBasedGatewayType value) {
        this.eventGatewayType = value;
    }

}
