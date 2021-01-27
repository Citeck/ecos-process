
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for tExpression complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tExpression">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElementWithMixedContent">
 *       &lt;attribute name="language" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tExpression", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
public class TExpression
    extends TCmmnElementWithMixedContent
{

    @XmlAttribute(name = "language")
    @XmlSchemaType(name = "anyURI")
    protected String language;

    /**
     * Gets the value of the language property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the value of the language property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLanguage(String value) {
        this.language = value;
    }

}
