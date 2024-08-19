
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Generated;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tList complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tList">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tExpression">
 *       &lt;sequence>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}expression" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tList", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "expression"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TList
    extends TExpression
{

    @XmlElementRef(name = "expression", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", type = JAXBElement.class, required = false)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<JAXBElement<? extends TExpression>> expression;

    /**
     * Gets the value of the expression property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the expression property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExpression().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TDecisionTable }{@code >}
     * {@link JAXBElement }{@code <}{@link TRelation }{@code >}
     * {@link JAXBElement }{@code <}{@link TList }{@code >}
     * {@link JAXBElement }{@code <}{@link TFunctionDefinition }{@code >}
     * {@link JAXBElement }{@code <}{@link TContext }{@code >}
     * {@link JAXBElement }{@code <}{@link TLiteralExpression }{@code >}
     * {@link JAXBElement }{@code <}{@link TExpression }{@code >}
     * {@link JAXBElement }{@code <}{@link TInvocation }{@code >}
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<JAXBElement<? extends TExpression>> getExpression() {
        if (expression == null) {
            expression = new ArrayList<JAXBElement<? extends TExpression>>();
        }
        return this.expression;
    }

}
