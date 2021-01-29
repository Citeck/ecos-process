
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PlanItemTransition.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="PlanItemTransition">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="close"/>
 *     &lt;enumeration value="complete"/>
 *     &lt;enumeration value="create"/>
 *     &lt;enumeration value="disable"/>
 *     &lt;enumeration value="enable"/>
 *     &lt;enumeration value="exit"/>
 *     &lt;enumeration value="fault"/>
 *     &lt;enumeration value="manualStart"/>
 *     &lt;enumeration value="occur"/>
 *     &lt;enumeration value="parentResume"/>
 *     &lt;enumeration value="parentSuspend"/>
 *     &lt;enumeration value="reactivate"/>
 *     &lt;enumeration value="reenable"/>
 *     &lt;enumeration value="resume"/>
 *     &lt;enumeration value="start"/>
 *     &lt;enumeration value="suspend"/>
 *     &lt;enumeration value="terminate"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "PlanItemTransition", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlEnum
public enum PlanItemTransition {

    @XmlEnumValue("close")
    CLOSE("close"),
    @XmlEnumValue("complete")
    COMPLETE("complete"),
    @XmlEnumValue("create")
    CREATE("create"),
    @XmlEnumValue("disable")
    DISABLE("disable"),
    @XmlEnumValue("enable")
    ENABLE("enable"),
    @XmlEnumValue("exit")
    EXIT("exit"),
    @XmlEnumValue("fault")
    FAULT("fault"),
    @XmlEnumValue("manualStart")
    MANUAL_START("manualStart"),
    @XmlEnumValue("occur")
    OCCUR("occur"),
    @XmlEnumValue("parentResume")
    PARENT_RESUME("parentResume"),
    @XmlEnumValue("parentSuspend")
    PARENT_SUSPEND("parentSuspend"),
    @XmlEnumValue("reactivate")
    REACTIVATE("reactivate"),
    @XmlEnumValue("reenable")
    REENABLE("reenable"),
    @XmlEnumValue("resume")
    RESUME("resume"),
    @XmlEnumValue("start")
    START("start"),
    @XmlEnumValue("suspend")
    SUSPEND("suspend"),
    @XmlEnumValue("terminate")
    TERMINATE("terminate");
    private final String value;

    PlanItemTransition(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PlanItemTransition fromValue(String v) {
        for (PlanItemTransition c: PlanItemTransition.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
