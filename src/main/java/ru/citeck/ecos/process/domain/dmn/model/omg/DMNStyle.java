
package ru.citeck.ecos.process.domain.dmn.model.omg;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DMNStyle complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DMNStyle">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/DMN/20180521/DI/}Style">
 *       &lt;sequence>
 *         &lt;element name="FillColor" type="{http://www.omg.org/spec/DMN/20180521/DC/}Color" minOccurs="0"/>
 *         &lt;element name="StrokeColor" type="{http://www.omg.org/spec/DMN/20180521/DC/}Color" minOccurs="0"/>
 *         &lt;element name="FontColor" type="{http://www.omg.org/spec/DMN/20180521/DC/}Color" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="fontFamily" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fontSize" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="fontItalic" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="fontBold" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="fontUnderline" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="fontStrikeThrough" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="labelHorizontalAlignement" type="{http://www.omg.org/spec/DMN/20180521/DC/}AlignmentKind" />
 *       &lt;attribute name="labelVerticalAlignment" type="{http://www.omg.org/spec/DMN/20180521/DC/}AlignmentKind" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DMNStyle", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/", propOrder = {
    "fillColor",
    "strokeColor",
    "fontColor"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class DMNStyle
    extends Style
{

    @XmlElement(name = "FillColor", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Color fillColor;
    @XmlElement(name = "StrokeColor", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Color strokeColor;
    @XmlElement(name = "FontColor", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Color fontColor;
    @XmlAttribute(name = "fontFamily")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String fontFamily;
    @XmlAttribute(name = "fontSize")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Double fontSize;
    @XmlAttribute(name = "fontItalic")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean fontItalic;
    @XmlAttribute(name = "fontBold")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean fontBold;
    @XmlAttribute(name = "fontUnderline")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean fontUnderline;
    @XmlAttribute(name = "fontStrikeThrough")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Boolean fontStrikeThrough;
    @XmlAttribute(name = "labelHorizontalAlignement")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected AlignmentKind labelHorizontalAlignement;
    @XmlAttribute(name = "labelVerticalAlignment")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected AlignmentKind labelVerticalAlignment;

    /**
     * Gets the value of the fillColor property.
     *
     * @return
     *     possible object is
     *     {@link Color }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * Sets the value of the fillColor property.
     *
     * @param value
     *     allowed object is
     *     {@link Color }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFillColor(Color value) {
        this.fillColor = value;
    }

    /**
     * Gets the value of the strokeColor property.
     *
     * @return
     *     possible object is
     *     {@link Color }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Color getStrokeColor() {
        return strokeColor;
    }

    /**
     * Sets the value of the strokeColor property.
     *
     * @param value
     *     allowed object is
     *     {@link Color }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setStrokeColor(Color value) {
        this.strokeColor = value;
    }

    /**
     * Gets the value of the fontColor property.
     *
     * @return
     *     possible object is
     *     {@link Color }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Color getFontColor() {
        return fontColor;
    }

    /**
     * Sets the value of the fontColor property.
     *
     * @param value
     *     allowed object is
     *     {@link Color }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontColor(Color value) {
        this.fontColor = value;
    }

    /**
     * Gets the value of the fontFamily property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getFontFamily() {
        return fontFamily;
    }

    /**
     * Sets the value of the fontFamily property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontFamily(String value) {
        this.fontFamily = value;
    }

    /**
     * Gets the value of the fontSize property.
     *
     * @return
     *     possible object is
     *     {@link Double }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Double getFontSize() {
        return fontSize;
    }

    /**
     * Sets the value of the fontSize property.
     *
     * @param value
     *     allowed object is
     *     {@link Double }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontSize(Double value) {
        this.fontSize = value;
    }

    /**
     * Gets the value of the fontItalic property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Boolean isFontItalic() {
        return fontItalic;
    }

    /**
     * Sets the value of the fontItalic property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontItalic(Boolean value) {
        this.fontItalic = value;
    }

    /**
     * Gets the value of the fontBold property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Boolean isFontBold() {
        return fontBold;
    }

    /**
     * Sets the value of the fontBold property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontBold(Boolean value) {
        this.fontBold = value;
    }

    /**
     * Gets the value of the fontUnderline property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Boolean isFontUnderline() {
        return fontUnderline;
    }

    /**
     * Sets the value of the fontUnderline property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontUnderline(Boolean value) {
        this.fontUnderline = value;
    }

    /**
     * Gets the value of the fontStrikeThrough property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Boolean isFontStrikeThrough() {
        return fontStrikeThrough;
    }

    /**
     * Sets the value of the fontStrikeThrough property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFontStrikeThrough(Boolean value) {
        this.fontStrikeThrough = value;
    }

    /**
     * Gets the value of the labelHorizontalAlignement property.
     *
     * @return
     *     possible object is
     *     {@link AlignmentKind }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public AlignmentKind getLabelHorizontalAlignement() {
        return labelHorizontalAlignement;
    }

    /**
     * Sets the value of the labelHorizontalAlignement property.
     *
     * @param value
     *     allowed object is
     *     {@link AlignmentKind }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLabelHorizontalAlignement(AlignmentKind value) {
        this.labelHorizontalAlignement = value;
    }

    /**
     * Gets the value of the labelVerticalAlignment property.
     *
     * @return
     *     possible object is
     *     {@link AlignmentKind }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public AlignmentKind getLabelVerticalAlignment() {
        return labelVerticalAlignment;
    }

    /**
     * Sets the value of the labelVerticalAlignment property.
     *
     * @param value
     *     allowed object is
     *     {@link AlignmentKind }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setLabelVerticalAlignment(AlignmentKind value) {
        this.labelVerticalAlignment = value;
    }

}
