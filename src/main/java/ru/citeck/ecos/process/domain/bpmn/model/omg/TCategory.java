//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00 
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tCategory complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tCategory">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/BPMN/20100524/MODEL}tRootElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/BPMN/20100524/MODEL}categoryValue" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tCategory", propOrder = {
    "categoryValue"
})
public class TCategory
    extends TRootElement
{

    protected List<TCategoryValue> categoryValue;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the categoryValue property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the categoryValue property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCategoryValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TCategoryValue }
     * 
     * 
     */
    public List<TCategoryValue> getCategoryValue() {
        if (categoryValue == null) {
            categoryValue = new ArrayList<TCategoryValue>();
        }
        return this.categoryValue;
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
