
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 *         tCmmnElement is the base type for ALL CMMN complex types except tExpression.
 *
 *
 * <p>Java class for tCmmnElement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCmmnElement">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}documentation" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}extensionElements" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCmmnElement", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "documentation",
    "extensionElements"
})
@XmlSeeAlso({
    CaseFileItem.class,
    TApplicabilityRule.class,
    TPlanItemControl.class,
    TIfPart.class,
    TParameterMapping.class,
    Role.class,
    TRepetitionRule.class,
    TProperty.class,
    TCaseFileItemDefinition.class,
    TOnPart.class,
    TRequiredRule.class,
    Case.class,
    Sentry.class,
    TParameter.class,
    TStartTrigger.class,
    TDecision.class,
    TManualActivationRule.class,
    TTableItem.class,
    TPlanItem.class,
    TRelationship.class,
    TProcess.class,
    TPlanItemDefinition.class,
    TArtifact.class,
    CaseFile.class,
    CaseRoles.class,
    TChildren.class,
    TCriterion.class
})
public abstract class TCmmnElement {

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TDocumentation> documentation;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExtensionElements extensionElements;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<>();

    /**
     * Gets the value of the documentation property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the documentation property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDocumentation().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDocumentation }
     *
     *
     */
    public List<TDocumentation> getDocumentation() {
        if (documentation == null) {
            documentation = new ArrayList<>();
        }
        return this.documentation;
    }

    /**
     * Gets the value of the extensionElements property.
     *
     * @return
     *     possible object is
     *     {@link TExtensionElements }
     *
     */
    public TExtensionElements getExtensionElements() {
        return extensionElements;
    }

    /**
     * Sets the value of the extensionElements property.
     *
     * @param value
     *     allowed object is
     *     {@link TExtensionElements }
     *
     */
    public void setExtensionElements(TExtensionElements value) {
        this.extensionElements = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     *
     * <p>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     *
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     *
     *
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
