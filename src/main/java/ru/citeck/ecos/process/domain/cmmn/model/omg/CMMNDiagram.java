
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for CMMNDiagram complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CMMNDiagram">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/DI}Diagram">
 *       &lt;sequence>
 *         &lt;element name="Size" type="{http://www.omg.org/spec/CMMN/20151109/DC}Dimension" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/CMMNDI}CMMNDiagramElement" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cmmnElementRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CMMNDiagram", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", propOrder = {
    "size",
    "cmmnDiagramElement"
})
public class CMMNDiagram
    extends Diagram
{

    @XmlElement(name = "Size", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI")
    protected Dimension size;
    @XmlElementRef(name = "CMMNDiagramElement", namespace = "http://www.omg.org/spec/CMMN/20151109/CMMNDI", type = JAXBElement.class, required = false)
    protected List<JAXBElement<? extends DiagramElement>> cmmnDiagramElement;
    @XmlAttribute(name = "cmmnElementRef")
    protected QName cmmnElementRef;

    /**
     * Gets the value of the size property.
     *
     * @return
     *     possible object is
     *     {@link Dimension }
     *
     */
    public Dimension getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     *
     * @param value
     *     allowed object is
     *     {@link Dimension }
     *
     */
    public void setSize(Dimension value) {
        this.size = value;
    }

    /**
     * Gets the value of the cmmnDiagramElement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cmmnDiagramElement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCMMNDiagramElement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link DiagramElement }{@code >}
     * {@link JAXBElement }{@code <}{@link CMMNEdge }{@code >}
     * {@link JAXBElement }{@code <}{@link CMMNShape }{@code >}
     *
     *
     */
    public List<JAXBElement<? extends DiagramElement>> getCMMNDiagramElement() {
        if (cmmnDiagramElement == null) {
            cmmnDiagramElement = new ArrayList<>();
        }
        return this.cmmnDiagramElement;
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

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CMMNDiagram that = (CMMNDiagram) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(size, that.size)
            .append(CmmnXmlUtils.unwrapJaxb(cmmnDiagramElement), CmmnXmlUtils.unwrapJaxb(that.cmmnDiagramElement))
            .append(cmmnElementRef, that.cmmnElementRef)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(size)
            .append(CmmnXmlUtils.unwrapJaxb(cmmnDiagramElement))
            .append(cmmnElementRef)
            .toHashCode();
    }
}
