
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tAssociationDirection.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="tAssociationDirection">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="None"/>
 *     &lt;enumeration value="One"/>
 *     &lt;enumeration value="Both"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "tAssociationDirection", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public enum TAssociationDirection {

    @XmlEnumValue("None")
    NONE("None"),
    @XmlEnumValue("One")
    ONE("One"),
    @XmlEnumValue("Both")
    BOTH("Both");
    private final String value;

    TAssociationDirection(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TAssociationDirection fromValue(String v) {
        for (TAssociationDirection c: TAssociationDirection.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
