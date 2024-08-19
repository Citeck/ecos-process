
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Generated;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDecision complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDecision">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDRGElement">
 *       &lt;sequence>
 *         &lt;element name="question" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="allowedAnswers" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="variable" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tInformationItem" minOccurs="0"/>
 *         &lt;element name="informationRequirement" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tInformationRequirement" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="knowledgeRequirement" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tKnowledgeRequirement" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="authorityRequirement" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tAuthorityRequirement" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="supportedObjective" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="impactedPerformanceIndicator" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="decisionMaker" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="decisionOwner" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="usingProcess" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="usingTask" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}expression" minOccurs="0"/>
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
@XmlType(name = "tDecision", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "question",
    "allowedAnswers",
    "variable",
    "informationRequirement",
    "knowledgeRequirement",
    "authorityRequirement",
    "supportedObjective",
    "impactedPerformanceIndicator",
    "decisionMaker",
    "decisionOwner",
    "usingProcess",
    "usingTask",
    "expression"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TDecision
    extends TDRGElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String question;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String allowedAnswers;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TInformationItem variable;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TInformationRequirement> informationRequirement;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TKnowledgeRequirement> knowledgeRequirement;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TAuthorityRequirement> authorityRequirement;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> supportedObjective;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> impactedPerformanceIndicator;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> decisionMaker;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> decisionOwner;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> usingProcess;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> usingTask;
    @XmlElementRef(name = "expression", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", type = JAXBElement.class, required = false)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected JAXBElement<? extends TExpression> expression;

    /**
     * Gets the value of the question property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getQuestion() {
        return question;
    }

    /**
     * Sets the value of the question property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setQuestion(String value) {
        this.question = value;
    }

    /**
     * Gets the value of the allowedAnswers property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getAllowedAnswers() {
        return allowedAnswers;
    }

    /**
     * Sets the value of the allowedAnswers property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setAllowedAnswers(String value) {
        this.allowedAnswers = value;
    }

    /**
     * Gets the value of the variable property.
     *
     * @return
     *     possible object is
     *     {@link TInformationItem }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TInformationItem getVariable() {
        return variable;
    }

    /**
     * Sets the value of the variable property.
     *
     * @param value
     *     allowed object is
     *     {@link TInformationItem }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setVariable(TInformationItem value) {
        this.variable = value;
    }

    /**
     * Gets the value of the informationRequirement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the informationRequirement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInformationRequirement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TInformationRequirement }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TInformationRequirement> getInformationRequirement() {
        if (informationRequirement == null) {
            informationRequirement = new ArrayList<TInformationRequirement>();
        }
        return this.informationRequirement;
    }

    /**
     * Gets the value of the knowledgeRequirement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the knowledgeRequirement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKnowledgeRequirement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TKnowledgeRequirement }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TKnowledgeRequirement> getKnowledgeRequirement() {
        if (knowledgeRequirement == null) {
            knowledgeRequirement = new ArrayList<TKnowledgeRequirement>();
        }
        return this.knowledgeRequirement;
    }

    /**
     * Gets the value of the authorityRequirement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authorityRequirement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthorityRequirement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TAuthorityRequirement }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TAuthorityRequirement> getAuthorityRequirement() {
        if (authorityRequirement == null) {
            authorityRequirement = new ArrayList<TAuthorityRequirement>();
        }
        return this.authorityRequirement;
    }

    /**
     * Gets the value of the supportedObjective property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the supportedObjective property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSupportedObjective().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDMNElementReference }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDMNElementReference> getSupportedObjective() {
        if (supportedObjective == null) {
            supportedObjective = new ArrayList<TDMNElementReference>();
        }
        return this.supportedObjective;
    }

    /**
     * Gets the value of the impactedPerformanceIndicator property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the impactedPerformanceIndicator property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getImpactedPerformanceIndicator().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDMNElementReference }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDMNElementReference> getImpactedPerformanceIndicator() {
        if (impactedPerformanceIndicator == null) {
            impactedPerformanceIndicator = new ArrayList<TDMNElementReference>();
        }
        return this.impactedPerformanceIndicator;
    }

    /**
     * Gets the value of the decisionMaker property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the decisionMaker property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDecisionMaker().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDMNElementReference }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDMNElementReference> getDecisionMaker() {
        if (decisionMaker == null) {
            decisionMaker = new ArrayList<TDMNElementReference>();
        }
        return this.decisionMaker;
    }

    /**
     * Gets the value of the decisionOwner property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the decisionOwner property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDecisionOwner().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDMNElementReference }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDMNElementReference> getDecisionOwner() {
        if (decisionOwner == null) {
            decisionOwner = new ArrayList<TDMNElementReference>();
        }
        return this.decisionOwner;
    }

    /**
     * Gets the value of the usingProcess property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the usingProcess property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUsingProcess().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDMNElementReference }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDMNElementReference> getUsingProcess() {
        if (usingProcess == null) {
            usingProcess = new ArrayList<TDMNElementReference>();
        }
        return this.usingProcess;
    }

    /**
     * Gets the value of the usingTask property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the usingTask property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUsingTask().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDMNElementReference }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDMNElementReference> getUsingTask() {
        if (usingTask == null) {
            usingTask = new ArrayList<TDMNElementReference>();
        }
        return this.usingTask;
    }

    /**
     * Gets the value of the expression property.
     *
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link TDecisionTable }{@code >}
     *     {@link JAXBElement }{@code <}{@link TRelation }{@code >}
     *     {@link JAXBElement }{@code <}{@link TList }{@code >}
     *     {@link JAXBElement }{@code <}{@link TFunctionDefinition }{@code >}
     *     {@link JAXBElement }{@code <}{@link TContext }{@code >}
     *     {@link JAXBElement }{@code <}{@link TLiteralExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TInvocation }{@code >}
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public JAXBElement<? extends TExpression> getExpression() {
        return expression;
    }

    /**
     * Sets the value of the expression property.
     *
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link TDecisionTable }{@code >}
     *     {@link JAXBElement }{@code <}{@link TRelation }{@code >}
     *     {@link JAXBElement }{@code <}{@link TList }{@code >}
     *     {@link JAXBElement }{@code <}{@link TFunctionDefinition }{@code >}
     *     {@link JAXBElement }{@code <}{@link TContext }{@code >}
     *     {@link JAXBElement }{@code <}{@link TLiteralExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TExpression }{@code >}
     *     {@link JAXBElement }{@code <}{@link TInvocation }{@code >}
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setExpression(JAXBElement<? extends TExpression> value) {
        this.expression = value;
    }

}
