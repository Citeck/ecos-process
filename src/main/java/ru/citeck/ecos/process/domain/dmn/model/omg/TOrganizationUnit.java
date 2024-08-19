
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tOrganizationUnit complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tOrganizationUnit">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tBusinessContextElement">
 *       &lt;sequence>
 *         &lt;element name="decisionMade" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="decisionOwned" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tDMNElementReference" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "tOrganizationUnit", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "decisionMade",
    "decisionOwned"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TOrganizationUnit
    extends TBusinessContextElement
{

    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> decisionMade;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TDMNElementReference> decisionOwned;

    /**
     * Gets the value of the decisionMade property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the decisionMade property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDecisionMade().add(newItem);
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
    public List<TDMNElementReference> getDecisionMade() {
        if (decisionMade == null) {
            decisionMade = new ArrayList<TDMNElementReference>();
        }
        return this.decisionMade;
    }

    /**
     * Gets the value of the decisionOwned property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the decisionOwned property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDecisionOwned().add(newItem);
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
    public List<TDMNElementReference> getDecisionOwned() {
        if (decisionOwned == null) {
            decisionOwned = new ArrayList<TDMNElementReference>();
        }
        return this.decisionOwned;
    }

}
