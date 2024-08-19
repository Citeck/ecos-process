
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils;

import jakarta.xml.bind.annotation.*;


/**
 * <p>Java class for tCaseFileItemOnPart complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCaseFileItemOnPart">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tOnPart">
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
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCaseFileItemOnPart", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "standardEvent"
})
public class TCaseFileItemOnPart
    extends TOnPart
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TCaseFileItemOnPart that = (TCaseFileItemOnPart) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(getStandardEvent(), that.getStandardEvent())
            .append(CmmnXmlUtils.idRefToId(getSourceRef()), CmmnXmlUtils.idRefToId(that.getSourceRef()))
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(getStandardEvent())
            .append(CmmnXmlUtils.idRefToId(getSourceRef()))
            .toHashCode();
    }
}
