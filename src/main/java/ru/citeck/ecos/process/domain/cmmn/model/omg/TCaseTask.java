
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tCaseTask defines the type of element "caseTask"
 *
 *
 * <p>Java class for tCaseTask complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCaseTask">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tTask">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}parameterMapping" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="caseRefExpression" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}tExpression" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="caseRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
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
@XmlType(name = "tCaseTask", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "parameterMapping",
    "caseRefExpression"
})
public class TCaseTask
    extends TTask
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TParameterMapping> parameterMapping;
    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected TExpression caseRefExpression;
    @XmlAttribute(name = "caseRef")
    protected QName caseRef;

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
     * Gets the value of the caseRefExpression property.
     *
     * @return
     *     possible object is
     *     {@link TExpression }
     *
     */
    public TExpression getCaseRefExpression() {
        return caseRefExpression;
    }

    /**
     * Sets the value of the caseRefExpression property.
     *
     * @param value
     *     allowed object is
     *     {@link TExpression }
     *
     */
    public void setCaseRefExpression(TExpression value) {
        this.caseRefExpression = value;
    }

    /**
     * Gets the value of the caseRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getCaseRef() {
        return caseRef;
    }

    /**
     * Sets the value of the caseRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setCaseRef(QName value) {
        this.caseRef = value;
    }

}
