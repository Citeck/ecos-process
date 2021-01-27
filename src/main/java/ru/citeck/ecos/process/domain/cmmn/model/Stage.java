
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tStage defines the type for element "stage"
 *
 *
 * <p>Java class for tStage complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tStage">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tPlanFragment">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}planningTable" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}planItemDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}exitCriterion" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="autoComplete" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tStage", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "planningTable",
    "planItemDefinition",
    "exitCriterion"
})
public class Stage
    extends TPlanFragment
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TPlanningTable planningTable;
    @XmlElementRef(name = "planItemDefinition", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<? extends TPlanItemDefinition>> planItemDefinition;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TExitCriterion> exitCriterion;
    @XmlAttribute(name = "autoComplete")
    protected Boolean autoComplete;

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
     * Gets the value of the planItemDefinition property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the planItemDefinition property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPlanItemDefinition().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TUserEventListener }{@code >}
     * {@link JAXBElement }{@code <}{@link TTimerEventListener }{@code >}
     * {@link JAXBElement }{@code <}{@link Stage }{@code >}
     * {@link JAXBElement }{@code <}{@link TTask }{@code >}
     * {@link JAXBElement }{@code <}{@link TProcessTask }{@code >}
     * {@link JAXBElement }{@code <}{@link TPlanItemDefinition }{@code >}
     * {@link JAXBElement }{@code <}{@link TPlanFragment }{@code >}
     * {@link JAXBElement }{@code <}{@link TMilestone }{@code >}
     * {@link JAXBElement }{@code <}{@link TDecisionTask }{@code >}
     * {@link JAXBElement }{@code <}{@link THumanTask }{@code >}
     * {@link JAXBElement }{@code <}{@link TCaseTask }{@code >}
     * {@link JAXBElement }{@code <}{@link TEventListener }{@code >}
     *
     *
     */
    public List<JAXBElement<? extends TPlanItemDefinition>> getPlanItemDefinition() {
        if (planItemDefinition == null) {
            planItemDefinition = new ArrayList<>();
        }
        return this.planItemDefinition;
    }

    /**
     * Gets the value of the exitCriterion property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the exitCriterion property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExitCriterion().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TExitCriterion }
     *
     *
     */
    public List<TExitCriterion> getExitCriterion() {
        if (exitCriterion == null) {
            exitCriterion = new ArrayList<>();
        }
        return this.exitCriterion;
    }

    /**
     * Gets the value of the autoComplete property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public boolean isAutoComplete() {
        if (autoComplete == null) {
            return false;
        } else {
            return autoComplete;
        }
    }

    /**
     * Sets the value of the autoComplete property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setAutoComplete(Boolean value) {
        this.autoComplete = value;
    }

}
