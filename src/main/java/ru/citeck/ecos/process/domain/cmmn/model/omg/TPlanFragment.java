
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tPlanFragment defines the type for element "planFragment"
 *
 *
 * <p>Java class for tPlanFragment complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlanFragment">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tPlanItemDefinition">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}planItem" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}sentry" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlanFragment", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "planItem",
    "sentry"
})
@XmlSeeAlso({
    Stage.class
})
public class TPlanFragment
    extends TPlanItemDefinition
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TPlanItem> planItem;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<Sentry> sentry;

    /**
     * Gets the value of the planItem property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the planItem property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPlanItem().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TPlanItem }
     *
     *
     */
    public List<TPlanItem> getPlanItem() {
        if (planItem == null) {
            planItem = new ArrayList<>();
        }
        return this.planItem;
    }

    /**
     * Gets the value of the sentry property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sentry property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSentry().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Sentry }
     *
     *
     */
    public List<Sentry> getSentry() {
        if (sentry == null) {
            sentry = new ArrayList<>();
        }
        return this.sentry;
    }

}
