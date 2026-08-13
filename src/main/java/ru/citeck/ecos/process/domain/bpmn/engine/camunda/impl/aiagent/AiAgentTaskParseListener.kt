package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aiagent

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask.AbstractAiTaskScriptsParseListener
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_AI_AGENT

@Component
class AiAgentTaskParseListener : AbstractAiTaskScriptsParseListener(ECOS_TASK_AI_AGENT)
