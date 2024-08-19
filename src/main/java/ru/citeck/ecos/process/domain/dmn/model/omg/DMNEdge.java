
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for DMNEdge complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DMNEdge">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/DMN/20180521/DI/}Edge">
 *       &lt;sequence>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNLabel" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="dmnElementRef" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="sourceElement" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="targetElement" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DMNEdge", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/", propOrder = {
    "dmnLabel"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class DMNEdge
    extends Edge
{

    @XmlElement(name = "DMNLabel", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected DMNLabel dmnLabel;
    @XmlAttribute(name = "dmnElementRef", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected QName dmnElementRef;
    @XmlAttribute(name = "sourceElement")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected QName sourceElement;
    @XmlAttribute(name = "targetElement")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected QName targetElement;

    /**
     * Gets the value of the dmnLabel property.
     *
     * @return
     *     possible object is
     *     {@link DMNLabel }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public DMNLabel getDMNLabel() {
        return dmnLabel;
    }

    /**
     * Sets the value of the dmnLabel property.
     *
     * @param value
     *     allowed object is
     *     {@link DMNLabel }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDMNLabel(DMNLabel value) {
        this.dmnLabel = value;
    }

    /**
     * Gets the value of the dmnElementRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public QName getDmnElementRef() {
        return dmnElementRef;
    }

    /**
     * Sets the value of the dmnElementRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDmnElementRef(QName value) {
        this.dmnElementRef = value;
    }

    /**
     * Gets the value of the sourceElement property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public QName getSourceElement() {
        return sourceElement;
    }

    /**
     * Sets the value of the sourceElement property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setSourceElement(QName value) {
        this.sourceElement = value;
    }

    /**
     * Gets the value of the targetElement property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public QName getTargetElement() {
        return targetElement;
    }

    /**
     * Sets the value of the targetElement property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTargetElement(QName value) {
        this.targetElement = value;
    }

}
