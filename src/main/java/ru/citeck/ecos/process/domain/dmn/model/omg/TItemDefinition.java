
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tItemDefinition complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tItemDefinition">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tNamedElement">
 *       &lt;choice>
 *         &lt;sequence>
 *           &lt;element name="typeRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="allowedValues" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tUnaryTests" minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;element name="itemComponent" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tItemDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="functionItem" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tFunctionItem" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attribute name="typeLanguage" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="isCollection" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tItemDefinition", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "typeRef",
    "allowedValues",
    "itemComponent",
    "functionItem"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TItemDefinition
    extends TNamedElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String typeRef;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TUnaryTests allowedValues;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TItemDefinition> itemComponent;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TFunctionItem functionItem;
    @XmlAttribute(name = "typeLanguage")
    @XmlSchemaType(name = "anyURI")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String typeLanguage;
    @XmlAttribute(name = "isCollection")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean isCollection;

    /**
     * Gets the value of the typeRef property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getTypeRef() {
        return typeRef;
    }

    /**
     * Sets the value of the typeRef property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTypeRef(String value) {
        this.typeRef = value;
    }

    /**
     * Gets the value of the allowedValues property.
     *
     * @return
     *     possible object is
     *     {@link TUnaryTests }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TUnaryTests getAllowedValues() {
        return allowedValues;
    }

    /**
     * Sets the value of the allowedValues property.
     *
     * @param value
     *     allowed object is
     *     {@link TUnaryTests }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setAllowedValues(TUnaryTests value) {
        this.allowedValues = value;
    }

    /**
     * Gets the value of the itemComponent property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the itemComponent property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItemComponent().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TItemDefinition }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TItemDefinition> getItemComponent() {
        if (itemComponent == null) {
            itemComponent = new ArrayList<TItemDefinition>();
        }
        return this.itemComponent;
    }

    /**
     * Gets the value of the functionItem property.
     *
     * @return
     *     possible object is
     *     {@link TFunctionItem }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TFunctionItem getFunctionItem() {
        return functionItem;
    }

    /**
     * Sets the value of the functionItem property.
     *
     * @param value
     *     allowed object is
     *     {@link TFunctionItem }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFunctionItem(TFunctionItem value) {
        this.functionItem = value;
    }

    /**
     * Gets the value of the typeLanguage property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getTypeLanguage() {
        return typeLanguage;
    }

    /**
     * Sets the value of the typeLanguage property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTypeLanguage(String value) {
        this.typeLanguage = value;
    }

    /**
     * Gets the value of the isCollection property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public boolean isIsCollection() {
        if (isCollection == null) {
            return false;
        } else {
            return isCollection;
        }
    }

    /**
     * Sets the value of the isCollection property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setIsCollection(Boolean value) {
        this.isCollection = value;
    }

}
