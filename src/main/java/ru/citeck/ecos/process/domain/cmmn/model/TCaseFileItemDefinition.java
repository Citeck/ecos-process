
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;


/**
 *
 *         tCaseFileItemDefinition defines the type of element "caseFileItemDefinition"
 *
 *
 * <p>Java class for tCaseFileItemDefinition complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tCaseFileItemDefinition">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.omg.org/spec/CMMN/20151109/MODEL}property" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="definitionType" type="{http://www.omg.org/spec/CMMN/20151109/MODEL}DefinitionTypeEnum" default="http://www.omg.org/spec/CMMN/DefinitionType/Unspecified" />
 *       &lt;attribute name="structureRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="importRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCaseFileItemDefinition", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL", propOrder = {
    "property"
})
public class TCaseFileItemDefinition
    extends TCmmnElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
    protected List<TProperty> property;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "definitionType")
    protected String definitionType;
    @XmlAttribute(name = "structureRef")
    protected QName structureRef;
    @XmlAttribute(name = "importRef")
    protected QName importRef;

    /**
     * Gets the value of the property property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TProperty }
     *
     *
     */
    public List<TProperty> getProperty() {
        if (property == null) {
            property = new ArrayList<>();
        }
        return this.property;
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

    /**
     * Gets the value of the definitionType property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDefinitionType() {
        if (definitionType == null) {
            return "http://www.omg.org/spec/CMMN/DefinitionType/Unspecified";
        } else {
            return definitionType;
        }
    }

    /**
     * Sets the value of the definitionType property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDefinitionType(String value) {
        this.definitionType = value;
    }

    /**
     * Gets the value of the structureRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getStructureRef() {
        return structureRef;
    }

    /**
     * Sets the value of the structureRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setStructureRef(QName value) {
        this.structureRef = value;
    }

    /**
     * Gets the value of the importRef property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getImportRef() {
        return importRef;
    }

    /**
     * Sets the value of the importRef property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setImportRef(QName value) {
        this.importRef = value;
    }

}
