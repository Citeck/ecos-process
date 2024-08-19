
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDecisionTableOrientation.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="tDecisionTableOrientation">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Rule-as-Row"/>
 *     &lt;enumeration value="Rule-as-Column"/>
 *     &lt;enumeration value="CrossTable"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "tDecisionTableOrientation", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public enum TDecisionTableOrientation {

    @XmlEnumValue("Rule-as-Row")
    RULE_AS_ROW("Rule-as-Row"),
    @XmlEnumValue("Rule-as-Column")
    RULE_AS_COLUMN("Rule-as-Column"),
    @XmlEnumValue("CrossTable")
    CROSS_TABLE("CrossTable");
    private final String value;

    TDecisionTableOrientation(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TDecisionTableOrientation fromValue(String v) {
        for (TDecisionTableOrientation c: TDecisionTableOrientation.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
