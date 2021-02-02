package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.model.omg.TUserEventListener
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.listener.UserEventListenerDef
import javax.xml.namespace.QName

class UserEventListenerConverter : EcosOmgConverter<UserEventListenerDef, TUserEventListener> {

    companion object {
        const val TYPE = "UserEventListener"

        val PROP_AUTHORIZED_ROLES = QName(CmmnXmlUtils.NS_ECOS, "authorizedRoles")
    }

    override fun import(element: TUserEventListener, context: ImportContext): UserEventListenerDef {

        val roles = element.otherAttributes[PROP_AUTHORIZED_ROLES] ?: "[]"
        val rolesList = Json.mapper.readList(roles, String::class.java)

        return UserEventListenerDef(rolesList)
    }

    override fun export(element: UserEventListenerDef, context: ExportContext): TUserEventListener {

        val listener = TUserEventListener()
        listener.authorizedRoleRefs.add("role")
        listener.otherAttributes[PROP_AUTHORIZED_ROLES] = Json.mapper.toString(element.authorizedRoles)

        return listener
    }

    override fun getElementType() = TYPE
}
