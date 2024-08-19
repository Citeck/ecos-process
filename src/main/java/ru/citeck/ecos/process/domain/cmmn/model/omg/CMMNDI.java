
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for CMMNDI complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CMMNDI">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/CMMNDI}CMMNDiagram" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/CMMNDI}CMMNStyle" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CMMNDI", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", propOrder = {
    "cmmnDiagram",
    "cmmnStyle"
})
public class CMMNDI {

    @XmlElement(name = "CMMNDiagram", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI")
    protected List<CMMNDiagram> cmmnDiagram;
    @XmlElement(name = "CMMNStyle", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI")
    protected List<CMMNStyle> cmmnStyle;

    /**
     * Gets the value of the cmmnDiagram property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cmmnDiagram property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCMMNDiagram().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CMMNDiagram }
     *
     *
     */
    public List<CMMNDiagram> getCMMNDiagram() {
        if (cmmnDiagram == null) {
            cmmnDiagram = new ArrayList<>();
        }
        return this.cmmnDiagram;
    }

    /**
     * Gets the value of the cmmnStyle property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cmmnStyle property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCMMNStyle().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CMMNStyle }
     *
     *
     */
    public List<CMMNStyle> getCMMNStyle() {
        if (cmmnStyle == null) {
            cmmnStyle = new ArrayList<>();
        }
        return this.cmmnStyle;
    }

}
