
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.*;


/**
 *
 *         tRepetitionRule defines the type of element "repetitionRule".
 *
 *
 * <p>Java class for tRepetitionRule complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tRepetitionRule">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element name="condition" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tExpression" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="contextRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tRepetitionRule", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "condition"
})
public class TRepetitionRule
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExpression condition;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "contextRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object contextRef;

    /**
     * Gets the value of the condition property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getCondition() {
        return condition;
    }

    /**
     * Sets the value of the condition property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setCondition(TExpression value) {
        this.condition = value;
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

    /**
     * Gets the value of the contextRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getContextRef() {
        return contextRef;
    }

    /**
     * Sets the value of the contextRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setContextRef(Object value) {
        this.contextRef = value;
    }

}
