
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tParameter complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tParameter">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
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
@XmlType(name = "tParameter", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlSeeAlso({
    TProcessParameter.class,
    TDecisionParameter.class,
    TCaseParameter.class
})
public abstract class TParameter
    extends TCmmnElement
{

    @XmlAttribute(name = "name")
    protected String name;

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

}
