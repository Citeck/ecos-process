
package ru.citeck.ecos.process.domain.dmn.model.omg;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Generated;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDefinitions complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDefinitions">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tNamedElement">
 *       &lt;sequence>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}import" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="itemDefinition" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tItemDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}drgElement" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}artifact" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="elementCollection" type="{https://www.omg.org/spec/DMN/20191111/MODEL/}tElementCollection" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/MODEL/}businessContextElement" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{https://www.omg.org/spec/DMN/20191111/DMNDI/}DMNDI" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="expressionLanguage" type="{http://www.w3.org/2001/XMLSchema}anyURI" default="https://www.omg.org/spec/DMN/20191111/FEEL/" />
 *       &lt;attribute name="typeLanguage" type="{http://www.w3.org/2001/XMLSchema}anyURI" default="https://www.omg.org/spec/DMN/20191111/FEEL/" />
 *       &lt;attribute name="namespace" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="exporter" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="exporterVersion" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tDefinitions", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", propOrder = {
    "_import",
    "itemDefinition",
    "drgElement",
    "artifact",
    "elementCollection",
    "businessContextElement",
    "dmndi"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TDefinitions
    extends TNamedElement
{

    @XmlElement(name = "import", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TImport> _import;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TItemDefinition> itemDefinition;
    @XmlElementRef(name = "drgElement", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", type = JAXBElement.class, required = false)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<JAXBElement<? extends TDRGElement>> drgElement;
    @XmlElementRef(name = "artifact", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", type = JAXBElement.class, required = false)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<JAXBElement<? extends TArtifact>> artifact;
    @XmlElement(namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<TElementCollection> elementCollection;
    @XmlElementRef(name = "businessContextElement", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/", type = JAXBElement.class, required = false)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<JAXBElement<? extends TBusinessContextElement>> businessContextElement;
    @XmlElement(name = "DMNDI", namespace = "https://www.omg.org/spec/DMN/20191111/DMNDI/")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected DMNDI dmndi;
    @XmlAttribute(name = "expressionLanguage")
    @XmlSchemaType(name = "anyURI")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String expressionLanguage;
    @XmlAttribute(name = "typeLanguage")
    @XmlSchemaType(name = "anyURI")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String typeLanguage;
    @XmlAttribute(name = "namespace", required = true)
    @XmlSchemaType(name = "anyURI")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String namespace;
    @XmlAttribute(name = "exporter")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String exporter;
    @XmlAttribute(name = "exporterVersion")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String exporterVersion;

    /**
     * Gets the value of the import property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the import property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getImport().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TImport }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TImport> getImport() {
        if (_import == null) {
            _import = new ArrayList<TImport>();
        }
        return this._import;
    }

    /**
     * Gets the value of the itemDefinition property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the itemDefinition property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItemDefinition().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TItemDefinition }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TItemDefinition> getItemDefinition() {
        if (itemDefinition == null) {
            itemDefinition = new ArrayList<TItemDefinition>();
        }
        return this.itemDefinition;
    }

    /**
     * Gets the value of the drgElement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the drgElement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDrgElement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TBusinessKnowledgeModel }{@code >}
     * {@link JAXBElement }{@code <}{@link TKnowledgeSource }{@code >}
     * {@link JAXBElement }{@code <}{@link TInputData }{@code >}
     * {@link JAXBElement }{@code <}{@link TDecisionService }{@code >}
     * {@link JAXBElement }{@code <}{@link TInvocable }{@code >}
     * {@link JAXBElement }{@code <}{@link TDRGElement }{@code >}
     * {@link JAXBElement }{@code <}{@link TDecision }{@code >}
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<JAXBElement<? extends TDRGElement>> getDrgElement() {
        if (drgElement == null) {
            drgElement = new ArrayList<JAXBElement<? extends TDRGElement>>();
        }
        return this.drgElement;
    }

    /**
     * Gets the value of the artifact property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the artifact property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getArtifact().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TGroup }{@code >}
     * {@link JAXBElement }{@code <}{@link TTextAnnotation }{@code >}
     * {@link JAXBElement }{@code <}{@link TArtifact }{@code >}
     * {@link JAXBElement }{@code <}{@link TAssociation }{@code >}
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<JAXBElement<? extends TArtifact>> getArtifact() {
        if (artifact == null) {
            artifact = new ArrayList<JAXBElement<? extends TArtifact>>();
        }
        return this.artifact;
    }

    /**
     * Gets the value of the elementCollection property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the elementCollection property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getElementCollection().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TElementCollection }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<TElementCollection> getElementCollection() {
        if (elementCollection == null) {
            elementCollection = new ArrayList<TElementCollection>();
        }
        return this.elementCollection;
    }

    /**
     * Gets the value of the businessContextElement property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the businessContextElement property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBusinessContextElement().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link TPerformanceIndicator }{@code >}
     * {@link JAXBElement }{@code <}{@link TOrganizationUnit }{@code >}
     * {@link JAXBElement }{@code <}{@link TBusinessContextElement }{@code >}
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<JAXBElement<? extends TBusinessContextElement>> getBusinessContextElement() {
        if (businessContextElement == null) {
            businessContextElement = new ArrayList<JAXBElement<? extends TBusinessContextElement>>();
        }
        return this.businessContextElement;
    }

    /**
     * Gets the value of the dmndi property.
     *
     * @return
     *     possible object is
     *     {@link DMNDI }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public DMNDI getDMNDI() {
        return dmndi;
    }

    /**
     * Sets the value of the dmndi property.
     *
     * @param value
     *     allowed object is
     *     {@link DMNDI }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDMNDI(DMNDI value) {
        this.dmndi = value;
    }

    /**
     * Gets the value of the expressionLanguage property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getExpressionLanguage() {
        if (expressionLanguage == null) {
            return "https://www.omg.org/spec/DMN/20191111/FEEL/";
        } else {
            return expressionLanguage;
        }
    }

    /**
     * Sets the value of the expressionLanguage property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setExpressionLanguage(String value) {
        this.expressionLanguage = value;
    }

    /**
     * Gets the value of the typeLanguage property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getTypeLanguage() {
        if (typeLanguage == null) {
            return "https://www.omg.org/spec/DMN/20191111/FEEL/";
        } else {
            return typeLanguage;
        }
    }

    /**
     * Sets the value of the typeLanguage property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setTypeLanguage(String value) {
        this.typeLanguage = value;
    }

    /**
     * Gets the value of the namespace property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the value of the namespace property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setNamespace(String value) {
        this.namespace = value;
    }

    /**
     * Gets the value of the exporter property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getExporter() {
        return exporter;
    }

    /**
     * Sets the value of the exporter property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setExporter(String value) {
        this.exporter = value;
    }

    /**
     * Gets the value of the exporterVersion property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getExporterVersion() {
        return exporterVersion;
    }

    /**
     * Sets the value of the exporterVersion property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setExporterVersion(String value) {
        this.exporterVersion = value;
    }

}
