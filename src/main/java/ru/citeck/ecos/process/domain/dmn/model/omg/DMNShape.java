
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for DMNShape complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DMNShape">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/DMN/20180521/DI/}Shape">
 *       &lt;sequence>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNLabel" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNDecisionServiceDividerLine" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="dmnElementRef" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="isListedInputData" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isCollapsed" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DMNShape", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/", propOrder = {
    "dmnLabel",
    "dmnDecisionServiceDividerLine"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class DMNShape
    extends Shape
{

    @XmlElement(name = "DMNLabel", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected DMNLabel dmnLabel;
    @XmlElement(name = "DMNDecisionServiceDividerLine", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected DMNDecisionServiceDividerLine dmnDecisionServiceDividerLine;
    @XmlAttribute(name = "dmnElementRef", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected QName dmnElementRef;
    @XmlAttribute(name = "isListedInputData")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean isListedInputData;
    @XmlAttribute(name = "isCollapsed")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean isCollapsed;

    /**
     * Gets the value of the dmnLabel property.
     *
     * @return
     *     possible object is
     *     {@link DMNLabel }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public DMNLabel getDMNLabel() {
        return dmnLabel;
    }

    /**
     * Sets the value of the dmnLabel property.
     *
     * @param value
     *     allowed object is
     *     {@link DMNLabel }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDMNLabel(DMNLabel value) {
        this.dmnLabel = value;
    }

    /**
     * Gets the value of the dmnDecisionServiceDividerLine property.
     *
     * @return
     *     possible object is
     *     {@link DMNDecisionServiceDividerLine }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public DMNDecisionServiceDividerLine getDMNDecisionServiceDividerLine() {
        return dmnDecisionServiceDividerLine;
    }

    /**
     * Sets the value of the dmnDecisionServiceDividerLine property.
     *
     * @param value
     *     allowed object is
     *     {@link DMNDecisionServiceDividerLine }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDMNDecisionServiceDividerLine(DMNDecisionServiceDividerLine value) {
        this.dmnDecisionServiceDividerLine = value;
    }

    /**
     * Gets the value of the dmnElementRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public QName getDmnElementRef() {
        return dmnElementRef;
    }

    /**
     * Sets the value of the dmnElementRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDmnElementRef(QName value) {
        this.dmnElementRef = value;
    }

    /**
     * Gets the value of the isListedInputData property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Boolean isIsListedInputData() {
        return isListedInputData;
    }

    /**
     * Sets the value of the isListedInputData property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setIsListedInputData(Boolean value) {
        this.isListedInputData = value;
    }

    /**
     * Gets the value of the isCollapsed property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public boolean isIsCollapsed() {
        if (isCollapsed == null) {
            return false;
        } else {
            return isCollapsed;
        }
    }

    /**
     * Sets the value of the isCollapsed property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setIsCollapsed(Boolean value) {
        this.isCollapsed = value;
    }

}
