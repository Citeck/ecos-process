
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDecisionTable complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDecisionTable">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tExpression">
 *       &lt;sequence>
 *         &lt;element name="input" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tInputClause" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="output" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tOutputClause" maxOccurs="unbounded"/>
 *         &lt;element name="annotation" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tRuleAnnotationClause" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="rule" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDecisionRule" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="hitPolicy" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tHitPolicy" default="UNIQUE" />
 *       &lt;attribute name="aggregation" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tBuiltinAggregator" />
 *       &lt;attribute name="preferredOrientation" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDecisionTableOrientation" default="Rule-as-Row" />
 *       &lt;attribute name="outputLabel" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tDecisionTable", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "input",
    "output",
    "annotation",
    "rule"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TDecisionTable
    extends TExpression
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TInputClause> input;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TOutputClause> output;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TRuleAnnotationClause> annotation;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDecisionRule> rule;
    @XmlAttribute(name = "hitPolicy")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected THitPolicy hitPolicy;
    @XmlAttribute(name = "aggregation")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TBuiltinAggregator aggregation;
    @XmlAttribute(name = "preferredOrientation")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected TDecisionTableOrientation preferredOrientation;
    @XmlAttribute(name = "outputLabel")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String outputLabel;

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
     * {@link TInputClause }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TInputClause> getInput() {
        if (input == null) {
            input = new ArrayList<TInputClause>();
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
     * {@link TOutputClause }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TOutputClause> getOutput() {
        if (output == null) {
            output = new ArrayList<TOutputClause>();
        }
        return this.output;
    }

    /**
     * Gets the value of the annotation property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the annotation property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnnotation().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TRuleAnnotationClause }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TRuleAnnotationClause> getAnnotation() {
        if (annotation == null) {
            annotation = new ArrayList<TRuleAnnotationClause>();
        }
        return this.annotation;
    }

    /**
     * Gets the value of the rule property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rule property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRule().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TDecisionRule }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TDecisionRule> getRule() {
        if (rule == null) {
            rule = new ArrayList<TDecisionRule>();
        }
        return this.rule;
    }

    /**
     * Gets the value of the hitPolicy property.
     *
     * @return
     *     possible object is
     *     {@link THitPolicy }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public THitPolicy getHitPolicy() {
        if (hitPolicy == null) {
            return THitPolicy.UNIQUE;
        } else {
            return hitPolicy;
        }
    }

    /**
     * Sets the value of the hitPolicy property.
     *
     * @param value
     *     allowed object is
     *     {@link THitPolicy }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setHitPolicy(THitPolicy value) {
        this.hitPolicy = value;
    }

    /**
     * Gets the value of the aggregation property.
     *
     * @return
     *     possible object is
     *     {@link TBuiltinAggregator }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TBuiltinAggregator getAggregation() {
        return aggregation;
    }

    /**
     * Sets the value of the aggregation property.
     *
     * @param value
     *     allowed object is
     *     {@link TBuiltinAggregator }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setAggregation(TBuiltinAggregator value) {
        this.aggregation = value;
    }

    /**
     * Gets the value of the preferredOrientation property.
     *
     * @return
     *     possible object is
     *     {@link TDecisionTableOrientation }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public TDecisionTableOrientation getPreferredOrientation() {
        if (preferredOrientation == null) {
            return TDecisionTableOrientation.RULE_AS_ROW;
        } else {
            return preferredOrientation;
        }
    }

    /**
     * Sets the value of the preferredOrientation property.
     *
     * @param value
     *     allowed object is
     *     {@link TDecisionTableOrientation }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setPreferredOrientation(TDecisionTableOrientation value) {
        this.preferredOrientation = value;
    }

    /**
     * Gets the value of the outputLabel property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getOutputLabel() {
        return outputLabel;
    }

    /**
     * Sets the value of the outputLabel property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setOutputLabel(String value) {
        this.outputLabel = value;
    }

}
