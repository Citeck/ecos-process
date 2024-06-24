package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.domain.proctask.attssync.*
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ProcTaskAttsSyncTypesMappingTest {

    @Test
    fun `get types to attributes mapping from sync attributes test`() {
        val attributes = listOf(
            TaskSyncAttribute(
                id = "atts_1",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@type1"),
                        attribute = "att_1_from_type1",
                    ),
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@type2"),
                        attribute = "att_1_from_type2",
                    ),
                ),
            ),
            TaskSyncAttribute(
                id = "atts_2",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@type1"),
                        attribute = "att_2_from_type1",
                    ),
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@type3"),
                        attribute = "att_2_from_type3",
                    ),
                ),
            ),
            TaskSyncAttribute(
                id = "atts_3",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@type3"),
                        attribute = "att_3_from_type3",
                    )
                ),
            ),
            TaskSyncAttribute(
                id = "atts_4",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@type3"),
                        attribute = "att_4_from_type3",
                    )
                ),
            ),
        )

        val settings = listOf(
            TaskAttsSyncSettingsMeta(
                id = EntityRef.valueOf("sync-record"),
                enabled = true,
                name = "test",
                source = TaskAttsSyncSource.RECORD,
                attributesSync = attributes
            ),
            TaskAttsSyncSettingsMeta(
                id = EntityRef.valueOf("sync-type"),
                enabled = true,
                name = "test",
                source = TaskAttsSyncSource.TYPE,
                attributesSync = listOf(
                    TaskSyncAttribute(
                        id = "atts_type_1",
                        type = AttributeType.TEXT,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = EntityRef.valueOf("emodel/type@type1"),
                                recordExpressionAttribute = "att_1_from_type1",
                            ),
                            TaskSyncAttributeType(
                                typeRef = EntityRef.valueOf("emodel/type@type2"),
                                recordExpressionAttribute = "att_1_from_type2",
                            )
                        )
                    ),
                    TaskSyncAttribute(
                        id = "atts_type_3",
                        type = AttributeType.TEXT,
                        ecosTypes = listOf(
                            TaskSyncAttributeType(
                                typeRef = EntityRef.valueOf("emodel/type@type3"),
                                recordExpressionAttribute = "att_3_from_type3",
                            )
                        )
                    )
                )
            )
        )

        val typesToAttributes = settings.toTypesByAttributes()

        val expected = mapOf(
            EntityRef.valueOf("emodel/type@type1") to mapOf(
                "atts_1" to "att_1_from_type1",
                "atts_2" to "att_2_from_type1",
                "atts_type_1" to "att_1_from_type1"
            ),
            EntityRef.valueOf("emodel/type@type2") to mapOf(
                "atts_1" to "att_1_from_type2",
                "atts_type_1" to "att_1_from_type2"
            ),
            EntityRef.valueOf("emodel/type@type3") to mapOf(
                "atts_2" to "att_2_from_type3",
                "atts_3" to "att_3_from_type3",
                "atts_4" to "att_4_from_type3",
                "atts_type_3" to "att_3_from_type3"
            )
        )

        assertThat(typesToAttributes).containsExactlyInAnyOrderEntriesOf(expected)
    }
}
