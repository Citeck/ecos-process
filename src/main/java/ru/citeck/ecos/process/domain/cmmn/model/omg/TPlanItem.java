
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for tPlanItem complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlanItem">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element name="itemControl" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tPlanItemControl" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}entryCriterion" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}exitCriterion" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="definitionRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlanItem", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "itemControl",
    "entryCriterion",
    "exitCriterion"
})
public class TPlanItem
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TPlanItemControl itemControl;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TEntryCriterion> entryCriterion;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TExitCriterion> exitCriterion;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "definitionRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object definitionRef;

    /**
     * Gets the value of the itemControl property.
     *
     * @return
     *     possible object is
     *     {@link TPlanItemControl }
     *
     */
    public TPlanItemControl getItemControl() {
        return itemControl;
    }

    /**
     * Sets the value of the itemControl property.
     *
     * @param value
     *     allowed object is
     *     {@link TPlanItemControl }
     *
     */
    public void setItemControl(TPlanItemControl value) {
        this.itemControl = value;
    }

    /**
     * Gets the value of the entryCriterion property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the entryCriterion property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEntryCriterion().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TEntryCriterion }
     *
     *
     */
    public List<TEntryCriterion> getEntryCriterion() {
        if (entryCriterion == null) {
            entryCriterion = new ArrayList<>();
        }
        return this.entryCriterion;
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

    /**
     * Gets the value of the definitionRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getDefinitionRef() {
        return definitionRef;
    }

    /**
     * Sets the value of the definitionRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setDefinitionRef(Object value) {
        this.definitionRef = value;
    }


    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TPlanItem tPlanItem = (TPlanItem) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(getItemControl(), tPlanItem.getItemControl())
            .append(getEntryCriterion(), tPlanItem.getEntryCriterion())
            .append(getExitCriterion(), tPlanItem.getExitCriterion())
            .append(getName(), tPlanItem.getName())
            .append(CmmnXmlUtils.idRefToId(getDefinitionRef()), CmmnXmlUtils.idRefToId(tPlanItem.getDefinitionRef()))
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(getItemControl())
            .append(getEntryCriterion())
            .append(getExitCriterion())
            .append(getName())
            .append(CmmnXmlUtils.idRefToId(getDefinitionRef()))
            .toHashCode();
    }
}
