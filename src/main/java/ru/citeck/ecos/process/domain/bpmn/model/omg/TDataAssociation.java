//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDataAssociation complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDataAssociation">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tBaseElement">
 *       &lt;sequence>
 *         &lt;element name="sourceRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="targetRef" type="{http://www.w3.org/2001/XMLSchema}IDREF"/>
 *         &lt;element name="transformation" type="{http://www.omg.org/spec/BPMN/20100524/MODEL}tFormalExpression" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/BPMN/20100524/MODEL}assignment" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tDataAssociation", propOrder = {
    "sourceRef",
    "targetRef",
    "transformation",
    "assignment"
})
@XmlSeeAlso({
    TDataInputAssociation.class,
    TDataOutputAssociation.class
})
public class TDataAssociation
    extends TBaseElement
{

    @XmlElementRef(name = "sourceRef", namespace = "http://www.omg.org/spec/BPMN/20100524/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<Object>> sourceRef;
    @XmlElement(required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object targetRef;
    protected TFormalExpression transformation;
    protected List<TAssignment> assignment;

    /**
     * Gets the value of the sourceRef property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sourceRef property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSourceRef().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     *
     */
    public List<JAXBElement<Object>> getSourceRef() {
        if (sourceRef == null) {
            sourceRef = new ArrayList<JAXBElement<Object>>();
        }
        return this.sourceRef;
    }

    /**
     * Gets the value of the targetRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getTargetRef() {
        return targetRef;
    }

    /**
     * Sets the value of the targetRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setTargetRef(Object value) {
        this.targetRef = value;
    }

    /**
     * Gets the value of the transformation property.
     *
     * @return
     *     possible object is
     *     {@link TFormalExpression }
     *
     */
    public TFormalExpression getTransformation() {
        return transformation;
    }

    /**
     * Sets the value of the transformation property.
     *
     * @param value
     *     allowed object is
     *     {@link TFormalExpression }
     *
     */
    public void setTransformation(TFormalExpression value) {
        this.transformation = value;
    }

    /**
     * Gets the value of the assignment property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the assignment property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAssignment().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TAssignment }
     *
     *
     */
    public List<TAssignment> getAssignment() {
        if (assignment == null) {
            assignment = new ArrayList<TAssignment>();
        }
        return this.assignment;
    }

}
