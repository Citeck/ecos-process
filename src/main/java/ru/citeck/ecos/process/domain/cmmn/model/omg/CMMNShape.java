
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.*;
import javax.xml.namespace.QName;


/**
 * <p>Java class for CMMNShape complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CMMNShape">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/DI}Shape">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/CMMNDI}CMMNLabel"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cmmnElementRef" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="isCollapsed" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="isPlanningTableCollapsed" type="{http://www.w3.org/2001/XMLSchema}boolean" />
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
@XmlType(name = "CMMNShape", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", propOrder = {
    "cmmnLabel"
})
public class CMMNShape
    extends Shape
{

    @XmlElement(name = "CMMNLabel", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", required = true)
    protected CMMNLabel cmmnLabel;
    @XmlAttribute(name = "cmmnElementRef", required = true)
    protected QName cmmnElementRef;
    @XmlAttribute(name = "isCollapsed")
    protected Boolean isCollapsed;
    @XmlAttribute(name = "isPlanningTableCollapsed")
    protected Boolean isPlanningTableCollapsed;

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
     * Gets the value of the isCollapsed property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public Boolean isIsCollapsed() {
        return isCollapsed;
    }

    /**
     * Sets the value of the isCollapsed property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setIsCollapsed(Boolean value) {
        this.isCollapsed = value;
    }

    /**
     * Gets the value of the isPlanningTableCollapsed property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public Boolean isIsPlanningTableCollapsed() {
        return isPlanningTableCollapsed;
    }

    /**
     * Sets the value of the isPlanningTableCollapsed property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setIsPlanningTableCollapsed(Boolean value) {
        this.isPlanningTableCollapsed = value;
    }

}
