
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 *
 *         tPlanItemcontrol defines the type of element "planItemControl".
 *
 *
 * <p>Java class for tPlanItemControl complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlanItemControl">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}repetitionRule" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}requiredRule" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}manualActivationRule" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlanItemControl", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "repetitionRule",
    "requiredRule",
    "manualActivationRule"
})
public class TPlanItemControl
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TRepetitionRule repetitionRule;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TRequiredRule requiredRule;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TManualActivationRule manualActivationRule;

    /**
     * Gets the value of the repetitionRule property.
     *
     * @return
     *     possible object is
     *     {@link TRepetitionRule }
     *
     */
    public TRepetitionRule getRepetitionRule() {
        return repetitionRule;
    }

    /**
     * Sets the value of the repetitionRule property.
     *
     * @param value
     *     allowed object is
     *     {@link TRepetitionRule }
     *
     */
    public void setRepetitionRule(TRepetitionRule value) {
        this.repetitionRule = value;
    }

    /**
     * Gets the value of the requiredRule property.
     *
     * @return
     *     possible object is
     *     {@link TRequiredRule }
     *
     */
    public TRequiredRule getRequiredRule() {
        return requiredRule;
    }

    /**
     * Sets the value of the requiredRule property.
     *
     * @param value
     *     allowed object is
     *     {@link TRequiredRule }
     *
     */
    public void setRequiredRule(TRequiredRule value) {
        this.requiredRule = value;
    }

    /**
     * Gets the value of the manualActivationRule property.
     *
     * @return
     *     possible object is
     *     {@link TManualActivationRule }
     *
     */
    public TManualActivationRule getManualActivationRule() {
        return manualActivationRule;
    }

    /**
     * Sets the value of the manualActivationRule property.
     *
     * @param value
     *     allowed object is
     *     {@link TManualActivationRule }
     *
     */
    public void setManualActivationRule(TManualActivationRule value) {
        this.manualActivationRule = value;
    }

}
