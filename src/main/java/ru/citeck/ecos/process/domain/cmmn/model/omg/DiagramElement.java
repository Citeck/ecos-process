
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.w3c.dom.Element;
import ru.citeck.ecos.process.domain.procdef.convert.io.xml.XmlDefUtils;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DiagramElement is the abstract super type of all elements in diagrams, including diagrams themselves. When contained in a diagram, diagram elements are laid out relative to the diagram's origin.
 *
 * <p>Java class for DiagramElement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DiagramElement">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="extension" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/DI}Style" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="sharedStyle" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiagramElement", namespace = "http://www.omg.org/spec/CMMN/20151109/DI", propOrder = {
    "extension",
    "style"
})
@XmlSeeAlso({
    Shape.class,
    Edge.class,
    Diagram.class
})
public abstract class DiagramElement {

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/DI")
    protected DiagramElement.Extension extension;
    @XmlElementRef(name = "Style", namespace = "http://www.omg.org/spec/CMMN/20151109/DI", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends Style> style;
    @XmlAttribute(name = "sharedStyle")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object sharedStyle;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<>();

    /**
     * Gets the value of the extension property.
     *
     * @return
     *     possible object is
     *     {@link DiagramElement.Extension }
     *
     */
    public DiagramElement.Extension getExtension() {
        return extension;
    }

    /**
     * Sets the value of the extension property.
     *
     * @param value
     *     allowed object is
     *     {@link DiagramElement.Extension }
     *
     */
    public void setExtension(DiagramElement.Extension value) {
        this.extension = value;
    }

    /**
     * an optional locally-owned style for this diagram element.
     *
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Style }{@code >}
     *     {@link JAXBElement }{@code <}{@link CMMNStyle }{@code >}
     *
     */
    public JAXBElement<? extends Style> getStyle() {
        return style;
    }

    /**
     * Sets the value of the style property.
     *
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Style }{@code >}
     *     {@link JAXBElement }{@code <}{@link CMMNStyle }{@code >}
     *
     */
    public void setStyle(JAXBElement<? extends Style> value) {
        this.style = value;
    }

    /**
     * Gets the value of the sharedStyle property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getSharedStyle() {
        return sharedStyle;
    }

    /**
     * Sets the value of the sharedStyle property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setSharedStyle(Object value) {
        this.sharedStyle = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     *
     * <p>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     *
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     *
     *
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "any"
    })
    public static class Extension {

        @XmlAnyElement(lax = true)
        protected List<Object> any;

        /**
         * Gets the value of the any property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the any property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getAny().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Object }
         * {@link Element }
         *
         *
         */
        public List<Object> getAny() {
            if (any == null) {
                any = new ArrayList<>();
            }
            return this.any;
        }

    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DiagramElement that = (DiagramElement) o;

        return new EqualsBuilder()
            .append(extension, that.extension)
            .append(XmlDefUtils.unwrapJaxb(style), XmlDefUtils.unwrapJaxb(that.style))
            .append(sharedStyle, that.sharedStyle)
            .append(id, that.id)
            .append(otherAttributes, that.otherAttributes)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(extension)
            .append(XmlDefUtils.unwrapJaxb(style))
            .append(sharedStyle)
            .append(id)
            .append(otherAttributes)
            .toHashCode();
    }
}
