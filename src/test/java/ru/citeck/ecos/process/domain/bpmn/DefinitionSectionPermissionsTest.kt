package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.YamlUtils
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.permissions.dto.PermissionLevel
import ru.citeck.ecos.model.lib.permissions.dto.PermissionsDef
import ru.citeck.ecos.model.lib.type.dto.TypePermsDef
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.perms.registry.TypePermissionsRegistry
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPerms
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsApi
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsData
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class DefinitionSectionPermissionsTest {

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var permsRegistry: TypePermissionsRegistry

    private var previousModelPerms: TypePermsDef? = null
    private var modelPermsId: String? = null

    @MockBean
    private lateinit var customPermsApi: CustomRecordPermsApi

    private val grants = HashMap<EntityRef, CustomRecordPerms>()
    private val sections = ArrayList<EntityRef>()

    @BeforeEach
    fun setUp() {
        `when`(customPermsApi.getPerms(any())).thenAnswer { grants[it.arguments[0]] ?: CustomRecordPerms.EMPTY }
    }

    @ParameterizedTest
    @CsvSource(
        "BPMN, GROUP_bp-administrator",
        "BPMN, GROUP_bp-developer",
        "BPMN, GROUP_SECTION_USER",
        "DMN, GROUP_bp-administrator",
        "DMN, GROUP_bp-developer",
        "DMN, GROUP_SECTION_USER"
    )
    fun `section grants reach definitions in addition to model permissions`(sectionType: SectionType, group: String) {
        val id = "section-perms-${UUID.randomUUID()}"
        val section = sectionType.getRef(id)
        val source = if (sectionType == SectionType.BPMN) "bpmn-def" else "dmn-def"
        val definition = EntityRef.create(EprocApp.NAME, source, id)
        val typeRef = ModelUtils.getTypeRef(if (sectionType == SectionType.BPMN) "bpmn-process-def" else "dmn-def")
        previousModelPerms = permsRegistry.getPermissionsForType(typeRef)
        modelPermsId = previousModelPerms?.id ?: "model-$id"
        // Model rights allow editing, while deployment must come from section settings.
        permsRegistry.setValue(
            modelPermsId!!,
            TypePermsDef.create {
                withId(modelPermsId!!)
                withTypeRef(typeRef)
                withPermissions(
                    PermissionsDef.create {
                        withMatrix(
                            mapOf(
                                "bp-administrator" to mapOf("ANY" to PermissionLevel.WRITE),
                                "bp-developer" to mapOf("ANY" to PermissionLevel.WRITE)
                            )
                        )
                    }
                )
            }
        )
        // Simulate the effective inherited settings returned by emodel using shipped ROOT grants.
        val settings = DataValue.create(
            YamlUtils.read(
                ResourceUtils.getFile("classpath:eapps/artifacts/model/permission-settings/${sectionType.sourceId}-root-settings.yml")
                    .readText()
            )
        )
        grants[section] = CustomRecordPerms(
            settings["settings"].asList(DataValue::class.java).map { setting ->
                CustomRecordPermsData(
                    setting["permissions"].asStrList().mapTo(LinkedHashSet()) { EntityRef.valueOf(it).getLocalId() },
                    setting["authorities"].asStrList().toSet(),
                    emptySet()
                )
            }
        )
        if (group == "GROUP_SECTION_USER") {
            // This actor has no model role: all visibility/edit/publication rights come from its section.
            grants[section] = CustomRecordPerms(
                listOf(CustomRecordPermsData(setOf("read", "write", sectionType.deployPermissionId), setOf(group), emptySet()))
            )
        }
        val resource = if (sectionType == SectionType.BPMN) {
            "test/bpmn/simple-vaild-process.bpmn.xml"
        } else {
            "test/dmn/simple-dmn-test.dmn.xml"
        }
        val originalId = if (sectionType == SectionType.BPMN) "simple-vaild-process" else "simple-dmn-test"
        val definitionXml = ResourceUtils.getFile("classpath:$resource").readText()
            .replace(originalId, id)
            .replace("eproc/${sectionType.sourceId}@DEFAULT", section.toString())
        AuthContext.runAsSystem {
            recordsService.create("${sectionType.sourceId}-repo", mapOf("id" to id, "name" to MLText(id)))
            sections.add(EntityRef.create(EprocApp.NAME, "${sectionType.sourceId}-repo", id))
            val created = recordsService.mutate(
                RecordAtts(EntityRef.create(EprocApp.NAME, source, "")).apply {
                    this[if (sectionType == SectionType.BPMN) "processDefId" else "defId"] = id
                    this["sectionRef"] = section
                    this["definition"] = definitionXml
                    this["action"] = "DEPLOY"
                }
            )
            assertThat(created).isEqualTo(definition)
            assertThat(recordsService.getAtt(definition, "sectionRef?id").asText()).isEqualTo(section.toString())
        }
        AuthContext.runAs("section-user", listOf(group)) {
            val permission = "permissions._has.${sectionType.deployPermissionId}?bool!"
            assertThat(recordsService.getAtt(section, permission).asBoolean()).isTrue()
            assertThat(recordsService.getAtt(definition, permission).asBoolean()).isTrue()
            assertThat(recordsService.getAtt(definition, "name").asText()).isNotBlank()
            recordsService.mutate(
                RecordAtts(definition).apply {
                    this["definition"] = definitionXml
                    this["action"] = "DEPLOY"
                }
            )
        }
        AuthContext.runAs("unrelated-user", emptyList()) {
            assertThat(recordsService.getAtt(definition, "permissions._has.${sectionType.deployPermissionId}?bool!").asBoolean())
                .isFalse()
        }
    }

    @AfterEach
    fun tearDown() {
        AuthContext.runAsSystem {
            helper.cleanDeployments()
            helper.cleanDefinitions()
            sections.forEach { recordsService.delete(it) }
            modelPermsId?.let { id ->
                val previous = previousModelPerms
                if (previous == null) {
                    permsRegistry.setValue(id, null)
                } else {
                    permsRegistry.setValue(id, previous)
                }
            }
        }
    }
}
