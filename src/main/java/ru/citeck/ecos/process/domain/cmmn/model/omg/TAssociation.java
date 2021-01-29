
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tAssociation complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tAssociation">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tArtifact">
 *       &lt;attribute name="sourceRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="targetRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="associationDirection" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tAssociationDirection" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tAssociation", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
public class TAssociation
    extends TArtifact
{

    @XmlAttribute(name = "sourceRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object sourceRef;
    @XmlAttribute(name = "targetRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object targetRef;
    @XmlAttribute(name = "associationDirection")
    protected TAssociationDirection associationDirection;

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

    /**
     * Gets the value of the associationDirection property.
     *
     * @return
     *     possible object is
     *     {@link TAssociationDirection }
     *
     */
    public TAssociationDirection getAssociationDirection() {
        return associationDirection;
    }

    /**
     * Sets the value of the associationDirection property.
     *
     * @param value
     *     allowed object is
     *     {@link TAssociationDirection }
     *
     */
    public void setAssociationDirection(TAssociationDirection value) {
        this.associationDirection = value;
    }

}
