
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
 * <p>Java class for DMNDiagram complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DMNDiagram">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/DMN/20180521/DI/}Diagram">
 *       &lt;sequence>
 *         &lt;element name="Size" type="{http://www.omg.org/spec/DMN/20180521/DC/}Dimension" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNDiagramElement" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "DMNDiagram", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/", propOrder = {
    "size",
    "dmnDiagramElement"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class DMNDiagram
    extends Diagram
{

    @XmlElement(name = "Size", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected Dimension size;
    @XmlElementRef(name = "DMNDiagramElement", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/", type = JAXBElement.class, required = false)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<JAXBElement<? extends DiagramElement>> dmnDiagramElement;

    /**
     * Gets the value of the size property.
     *
     * @return
     *     possible object is
     *     {@link Dimension }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public Dimension getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     *
     * @param value
     *     allowed object is
     *     {@link Dimension }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setSize(Dimension value) {
        this.size = value;
    }

    /**
     * Gets the value of the dmnDiagramElement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dmnDiagramElement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDMNDiagramElement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link DiagramElement }{@code >}
     * {@link JAXBElement }{@code <}{@link DMNEdge }{@code >}
     * {@link JAXBElement }{@code <}{@link DMNShape }{@code >}
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<JAXBElement<? extends DiagramElement>> getDMNDiagramElement() {
        if (dmnDiagramElement == null) {
            dmnDiagramElement = new ArrayList<JAXBElement<? extends DiagramElement>>();
        }
        return this.dmnDiagramElement;
    }

}
