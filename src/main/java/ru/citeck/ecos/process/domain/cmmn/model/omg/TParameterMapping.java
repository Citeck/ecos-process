
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tParameterMapping complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tParameterMapping">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element name="transformation" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tExpression" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="sourceRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="targetRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
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
@XmlType(name = "tParameterMapping", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "transformation"
})
public class TParameterMapping
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExpression transformation;
    @XmlAttribute(name = "sourceRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object sourceRef;
    @XmlAttribute(name = "targetRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object targetRef;

    /**
     * Gets the value of the transformation property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getTransformation() {
        return transformation;
    }

    /**
     * Sets the value of the transformation property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setTransformation(TExpression value) {
        this.transformation = value;
    }

    /**
     * Gets the value of the sourceRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getSourceRef() {
        return sourceRef;
    }

    /**
     * Sets the value of the sourceRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setSourceRef(Object value) {
        this.sourceRef = value;
    }

    /**
     * Gets the value of the targetRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getTargetRef() {
        return targetRef;
    }

    /**
     * Sets the value of the targetRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setTargetRef(Object value) {
        this.targetRef = value;
    }

}
