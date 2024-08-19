
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tAuthorityRequirement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tAuthorityRequirement">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElement">
 *       &lt;choice>
 *         &lt;element name="requiredDecision" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference"/>
 *         &lt;element name="requiredInput" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference"/>
 *         &lt;element name="requiredAuthority" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference"/>
 *       &lt;/choice>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tAuthorityRequirement", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "requiredDecision",
    "requiredInput",
    "requiredAuthority"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TAuthorityRequirement
    extends TDMNElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDMNElementReference requiredDecision;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDMNElementReference requiredInput;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDMNElementReference requiredAuthority;

    /**
     * Gets the value of the requiredDecision property.
     *
     * @return
     *     possible object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDMNElementReference getRequiredDecision() {
        return requiredDecision;
    }

    /**
     * Sets the value of the requiredDecision property.
     *
     * @param value
     *     allowed object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRequiredDecision(TDMNElementReference value) {
        this.requiredDecision = value;
    }

    /**
     * Gets the value of the requiredInput property.
     *
     * @return
     *     possible object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDMNElementReference getRequiredInput() {
        return requiredInput;
    }

    /**
     * Sets the value of the requiredInput property.
     *
     * @param value
     *     allowed object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRequiredInput(TDMNElementReference value) {
        this.requiredInput = value;
    }

    /**
     * Gets the value of the requiredAuthority property.
     *
     * @return
     *     possible object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDMNElementReference getRequiredAuthority() {
        return requiredAuthority;
    }

    /**
     * Sets the value of the requiredAuthority property.
     *
     * @param value
     *     allowed object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRequiredAuthority(TDMNElementReference value) {
        this.requiredAuthority = value;
    }

}
