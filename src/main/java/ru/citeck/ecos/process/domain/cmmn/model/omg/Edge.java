
package ru.citeck.ecos.process.domain.cmmn.model.omg;

import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for Edge complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Edge">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/spec/CMMN/20151109/DI}DiagramElement">
 *       &lt;sequence>
 *         &lt;element name="waypoint" type="{http://www.omg.org/spec/CMMN/20151109/DC}Point" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
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
@XmlType(name = "Edge", namespace = "http://www.omg.org/spec/CMMN/20151109/DI", propOrder = {
    "waypoint"
})
@XmlSeeAlso({
    CMMNEdge.class
})
public abstract class Edge
    extends DiagramElement
{

    @XmlElement(namespace = "http://www.omg.org/spec/CMMN/20151109/DI")
    protected List<Point> waypoint;

    /**
     * Gets the value of the waypoint property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the waypoint property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWaypoint().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Point }
     *
     *
     */
    public List<Point> getWaypoint() {
        if (waypoint == null) {
            waypoint = new ArrayList<>();
        }
        return this.waypoint;
    }

}
