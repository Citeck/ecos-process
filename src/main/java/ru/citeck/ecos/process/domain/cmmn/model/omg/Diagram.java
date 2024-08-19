
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for Diagram complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Diagram">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/DI}DiagramElement">
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="documentation" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="resolution" type="{http://www.w3.org/2001/XMLSchema}double" />
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
@XmlType(name = "Diagram", namespace = "http://www.omg.org/spec/CMMN/20151109/DI")
@XmlSeeAlso({
    CMMNDiagram.class
})
public abstract class Diagram
    extends DiagramElement
{

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "documentation")
    protected String documentation;
    @XmlAttribute(name = "resolution")
    protected Double resolution;

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the documentation property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * Sets the value of the documentation property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDocumentation(String value) {
        this.documentation = value;
    }

    /**
     * Gets the value of the resolution property.
     *
     * @return
     *     possible object is
     *     {@link Double }
     *
     */
    public Double getResolution() {
        return resolution;
    }

    /**
     * Sets the value of the resolution property.
     *
     * @param value
     *     allowed object is
     *     {@link Double }
     *
     */
    public void setResolution(Double value) {
        this.resolution = value;
    }

}
