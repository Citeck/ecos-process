
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tRelationshipDirection.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="tRelationshipDirection">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="None"/>
 *     &lt;enumeration value="Forward"/>
 *     &lt;enumeration value="Backward"/>
 *     &lt;enumeration value="Both"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "tRelationshipDirection", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlEnum
public enum TRelationshipDirection {

    @XmlEnumValue("None")
    NONE("None"),
    @XmlEnumValue("Forward")
    FORWARD("Forward"),
    @XmlEnumValue("Backward")
    BACKWARD("Backward"),
    @XmlEnumValue("Both")
    BOTH("Both");
    private final String value;

    TRelationshipDirection(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TRelationshipDirection fromValue(String v) {
        for (TRelationshipDirection c: TRelationshipDirection.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
