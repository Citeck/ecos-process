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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tOutputSet complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tOutputSet">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tBaseElement">
 *       &lt;sequence>
 *         &lt;element name="dataOutputRefs" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="optionalOutputRefs" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="whileExecutingOutputRefs" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="inputSetRefs" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tOutputSet", propOrder = {
    "dataOutputRefs",
    "optionalOutputRefs",
    "whileExecutingOutputRefs",
    "inputSetRefs"
})
public class TOutputSet
    extends TBaseElement
{

    @XmlElementRef(name = "dataOutputRefs", namespace = "http://www.omg.org/spec/BPMN/20100524/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<Object>> dataOutputRefs;
    @XmlElementRef(name = "optionalOutputRefs", namespace = "http://www.omg.org/spec/BPMN/20100524/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<Object>> optionalOutputRefs;
    @XmlElementRef(name = "whileExecutingOutputRefs", namespace = "http://www.omg.org/spec/BPMN/20100524/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<Object>> whileExecutingOutputRefs;
    @XmlElementRef(name = "inputSetRefs", namespace = "http://www.omg.org/spec/BPMN/20100524/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<Object>> inputSetRefs;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the dataOutputRefs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataOutputRefs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDataOutputRefs().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     *
     */
    public List<JAXBElement<Object>> getDataOutputRefs() {
        if (dataOutputRefs == null) {
            dataOutputRefs = new ArrayList<JAXBElement<Object>>();
        }
        return this.dataOutputRefs;
    }

    /**
     * Gets the value of the optionalOutputRefs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the optionalOutputRefs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOptionalOutputRefs().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     *
     */
    public List<JAXBElement<Object>> getOptionalOutputRefs() {
        if (optionalOutputRefs == null) {
            optionalOutputRefs = new ArrayList<JAXBElement<Object>>();
        }
        return this.optionalOutputRefs;
    }

    /**
     * Gets the value of the whileExecutingOutputRefs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the whileExecutingOutputRefs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWhileExecutingOutputRefs().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     *
     */
    public List<JAXBElement<Object>> getWhileExecutingOutputRefs() {
        if (whileExecutingOutputRefs == null) {
            whileExecutingOutputRefs = new ArrayList<JAXBElement<Object>>();
        }
        return this.whileExecutingOutputRefs;
    }

    /**
     * Gets the value of the inputSetRefs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputSetRefs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputSetRefs().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     *
     *
     */
    public List<JAXBElement<Object>> getInputSetRefs() {
        if (inputSetRefs == null) {
            inputSetRefs = new ArrayList<JAXBElement<Object>>();
        }
        return this.inputSetRefs;
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
