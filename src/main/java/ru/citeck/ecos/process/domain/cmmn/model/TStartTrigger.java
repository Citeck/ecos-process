
package ru.citeck.ecos.process.domain.cmmn.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tStartTrigger complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tStartTrigger">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/MODEL}tCmmnElement">
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tStartTrigger", namespace = "http://www.omg.org/spec/CMMN/20151109/MODEL")
@XmlSeeAlso({
    TPlanItemStartTrigger.class,
    TCaseFileItemStartTrigger.class
})
public abstract class TStartTrigger
    extends TCmmnElement
{


}
