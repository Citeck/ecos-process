
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tTimerEventListener complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tTimerEventListener">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tEventListener">
 *       &lt;sequence>
 *         &lt;element name="timerExpression" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tExpression" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}timerStart" minOccurs="0"/>
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
@XmlType(name = "tTimerEventListener", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "timerExpression",
    "timerStart"
})
public class TTimerEventListener
    extends TEventListener
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExpression timerExpression;
    @XmlElementRef(name = "timerStart", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends TStartTrigger> timerStart;

    /**
     * Gets the value of the timerExpression property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getTimerExpression() {
        return timerExpression;
    }

    /**
     * Sets the value of the timerExpression property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setTimerExpression(TExpression value) {
        this.timerExpression = value;
    }

    /**
     *
     *                 timerStart can be used to trigger the timer after a PlanItem or CaseFileItem
     *                 lifecycle state transition has occurred.
     *
     *
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link TCaseFileItemStartTrigger }{@code >}
     *     {@link JAXBElement }{@code <}{@link TStartTrigger }{@code >}
     *     {@link JAXBElement }{@code <}{@link TPlanItemStartTrigger }{@code >}
     *
     */
    public JAXBElement<? extends TStartTrigger> getTimerStart() {
        return timerStart;
    }

    /**
     * Sets the value of the timerStart property.
     *
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link TCaseFileItemStartTrigger }{@code >}
     *     {@link JAXBElement }{@code <}{@link TStartTrigger }{@code >}
     *     {@link JAXBElement }{@code <}{@link TPlanItemStartTrigger }{@code >}
     *
     */
    public void setTimerStart(JAXBElement<? extends TStartTrigger> value) {
        this.timerStart = value;
    }

}
