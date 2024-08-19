
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tContextEntry complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tContextEntry">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElement">
 *       &lt;sequence>
 *         &lt;element name="variable" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tInformationItem" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}expression"/>
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
@XmlType(name = "tContextEntry", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "variable",
    "expression"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TContextEntry
    extends TDMNElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TInformationItem variable;
    @XmlElementRef(name = "expression", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", type = JAXBElement.class)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected JAXBElement<? extends TExpression> expression;

    /**
     * Gets the value of the variable property.
     *
     * @return
     *     possible object is
     *     {@link TInformationItem }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TInformationItem getVariable() {
        return variable;
    }

    /**
     * Sets the value of the variable property.
     *
     * @param value
     *     allowed object is
     *     {@link TInformationItem }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setVariable(TInformationItem value) {
        this.variable = value;
    }

    /**
     * Gets the value of the expression property.
     *
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link TDecisionTable }{@code >}
     *     {@link JAXBElement }{@code <}{@link TRelation }{@code >}
     *     {@link JAXBElement }{@code <}{@link TList }{@code >}
     *     {@link JAXBElement }{@code <}{@link TFunctionDefinition }{@code >}
     *     {@link JAXBElement }{@code <}{@link TContext }{@code >}
     *     {@link JAXBElement }{@code <}{@link TLiteralExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TInvocation }{@code >}
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public JAXBElement<? extends TExpression> getExpression() {
        return expression;
    }

    /**
     * Sets the value of the expression property.
     *
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link TDecisionTable }{@code >}
     *     {@link JAXBElement }{@code <}{@link TRelation }{@code >}
     *     {@link JAXBElement }{@code <}{@link TList }{@code >}
     *     {@link JAXBElement }{@code <}{@link TFunctionDefinition }{@code >}
     *     {@link JAXBElement }{@code <}{@link TContext }{@code >}
     *     {@link JAXBElement }{@code <}{@link TLiteralExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TInvocation }{@code >}
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setExpression(JAXBElement<? extends TExpression> value) {
        this.expression = value;
    }

}
