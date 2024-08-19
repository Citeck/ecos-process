
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tChildren defines a container for zero or more "caseFileItem" elements.
 *
 *
 * <p>Java class for tChildren complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tChildren">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}caseFileItem" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
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
@XmlType(name = "tChildren", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "caseFileItem"
})
public class TChildren
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<CaseFileItem> caseFileItem;

    /**
     * Gets the value of the caseFileItem property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the caseFileItem property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCaseFileItem().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CaseFileItem }
     *
     *
     */
    public List<CaseFileItem> getCaseFileItem() {
        if (caseFileItem == null) {
            caseFileItem = new ArrayList<>();
        }
        return this.caseFileItem;
    }

}
