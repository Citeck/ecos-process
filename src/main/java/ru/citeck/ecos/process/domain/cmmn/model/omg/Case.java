
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tCase defines the type of element "case".
 *
 *
 * <p>Java class for tCase complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCase">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element name="caseFileModel" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCaseFile" minOccurs="0"/>
 *         &lt;element name="casePlanModel" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tStage" minOccurs="0"/>
 *         &lt;element name="caseRoles" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCaseRoles" minOccurs="0"/>
 *         &lt;element name="input" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCaseParameter" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="output" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCaseParameter" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tCase", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "caseFileModel",
    "casePlanModel",
    "caseRoles",
    "input",
    "output"
})
public class Case
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected CaseFile caseFileModel;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected Stage casePlanModel;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected CaseRoles caseRoles;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TCaseParameter> input;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TCaseParameter> output;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the caseFileModel property.
     *
     * @return
     *     possible object is
     *     {@link CaseFile }
     *
     */
    public CaseFile getCaseFileModel() {
        return caseFileModel;
    }

    /**
     * Sets the value of the caseFileModel property.
     *
     * @param value
     *     allowed object is
     *     {@link CaseFile }
     *
     */
    public void setCaseFileModel(CaseFile value) {
        this.caseFileModel = value;
    }

    /**
     * Gets the value of the casePlanModel property.
     *
     * @return
     *     possible object is
     *     {@link Stage }
     *
     */
    public Stage getCasePlanModel() {
        return casePlanModel;
    }

    /**
     * Sets the value of the casePlanModel property.
     *
     * @param value
     *     allowed object is
     *     {@link Stage }
     *
     */
    public void setCasePlanModel(Stage value) {
        this.casePlanModel = value;
    }

    /**
     * Gets the value of the caseRoles property.
     *
     * @return
     *     possible object is
     *     {@link CaseRoles }
     *
     */
    public CaseRoles getCaseRoles() {
        return caseRoles;
    }

    /**
     * Sets the value of the caseRoles property.
     *
     * @param value
     *     allowed object is
     *     {@link CaseRoles }
     *
     */
    public void setCaseRoles(CaseRoles value) {
        this.caseRoles = value;
    }

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
     * {@link TCaseParameter }
     *
     *
     */
    public List<TCaseParameter> getInput() {
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
     * {@link TCaseParameter }
     *
     *
     */
    public List<TCaseParameter> getOutput() {
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

}
