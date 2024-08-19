
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DMNDI complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DMNDI">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNDiagram" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNStyle" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DMNDI", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/", propOrder = {
    "dmnDiagram",
    "dmnStyle"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class DMNDI {

    @XmlElement(name = "DMNDiagram", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<DMNDiagram> dmnDiagram;
    @XmlElement(name = "DMNStyle", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<DMNStyle> dmnStyle;

    /**
     * Gets the value of the dmnDiagram property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dmnDiagram property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDMNDiagram().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DMNDiagram }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<DMNDiagram> getDMNDiagram() {
        if (dmnDiagram == null) {
            dmnDiagram = new ArrayList<DMNDiagram>();
        }
        return this.dmnDiagram;
    }

    /**
     * Gets the value of the dmnStyle property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dmnStyle property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDMNStyle().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DMNStyle }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<DMNStyle> getDMNStyle() {
        if (dmnStyle == null) {
            dmnStyle = new ArrayList<DMNStyle>();
        }
        return this.dmnStyle;
    }

}
