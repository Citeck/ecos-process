
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Color is a data type that represents a color value in the RGB format.
 *
 * <p>Java class for Color complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Color">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="red" use="required" type="{http://www.omg.org/spec/CMMN/20151109/DC}rgb" />
 *       &lt;attribute name="green" use="required" type="{http://www.omg.org/spec/CMMN/20151109/DC}rgb" />
 *       &lt;attribute name="blue" use="required" type="{http://www.omg.org/spec/CMMN/20151109/DC}rgb" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Color", namespace = "http://www.omg.org/spec/CMMN/20151109/DC")
public class Color {

    @XmlAttribute(name = "red", required = true)
    protected int red;
    @XmlAttribute(name = "green", required = true)
    protected int green;
    @XmlAttribute(name = "blue", required = true)
    protected int blue;

    /**
     * Gets the value of the red property.
     *
     */
    public int getRed() {
        return red;
    }

    /**
     * Sets the value of the red property.
     *
     */
    public void setRed(int value) {
        this.red = value;
    }

    /**
     * Gets the value of the green property.
     *
     */
    public int getGreen() {
        return green;
    }

    /**
     * Sets the value of the green property.
     *
     */
    public void setGreen(int value) {
        this.green = value;
    }

    /**
     * Gets the value of the blue property.
     *
     */
    public int getBlue() {
        return blue;
    }

    /**
     * Sets the value of the blue property.
     *
     */
    public void setBlue(int value) {
        this.blue = value;
    }

}
