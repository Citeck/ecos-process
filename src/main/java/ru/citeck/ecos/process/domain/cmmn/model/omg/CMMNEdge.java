
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;


/**
 * <p>Java class for CMMNEdge complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CMMNEdge">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/DI}Edge">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/CMMNDI}CMMNLabel"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cmmnElementRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="sourceCMMNElementRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="targetCMMNElementRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="isStandardEventVisible" type="{http://www.w3.org/2001/XMLSchema}boolean" />
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
@XmlType(name = "CMMNEdge", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", propOrder = {
    "cmmnLabel"
})
public class CMMNEdge
    extends Edge
{

    @XmlElement(name = "CMMNLabel", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", required = true)
    protected CMMNLabel cmmnLabel;
    @XmlAttribute(name = "cmmnElementRef")
    protected QName cmmnElementRef;
    @XmlAttribute(name = "sourceCMMNElementRef")
    protected QName sourceCMMNElementRef;
    @XmlAttribute(name = "targetCMMNElementRef")
    protected QName targetCMMNElementRef;
    @XmlAttribute(name = "isStandardEventVisible")
    protected Boolean isStandardEventVisible;

    /**
     * Gets the value of the cmmnLabel property.
     *
     * @return
     *     possible object is
     *     {@link CMMNLabel }
     *
     */
    public CMMNLabel getCMMNLabel() {
        return cmmnLabel;
    }

    /**
     * Sets the value of the cmmnLabel property.
     *
     * @param value
     *     allowed object is
     *     {@link CMMNLabel }
     *
     */
    public void setCMMNLabel(CMMNLabel value) {
        this.cmmnLabel = value;
    }

    /**
     * Gets the value of the cmmnElementRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getCmmnElementRef() {
        return cmmnElementRef;
    }

    /**
     * Sets the value of the cmmnElementRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setCmmnElementRef(QName value) {
        this.cmmnElementRef = value;
    }

    /**
     * Gets the value of the sourceCMMNElementRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getSourceCMMNElementRef() {
        return sourceCMMNElementRef;
    }

    /**
     * Sets the value of the sourceCMMNElementRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setSourceCMMNElementRef(QName value) {
        this.sourceCMMNElementRef = value;
    }

    /**
     * Gets the value of the targetCMMNElementRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getTargetCMMNElementRef() {
        return targetCMMNElementRef;
    }

    /**
     * Sets the value of the targetCMMNElementRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setTargetCMMNElementRef(QName value) {
        this.targetCMMNElementRef = value;
    }

    /**
     * Gets the value of the isStandardEventVisible property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public Boolean isIsStandardEventVisible() {
        return isStandardEventVisible;
    }

    /**
     * Sets the value of the isStandardEventVisible property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setIsStandardEventVisible(Boolean value) {
        this.isStandardEventVisible = value;
    }

}
