package ru.citeck.ecos.process.config.camunda

import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component

// TODO: remove
@Component
class TestParseListener(
    private val testListener: TestListener
) : AbstractBpmnParseListener() {

    override fun parseSendTask(sendTaskElement: Element?, scope: ScopeImpl?, activity: ActivityImpl?) {
        super.parseSendTask(sendTaskElement, scope, activity)

        println("====================================")
        println("call from parseSendTask TestParseListener")
        println("====================================")
    }

    override fun parseServiceTask(serviceTaskElement: Element, scope: ScopeImpl?, activity: ActivityImpl?) {
        // super.parseServiceTask(serviceTaskElement, scope, activity)

        println("====================================")
        println("call from parseServiceTask TestParseListener")
        println("====================================")

        val extensionElement = serviceTaskElement.element("extensionElements")
        extensionElement?.let {

            val myElement = it.elements("field")

            val props = it.element("properties")

            if (props != null) {

                val propertiesList = props.elements("property")
                propertiesList.forEach { el ->

                    val name = el.attribute("name")
                    val value = el.attribute("value")

                    val elements = el.elements()

                    println(name)
                }
            }
        }

        activity!!.addListener(ExecutionListener.EVENTNAME_START, testListener)
    }
}
