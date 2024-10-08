//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.02.05 at 07:02:39 AM GMT+07:00
//


package ru.citeck.ecos.process.domain.bpmn.model.omg;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tGatewayDirection.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="tGatewayDirection">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Unspecified"/>
 *     &lt;enumeration value="Converging"/>
 *     &lt;enumeration value="Diverging"/>
 *     &lt;enumeration value="Mixed"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "tGatewayDirection")
@XmlEnum
public enum TGatewayDirection {

    @XmlEnumValue("Unspecified")
    UNSPECIFIED("Unspecified"),
    @XmlEnumValue("Converging")
    CONVERGING("Converging"),
    @XmlEnumValue("Diverging")
    DIVERGING("Diverging"),
    @XmlEnumValue("Mixed")
    MIXED("Mixed");
    private final String value;

    TGatewayDirection(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TGatewayDirection fromValue(String v) {
        for (TGatewayDirection c: TGatewayDirection.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
