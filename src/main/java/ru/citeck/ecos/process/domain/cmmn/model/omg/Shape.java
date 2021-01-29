
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for Shape complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Shape">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/DI}DiagramElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/DC}Bounds" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Shape", namespace = "http://www.omg.org/spec/CMMN/20151109/DI", propOrder = {
    "bounds"
})
@XmlSeeAlso({
    CMMNLabel.class,
    CMMNShape.class
})
public abstract class Shape
    extends DiagramElement
{

    @XmlElement(name = "Bounds", namespace = "http://www.omg.org/spec/CMMN/20151109/DC")
    protected Bounds bounds;

    /**
     * the optional bounds of the shape relative to the origin of its nesting plane.
     *
     * @return
     *     possible object is
     *     {@link Bounds }
     *
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * Sets the value of the bounds property.
     *
     * @param value
     *     allowed object is
     *     {@link Bounds }
     *
     */
    public void setBounds(Bounds value) {
        this.bounds = value;
    }

}
