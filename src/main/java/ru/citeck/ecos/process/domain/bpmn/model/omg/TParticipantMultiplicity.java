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
 * <p>Java class for tParticipantMultiplicity complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tParticipantMultiplicity">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tBaseElement">
 *       &lt;attribute name="minimum" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *       &lt;attribute name="maximum" type="{http://www.w3.org/2001/XMLSchema}int" default="1" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tParticipantMultiplicity")
public class TParticipantMultiplicity
    extends TBaseElement
{

    @XmlAttribute(name = "minimum")
    protected Integer minimum;
    @XmlAttribute(name = "maximum")
    protected Integer maximum;

    /**
     * Gets the value of the minimum property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public int getMinimum() {
        if (minimum == null) {
            return  0;
        } else {
            return minimum;
        }
    }

    /**
     * Sets the value of the minimum property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setMinimum(Integer value) {
        this.minimum = value;
    }

    /**
     * Gets the value of the maximum property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public int getMaximum() {
        if (maximum == null) {
            return  1;
        } else {
            return maximum;
        }
    }

    /**
     * Sets the value of the maximum property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setMaximum(Integer value) {
        this.maximum = value;
    }

}
