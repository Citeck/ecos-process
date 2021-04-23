
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;


/**
 *
 *         tHumanTask defines the type of element "humanTask"
 *
 *
 * <p>Java class for tHumanTask complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tHumanTask">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tTask">
 *       &lt;sequence>
 *         &lt;element name="planningTable" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tPlanningTable" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="performerRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
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
@XmlType(name = "tHumanTask", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "planningTable"
})
public class THumanTask
    extends TTask
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TPlanningTable planningTable;
    @XmlAttribute(name = "performerRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object performerRef;

    /**
     * Gets the value of the planningTable property.
     *
     * @return
     *     possible object is
     *     {@link TPlanningTable }
     *
     */
    public TPlanningTable getPlanningTable() {
        return planningTable;
    }

    /**
     * Sets the value of the planningTable property.
     *
     * @param value
     *     allowed object is
     *     {@link TPlanningTable }
     *
     */
    public void setPlanningTable(TPlanningTable value) {
        this.planningTable = value;
    }

    /**
     * Gets the value of the performerRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getPerformerRef() {
        return performerRef;
    }

    /**
     * Sets the value of the performerRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setPerformerRef(Object value) {
        this.performerRef = value;
    }

}
