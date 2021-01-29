
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for tTableItem complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tTableItem">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;attribute name="applicabilityRuleRefs" type="{http://www.w3.org/2001/XMLSchema}IDREFS" />
 *       &lt;attribute name="authorizedRoleRefs" type="{http://www.w3.org/2001/XMLSchema}IDREFS" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tTableItem", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlSeeAlso({
    TPlanningTable.class,
    TDiscretionaryItem.class
})
public abstract class TTableItem
    extends TCmmnElement
{

    @XmlAttribute(name = "applicabilityRuleRefs")
    @XmlIDREF
    @XmlSchemaType(name = "IDREFS")
    protected List<Object> applicabilityRuleRefs;
    @XmlAttribute(name = "authorizedRoleRefs")
    @XmlIDREF
    @XmlSchemaType(name = "IDREFS")
    protected List<Object> authorizedRoleRefs;

    /**
     * Gets the value of the applicabilityRuleRefs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the applicabilityRuleRefs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getApplicabilityRuleRefs().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     *
     *
     */
    public List<Object> getApplicabilityRuleRefs() {
        if (applicabilityRuleRefs == null) {
            applicabilityRuleRefs = new ArrayList<>();
        }
        return this.applicabilityRuleRefs;
    }

    /**
     * Gets the value of the authorizedRoleRefs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authorizedRoleRefs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthorizedRoleRefs().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     *
     *
     */
    public List<Object> getAuthorizedRoleRefs() {
        if (authorizedRoleRefs == null) {
            authorizedRoleRefs = new ArrayList<>();
        }
        return this.authorizedRoleRefs;
    }

}
