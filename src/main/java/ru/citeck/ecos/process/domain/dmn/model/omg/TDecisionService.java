
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDecisionService complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDecisionService">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tInvocable">
 *       &lt;sequence>
 *         &lt;element name="outputDecision" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="encapsulatedDecision" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="inputDecision" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="inputData" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tDecisionService", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "outputDecision",
    "encapsulatedDecision",
    "inputDecision",
    "inputData"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TDecisionService
    extends TInvocable
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> outputDecision;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> encapsulatedDecision;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> inputDecision;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> inputData;

    /**
     * Gets the value of the outputDecision property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the outputDecision property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOutputDecision().add(newItem);
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
    public List<TDMNElementReference> getOutputDecision() {
        if (outputDecision == null) {
            outputDecision = new ArrayList<TDMNElementReference>();
        }
        return this.outputDecision;
    }

    /**
     * Gets the value of the encapsulatedDecision property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the encapsulatedDecision property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEncapsulatedDecision().add(newItem);
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
    public List<TDMNElementReference> getEncapsulatedDecision() {
        if (encapsulatedDecision == null) {
            encapsulatedDecision = new ArrayList<TDMNElementReference>();
        }
        return this.encapsulatedDecision;
    }

    /**
     * Gets the value of the inputDecision property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputDecision property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputDecision().add(newItem);
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
    public List<TDMNElementReference> getInputDecision() {
        if (inputDecision == null) {
            inputDecision = new ArrayList<TDMNElementReference>();
        }
        return this.inputDecision;
    }

    /**
     * Gets the value of the inputData property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputData property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputData().add(newItem);
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
    public List<TDMNElementReference> getInputData() {
        if (inputData == null) {
            inputData = new ArrayList<TDMNElementReference>();
        }
        return this.inputData;
    }

}
