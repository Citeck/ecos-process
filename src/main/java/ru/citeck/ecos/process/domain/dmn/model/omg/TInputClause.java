
package ru.citeck.ecos.process.domain.dmn.model.omg;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tInputClause complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tInputClause">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElement">
 *       &lt;sequence>
 *         &lt;element name="inputExpression" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tLiteralExpression"/>
 *         &lt;element name="inputValues" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tUnaryTests" minOccurs="0"/>
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
@XmlType(name = "tInputClause", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "inputExpression",
    "inputValues"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TInputClause
    extends TDMNElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TLiteralExpression inputExpression;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TUnaryTests inputValues;

    /**
     * Gets the value of the inputExpression property.
     *
     * @return
     *     possible object is
     *     {@link TLiteralExpression }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TLiteralExpression getInputExpression() {
        return inputExpression;
    }

    /**
     * Sets the value of the inputExpression property.
     *
     * @param value
     *     allowed object is
     *     {@link TLiteralExpression }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setInputExpression(TLiteralExpression value) {
        this.inputExpression = value;
    }

    /**
     * Gets the value of the inputValues property.
     *
     * @return
     *     possible object is
     *     {@link TUnaryTests }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TUnaryTests getInputValues() {
        return inputValues;
    }

    /**
     * Sets the value of the inputValues property.
     *
     * @param value
     *     allowed object is
     *     {@link TUnaryTests }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setInputValues(TUnaryTests value) {
        this.inputValues = value;
    }

}
