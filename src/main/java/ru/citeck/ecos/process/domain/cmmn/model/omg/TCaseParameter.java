
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tCaseParameter complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCaseParameter">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tParameter">
 *       &lt;sequence>
 *         &lt;element name="bindingRefinement" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tExpression" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="bindingRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCaseParameter", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "bindingRefinement"
})
public class TCaseParameter
    extends TParameter
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExpression bindingRefinement;
    @XmlAttribute(name = "bindingRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object bindingRef;

    /**
     * Gets the value of the bindingRefinement property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getBindingRefinement() {
        return bindingRefinement;
    }

    /**
     * Sets the value of the bindingRefinement property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setBindingRefinement(TExpression value) {
        this.bindingRefinement = value;
    }

    /**
     * Gets the value of the bindingRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getBindingRef() {
        return bindingRef;
    }

    /**
     * Sets the value of the bindingRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setBindingRef(Object value) {
        this.bindingRef = value;
    }

}
