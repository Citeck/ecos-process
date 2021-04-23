
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ru.citeck.ecos.process.domain.procdef.convert.io.xml.XmlDefUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tSentry defines the type of element "sentry"
 *
 *
 * <p>Java class for tSentry complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tSentry">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}onPart" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}ifPart" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tSentry", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "onPart",
    "ifPart"
})
public class Sentry
    extends TCmmnElement
{

    @XmlElementRef(name = "onPart", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", type = JAXBElement.class, required = false)
    protected List<JAXBElement<? extends TOnPart>> onPart;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TIfPart ifPart;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the onPart property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the onPart property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOnPart().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TOnPart }{@code >}
     * {@link JAXBElement }{@code <}{@link TPlanItemOnPart }{@code >}
     * {@link JAXBElement }{@code <}{@link TCaseFileItemOnPart }{@code >}
     *
     *
     */
    public List<JAXBElement<? extends TOnPart>> getOnPart() {
        if (onPart == null) {
            onPart = new ArrayList<>();
        }
        return this.onPart;
    }

    /**
     * Gets the value of the ifPart property.
     *
     * @return
     *     possible object is
     *     {@link TIfPart }
     *
     */
    public TIfPart getIfPart() {
        return ifPart;
    }

    /**
     * Sets the value of the ifPart property.
     *
     * @param value
     *     allowed object is
     *     {@link TIfPart }
     *
     */
    public void setIfPart(TIfPart value) {
        this.ifPart = value;
    }

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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Sentry sentry = (Sentry) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(XmlDefUtils.unwrapJaxb(onPart), XmlDefUtils.unwrapJaxb(sentry.onPart))
            .append(ifPart, sentry.ifPart)
            .append(name, sentry.name)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(XmlDefUtils.unwrapJaxb(onPart))
            .append(ifPart)
            .append(name)
            .toHashCode();
    }
}
