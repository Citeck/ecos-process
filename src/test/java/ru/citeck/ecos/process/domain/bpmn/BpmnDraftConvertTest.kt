package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import java.nio.charset.StandardCharsets

class BpmnDraftConvertTest {

    @Test
    fun `convert draft with invalid ecos state should throw by default`() {
        val definition = ResourceUtils.getFile(
            "classpath:test/bpmn/convert-draft-without-validation.bpmn.xml"
        ).readText(StandardCharsets.UTF_8)

        assertThrows<RuntimeException> {
            BpmnIO.importEcosBpmn(definition)
        }
    }

    @Test
    fun `convert draft with invalid ecos state should not throw if validation disabled`() {
        val definition = ResourceUtils.getFile(
            "classpath:test/bpmn/convert-draft-without-validation.bpmn.xml"
        ).readText(StandardCharsets.UTF_8)

        val result = BpmnIO.importEcosBpmn(definition, false)

        assertThat(result.process).hasSize(1)
        assertThat(result.process[0].flowElements).hasSize(25)
    }
}
