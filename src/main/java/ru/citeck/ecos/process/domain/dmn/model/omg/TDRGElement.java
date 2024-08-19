
package ru.citeck.ecos.process.domain.dmn.model.omg;

import jakarta.annotation.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tDRGElement complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDRGElement">
 *   &lt;complexContent>
 *     &lt;extension base="{https://www.omg.org/spec/DMN/20191111/MODEL/}tNamedElement">
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tDRGElement", namespace = "https://www.omg.org/spec/DMN/20191111/MODEL/")
@XmlSeeAlso({
    TKnowledgeSource.class,
    TInputData.class,
    TDecision.class,
    TInvocable.class
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2023-03-02T02:21:07+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class TDRGElement
    extends TNamedElement
{


}
