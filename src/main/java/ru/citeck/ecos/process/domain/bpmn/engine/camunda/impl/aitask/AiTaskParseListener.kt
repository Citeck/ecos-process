package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_AI

@Component
class AiTaskParseListener : AbstractAiTaskScriptsParseListener(ECOS_TASK_AI)
