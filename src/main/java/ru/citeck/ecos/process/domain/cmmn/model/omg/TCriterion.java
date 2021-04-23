
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tCriterion complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCriterion">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sentryRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCriterion", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlSeeAlso({
    TEntryCriterion.class,
    TExitCriterion.class
})
public abstract class TCriterion
    extends TCmmnElement
{

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "sentryRef")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object sentryRef;

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the sentryRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getSentryRef() {
        return sentryRef;
    }

    /**
     * Sets the value of the sentryRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setSentryRef(Object value) {
        this.sentryRef = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TCriterion that = (TCriterion) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(getName(), that.getName())
            .append(CmmnXmlUtils.idRefToId(getSentryRef()), CmmnXmlUtils.idRefToId(that.getSentryRef()))
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(getName())
            .append(CmmnXmlUtils.idRefToId(getSentryRef()))
            .toHashCode();
    }
}
