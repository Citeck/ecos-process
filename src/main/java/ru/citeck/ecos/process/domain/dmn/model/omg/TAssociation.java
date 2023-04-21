
package ru.citeck.ecos.process.domain.dmn.model.omg;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tAssociation complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tAssociation">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tArtifact">
 *       &lt;sequence>
 *         &lt;element name="sourceRef" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference"/>
 *         &lt;element name="targetRef" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference"/>
 *       &lt;/sequence>
 *       &lt;attribute name="associationDirection" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tAssociationDirection" default="None" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tAssociation", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "sourceRef",
    "targetRef"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TAssociation
    extends TArtifact
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDMNElementReference sourceRef;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDMNElementReference targetRef;
    @XmlAttribute(name = "associationDirection")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TAssociationDirection associationDirection;

    /**
     * Gets the value of the sourceRef property.
     *
     * @return
     *     possible object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDMNElementReference getSourceRef() {
        return sourceRef;
    }

    /**
     * Sets the value of the sourceRef property.
     *
     * @param value
     *     allowed object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setSourceRef(TDMNElementReference value) {
        this.sourceRef = value;
    }

    /**
     * Gets the value of the targetRef property.
     *
     * @return
     *     possible object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDMNElementReference getTargetRef() {
        return targetRef;
    }

    /**
     * Sets the value of the targetRef property.
     *
     * @param value
     *     allowed object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTargetRef(TDMNElementReference value) {
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
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TAssociationDirection getAssociationDirection() {
        if (associationDirection == null) {
            return TAssociationDirection.NONE;
        } else {
            return associationDirection;
        }
    }

    /**
     * Sets the value of the associationDirection property.
     *
     * @param value
     *     allowed object is
     *     {@link TAssociationDirection }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setAssociationDirection(TAssociationDirection value) {
        this.associationDirection = value;
    }

}
