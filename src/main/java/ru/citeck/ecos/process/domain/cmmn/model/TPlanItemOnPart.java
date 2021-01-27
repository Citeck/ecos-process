
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tPlanItemOnPart complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlanItemOnPart">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tOnPart">
 *       &lt;sequence>
 *         &lt;element name="standardEvent" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}PlanItemTransition" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="sourceRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="exitCriterionRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlanItemOnPart", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "standardEvent"
})
public class TPlanItemOnPart
    extends TOnPart
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected PlanItemTransition standardEvent;
    @XmlAttribute(name = "sourceRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object sourceRef;
    @XmlAttribute(name = "exitCriterionRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object exitCriterionRef;

    /**
     * Gets the value of the standardEvent property.
     *
     * @return
     *     possible object is
     *     {@link PlanItemTransition }
     *
     */
    public PlanItemTransition getStandardEvent() {
        return standardEvent;
    }

    /**
     * Sets the value of the standardEvent property.
     *
     * @param value
     *     allowed object is
     *     {@link PlanItemTransition }
     *
     */
    public void setStandardEvent(PlanItemTransition value) {
        this.standardEvent = value;
    }

    /**
     * Gets the value of the sourceRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getSourceRef() {
        return sourceRef;
    }

    /**
     * Sets the value of the sourceRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setSourceRef(Object value) {
        this.sourceRef = value;
    }

    /**
     * Gets the value of the exitCriterionRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getExitCriterionRef() {
        return exitCriterionRef;
    }

    /**
     * Sets the value of the exitCriterionRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setExitCriterionRef(Object value) {
        this.exitCriterionRef = value;
    }

}
