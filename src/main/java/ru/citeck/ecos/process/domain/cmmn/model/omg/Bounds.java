
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Bounds specifies a rectangular area in some x-y coordinate system that is defined by a location (x and y) and a size (width and height).
 *
 * <p>Java class for Bounds complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Bounds">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="x" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="y" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="width" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="height" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Bounds", namespace = "http://www.omg.org/spec/CMMN/20151109/DC")
public class Bounds {

    @XmlAttribute(name = "x", required = true)
    protected double x;
    @XmlAttribute(name = "y", required = true)
    protected double y;
    @XmlAttribute(name = "width", required = true)
    protected double width;
    @XmlAttribute(name = "height", required = true)
    protected double height;

    /**
     * Gets the value of the x property.
     *
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the value of the x property.
     *
     */
    public void setX(double value) {
        this.x = value;
    }

    /**
     * Gets the value of the y property.
     *
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     *
     */
    public void setY(double value) {
        this.y = value;
    }

    /**
     * Gets the value of the width property.
     *
     */
    public double getWidth() {
        return width;
    }

    /**
     * Sets the value of the width property.
     *
     */
    public void setWidth(double value) {
        this.width = value;
    }

    /**
     * Gets the value of the height property.
     *
     */
    public double getHeight() {
        return height;
    }

    /**
     * Sets the value of the height property.
     *
     */
    public void setHeight(double value) {
        this.height = value;
    }

}
