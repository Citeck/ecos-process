
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for KnownColor.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="KnownColor">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="maroon"/>
 *     &lt;enumeration value="red"/>
 *     &lt;enumeration value="orange"/>
 *     &lt;enumeration value="yellow"/>
 *     &lt;enumeration value="olive"/>
 *     &lt;enumeration value="purple"/>
 *     &lt;enumeration value="fuchsia"/>
 *     &lt;enumeration value="white"/>
 *     &lt;enumeration value="lime"/>
 *     &lt;enumeration value="green"/>
 *     &lt;enumeration value="navy"/>
 *     &lt;enumeration value="blue"/>
 *     &lt;enumeration value="aqua"/>
 *     &lt;enumeration value="teal"/>
 *     &lt;enumeration value="black"/>
 *     &lt;enumeration value="silver"/>
 *     &lt;enumeration value="gray"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "KnownColor", namespace = "http://www.omg.org/spec/DMN/20180521/DC/")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public enum KnownColor {


    /**
     * a color with a value of #800000
     *
     */
    @XmlEnumValue("maroon")
    MAROON("maroon"),

    /**
     * a color with a value of #FF0000
     *
     */
    @XmlEnumValue("red")
    RED("red"),

    /**
     * a color with a value of #FFA500
     *
     */
    @XmlEnumValue("orange")
    ORANGE("orange"),

    /**
     * a color with a value of #FFFF00
     *
     */
    @XmlEnumValue("yellow")
    YELLOW("yellow"),

    /**
     * a color with a value of #808000
     *
     */
    @XmlEnumValue("olive")
    OLIVE("olive"),

    /**
     * a color with a value of #800080
     *
     */
    @XmlEnumValue("purple")
    PURPLE("purple"),

    /**
     * a color with a value of #FF00FF
     *
     */
    @XmlEnumValue("fuchsia")
    FUCHSIA("fuchsia"),

    /**
     * a color with a value of #FFFFFF
     *
     */
    @XmlEnumValue("white")
    WHITE("white"),

    /**
     * a color with a value of #00FF00
     *
     */
    @XmlEnumValue("lime")
    LIME("lime"),

    /**
     * a color with a value of #008000
     *
     */
    @XmlEnumValue("green")
    GREEN("green"),

    /**
     * a color with a value of #000080
     *
     */
    @XmlEnumValue("navy")
    NAVY("navy"),

    /**
     * a color with a value of #0000FF
     *
     */
    @XmlEnumValue("blue")
    BLUE("blue"),

    /**
     * a color with a value of #00FFFF
     *
     */
    @XmlEnumValue("aqua")
    AQUA("aqua"),

    /**
     * a color with a value of #008080
     *
     */
    @XmlEnumValue("teal")
    TEAL("teal"),

    /**
     * a color with a value of #000000
     *
     */
    @XmlEnumValue("black")
    BLACK("black"),

    /**
     * a color with a value of #C0C0C0
     *
     */
    @XmlEnumValue("silver")
    SILVER("silver"),

    /**
     * a color with a value of #808080
     *
     */
    @XmlEnumValue("gray")
    GRAY("gray");
    private final String value;

    KnownColor(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static KnownColor fromValue(String v) {
        for (KnownColor c: KnownColor.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
