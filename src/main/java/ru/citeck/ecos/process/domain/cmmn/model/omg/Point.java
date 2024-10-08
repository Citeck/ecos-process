
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A Point specifies an location in some x-y coordinate system.
 *
 * <p>Java class for Point complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Point">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="x" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="y" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Point", namespace = "http://www.omg.org/spec/CMMN/20151109/DC")
public class Point {

    @XmlAttribute(name = "x", required = true)
    protected double x;
    @XmlAttribute(name = "y", required = true)
    protected double y;

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

}
