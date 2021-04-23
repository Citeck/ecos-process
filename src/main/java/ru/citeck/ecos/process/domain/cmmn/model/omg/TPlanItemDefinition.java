
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tPlanItemDefinition complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlanItemDefinition">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element name="defaultControl" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tPlanItemControl" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlanItemDefinition", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "defaultControl"
})
@XmlSeeAlso({
    TPlanFragment.class,
    TTask.class,
    TMilestone.class,
    TEventListener.class
})
public abstract class TPlanItemDefinition
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TPlanItemControl defaultControl;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the defaultControl property.
     *
     * @return
     *     possible object is
     *     {@link TPlanItemControl }
     *
     */
    public TPlanItemControl getDefaultControl() {
        return defaultControl;
    }

    /**
     * Sets the value of the defaultControl property.
     *
     * @param value
     *     allowed object is
     *     {@link TPlanItemControl }
     *
     */
    public void setDefaultControl(TPlanItemControl value) {
        this.defaultControl = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

}
