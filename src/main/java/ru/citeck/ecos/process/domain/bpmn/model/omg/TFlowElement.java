//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for tFlowElement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tFlowElement">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tBaseElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/BPMN/20100524/MODEL}auditing" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/BPMN/20100524/MODEL}monitoring" minOccurs="0"/>
 *         &lt;element name="categoryValueRef" type="{http://www.w3.org/2001/XMLSchema}QName" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tFlowElement", propOrder = {
    "auditing",
    "monitoring",
    "categoryValueRef"
})
@XmlSeeAlso({
    TSequenceFlow.class,
    TDataStoreReference.class,
    TDataObject.class,
    TDataObjectReference.class,
    TFlowNode.class
})
public abstract class TFlowElement
    extends TBaseElement
{

    protected TAuditing auditing;
    protected TMonitoring monitoring;
    protected List<QName> categoryValueRef;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the auditing property.
     *
     * @return
     *     possible object is
     *     {@link TAuditing }
     *
     */
    public TAuditing getAuditing() {
        return auditing;
    }

    /**
     * Sets the value of the auditing property.
     *
     * @param value
     *     allowed object is
     *     {@link TAuditing }
     *
     */
    public void setAuditing(TAuditing value) {
        this.auditing = value;
    }

    /**
     * Gets the value of the monitoring property.
     *
     * @return
     *     possible object is
     *     {@link TMonitoring }
     *
     */
    public TMonitoring getMonitoring() {
        return monitoring;
    }

    /**
     * Sets the value of the monitoring property.
     *
     * @param value
     *     allowed object is
     *     {@link TMonitoring }
     *
     */
    public void setMonitoring(TMonitoring value) {
        this.monitoring = value;
    }

    /**
     * Gets the value of the categoryValueRef property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the categoryValueRef property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCategoryValueRef().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link QName }
     *
     *
     */
    public List<QName> getCategoryValueRef() {
        if (categoryValueRef == null) {
            categoryValueRef = new ArrayList<QName>();
        }
        return this.categoryValueRef;
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

}
