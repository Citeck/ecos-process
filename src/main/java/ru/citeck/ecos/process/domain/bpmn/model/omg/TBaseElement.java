//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;


/**
 * <p>Java class for tBaseElement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tBaseElement">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/BPMN/20100524/MODEL}documentation" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/BPMN/20100524/MODEL}extensionElements" minOccurs="0"/>
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
@XmlType(name = "tBaseElement", propOrder = {
    "documentation",
    "extensionElements"
})
@XmlSeeAlso({
    TConversationAssociation.class,
    TInputOutputBinding.class,
    TAuditing.class,
    TOutputSet.class,
    TDataOutput.class,
    TProperty.class,
    TRendering.class,
    TMonitoring.class,
    TLaneSet.class,
    TInputSet.class,
    TConversationLink.class,
    TMessageFlow.class,
    TResourceAssignmentExpression.class,
    TParticipantAssociation.class,
    TRelationship.class,
    TDataInput.class,
    TComplexBehaviorDefinition.class,
    TOperation.class,
    TResourceParameter.class,
    TParticipant.class,
    TCorrelationSubscription.class,
    TConversationNode.class,
    TArtifact.class,
    TDataState.class,
    TMessageFlowAssociation.class,
    TCategoryValue.class,
    TLoopCharacteristics.class,
    TCorrelationPropertyBinding.class,
    TInputOutputSpecification.class,
    TResourceRole.class,
    TDataAssociation.class,
    TCorrelationPropertyRetrievalExpression.class,
    TLane.class,
    TParticipantMultiplicity.class,
    TAssignment.class,
    TResourceParameterBinding.class,
    TRootElement.class,
    TCorrelationKey.class,
    TFlowElement.class
})
public abstract class TBaseElement {

    protected List<TDocumentation> documentation;
    protected TExtensionElements extensionElements;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

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
            documentation = new ArrayList<TDocumentation>();
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
