
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tOutputClause complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tOutputClause">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElement">
 *       &lt;sequence>
 *         &lt;element name="outputValues" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tUnaryTests" minOccurs="0"/>
 *         &lt;element name="defaultOutputEntry" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tLiteralExpression" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="typeRef" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tOutputClause", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "outputValues",
    "defaultOutputEntry"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TOutputClause
    extends TDMNElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TUnaryTests outputValues;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TLiteralExpression defaultOutputEntry;
    @XmlAttribute(name = "name")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String name;
    @XmlAttribute(name = "typeRef")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String typeRef;

    /**
     * Gets the value of the outputValues property.
     *
     * @return
     *     possible object is
     *     {@link TUnaryTests }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TUnaryTests getOutputValues() {
        return outputValues;
    }

    /**
     * Sets the value of the outputValues property.
     *
     * @param value
     *     allowed object is
     *     {@link TUnaryTests }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setOutputValues(TUnaryTests value) {
        this.outputValues = value;
    }

    /**
     * Gets the value of the defaultOutputEntry property.
     *
     * @return
     *     possible object is
     *     {@link TLiteralExpression }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TLiteralExpression getDefaultOutputEntry() {
        return defaultOutputEntry;
    }

    /**
     * Sets the value of the defaultOutputEntry property.
     *
     * @param value
     *     allowed object is
     *     {@link TLiteralExpression }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDefaultOutputEntry(TLiteralExpression value) {
        this.defaultOutputEntry = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
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
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setName(String value) {
        this.name = value;
    }

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

}
