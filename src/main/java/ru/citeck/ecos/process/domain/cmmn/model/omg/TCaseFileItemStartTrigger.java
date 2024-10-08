
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for tCaseFileItemStartTrigger complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCaseFileItemStartTrigger">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tStartTrigger">
 *       &lt;sequence>
 *         &lt;element name="standardEvent" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}CaseFileItemTransition" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="sourceRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCaseFileItemStartTrigger", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "standardEvent"
})
public class TCaseFileItemStartTrigger
    extends TStartTrigger
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected CaseFileItemTransition standardEvent;
    @XmlAttribute(name = "sourceRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object sourceRef;

    /**
     * Gets the value of the standardEvent property.
     *
     * @return
     *     possible object is
     *     {@link CaseFileItemTransition }
     *
     */
    public CaseFileItemTransition getStandardEvent() {
        return standardEvent;
    }

    /**
     * Sets the value of the standardEvent property.
     *
     * @param value
     *     allowed object is
     *     {@link CaseFileItemTransition }
     *
     */
    public void setStandardEvent(CaseFileItemTransition value) {
        this.standardEvent = value;
    }

    /**
     * Gets the value of the sourceRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getSourceRef() {
        return sourceRef;
    }

    /**
     * Sets the value of the sourceRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setSourceRef(Object value) {
        this.sourceRef = value;
    }

}
