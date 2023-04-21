
package ru.citeck.ecos.process.domain.dmn.model.omg;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tKnowledgeRequirement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tKnowledgeRequirement">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElement">
 *       &lt;sequence>
 *         &lt;element name="requiredKnowledge" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference"/>
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
@XmlType(name = "tKnowledgeRequirement", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "requiredKnowledge"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TKnowledgeRequirement
    extends TDMNElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDMNElementReference requiredKnowledge;

    /**
     * Gets the value of the requiredKnowledge property.
     *
     * @return
     *     possible object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDMNElementReference getRequiredKnowledge() {
        return requiredKnowledge;
    }

    /**
     * Sets the value of the requiredKnowledge property.
     *
     * @param value
     *     allowed object is
     *     {@link TDMNElementReference }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setRequiredKnowledge(TDMNElementReference value) {
        this.requiredKnowledge = value;
    }

}
