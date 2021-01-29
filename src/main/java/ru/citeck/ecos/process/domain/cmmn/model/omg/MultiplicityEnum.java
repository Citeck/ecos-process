
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MultiplicityEnum.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="MultiplicityEnum">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ZeroOrOne"/>
 *     &lt;enumeration value="ZeroOrMore"/>
 *     &lt;enumeration value="ExactlyOne"/>
 *     &lt;enumeration value="OneOrMore"/>
 *     &lt;enumeration value="Unspecified"/>
 *     &lt;enumeration value="Unknown"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "MultiplicityEnum", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlEnum
public enum MultiplicityEnum {

    @XmlEnumValue("ZeroOrOne")
    ZERO_OR_ONE("ZeroOrOne"),
    @XmlEnumValue("ZeroOrMore")
    ZERO_OR_MORE("ZeroOrMore"),
    @XmlEnumValue("ExactlyOne")
    EXACTLY_ONE("ExactlyOne"),
    @XmlEnumValue("OneOrMore")
    ONE_OR_MORE("OneOrMore"),
    @XmlEnumValue("Unspecified")
    UNSPECIFIED("Unspecified"),
    @XmlEnumValue("Unknown")
    UNKNOWN("Unknown");
    private final String value;

    MultiplicityEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MultiplicityEnum fromValue(String v) {
        for (MultiplicityEnum c: MultiplicityEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
