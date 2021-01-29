
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tDecision defines the type of element "decision"
 *
 *
 * <p>Java class for tDecision complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDecision">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element name="input" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tDecisionParameter" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="output" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tDecisionParameter" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="externalRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="implementationType" type="{http://www.w3.org/2001/XMLSchema}anyURI" default="http://www.omg.org/spec/CMMN/DecisionType/Unspecified" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tDecision", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "input",
    "output"
})
public class TDecision
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TDecisionParameter> input;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TDecisionParameter> output;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "externalRef")
    protected QName externalRef;
    @XmlAttribute(name = "implementationType")
    @XmlSchemaType(name = "anyURI")
    protected String implementationType;

    /**
     * Gets the value of the input property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the input property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInput().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDecisionParameter }
     *
     *
     */
    public List<TDecisionParameter> getInput() {
        if (input == null) {
            input = new ArrayList<>();
        }
        return this.input;
    }

    /**
     * Gets the value of the output property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the output property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOutput().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDecisionParameter }
     *
     *
     */
    public List<TDecisionParameter> getOutput() {
        if (output == null) {
            output = new ArrayList<>();
        }
        return this.output;
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

    /**
     * Gets the value of the externalRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getExternalRef() {
        return externalRef;
    }

    /**
     * Sets the value of the externalRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setExternalRef(QName value) {
        this.externalRef = value;
    }

    /**
     * Gets the value of the implementationType property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getImplementationType() {
        if (implementationType == null) {
            return "http://www.omg.org/spec/CMMN/DecisionType/Unspecified";
        } else {
            return implementationType;
        }
    }

    /**
     * Sets the value of the implementationType property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setImplementationType(String value) {
        this.implementationType = value;
    }

}
