
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CaseFileItemTransition.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CaseFileItemTransition">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="addChild"/>
 *     &lt;enumeration value="addReference"/>
 *     &lt;enumeration value="create"/>
 *     &lt;enumeration value="delete"/>
 *     &lt;enumeration value="removeChild"/>
 *     &lt;enumeration value="removeReference"/>
 *     &lt;enumeration value="replace"/>
 *     &lt;enumeration value="update"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CaseFileItemTransition", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlEnum
public enum CaseFileItemTransition {

    @XmlEnumValue("addChild")
    ADD_CHILD("addChild"),
    @XmlEnumValue("addReference")
    ADD_REFERENCE("addReference"),
    @XmlEnumValue("create")
    CREATE("create"),
    @XmlEnumValue("delete")
    DELETE("delete"),
    @XmlEnumValue("removeChild")
    REMOVE_CHILD("removeChild"),
    @XmlEnumValue("removeReference")
    REMOVE_REFERENCE("removeReference"),
    @XmlEnumValue("replace")
    REPLACE("replace"),
    @XmlEnumValue("update")
    UPDATE("update");
    private final String value;

    CaseFileItemTransition(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CaseFileItemTransition fromValue(String v) {
        for (CaseFileItemTransition c: CaseFileItemTransition.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
