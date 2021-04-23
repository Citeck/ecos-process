
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tDecisionTask defines the type of element "decision"
 *
 *
 * <p>Java class for tDecisionTask complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDecisionTask">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tTask">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}parameterMapping" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="decisionRefExpression" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tExpression" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="decisionRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@EqualsAndHashCode(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tDecisionTask", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "parameterMapping",
    "decisionRefExpression"
})
public class TDecisionTask
    extends TTask
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TParameterMapping> parameterMapping;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExpression decisionRefExpression;
    @XmlAttribute(name = "decisionRef")
    protected QName decisionRef;

    /**
     * Gets the value of the parameterMapping property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameterMapping property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameterMapping().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TParameterMapping }
     *
     *
     */
    public List<TParameterMapping> getParameterMapping() {
        if (parameterMapping == null) {
            parameterMapping = new ArrayList<>();
        }
        return this.parameterMapping;
    }

    /**
     * Gets the value of the decisionRefExpression property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getDecisionRefExpression() {
        return decisionRefExpression;
    }

    /**
     * Sets the value of the decisionRefExpression property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setDecisionRefExpression(TExpression value) {
        this.decisionRefExpression = value;
    }

    /**
     * Gets the value of the decisionRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getDecisionRef() {
        return decisionRef;
    }

    /**
     * Sets the value of the decisionRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setDecisionRef(QName value) {
        this.decisionRef = value;
    }

}
