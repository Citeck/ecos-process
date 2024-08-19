
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AlignmentKind.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AlignmentKind">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="start"/>
 *     &lt;enumeration value="end"/>
 *     &lt;enumeration value="center"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "AlignmentKind", namespace = "http://www.omg.org/spec/DMN/20180521/DC/")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public enum AlignmentKind {

    @XmlEnumValue("start")
    START("start"),
    @XmlEnumValue("end")
    END("end"),
    @XmlEnumValue("center")
    CENTER("center");
    private final String value;

    AlignmentKind(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AlignmentKind fromValue(String v) {
        for (AlignmentKind c: AlignmentKind.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
