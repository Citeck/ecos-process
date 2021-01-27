package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.model.TUserEventListener
import ru.citeck.ecos.process.domain.cmmn.service.CmmnUtils
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type.listener.CmmnUserEventListener
import javax.xml.namespace.QName

class UserEventListenerConverter : CmmnConverter<TUserEventListener, CmmnUserEventListener> {

    companion object {
        const val TYPE = "UserEventListener"

        val PROP_AUTHORIZED_ROLES = QName(CmmnUtils.NS_ECOS, "authorizedRoles")
    }

    override fun import(element: TUserEventListener, context: ImportContext): CmmnUserEventListener {

        val roles = element.otherAttributes[PROP_AUTHORIZED_ROLES] ?: "[]"
        val rolesList = Json.mapper.readList(roles, String::class.java)

        return CmmnUserEventListener(rolesList)
    }

    override fun export(element: CmmnUserEventListener, context: ExportContext): TUserEventListener {

        val listener = TUserEventListener()
        listener.otherAttributes[PROP_AUTHORIZED_ROLES] = Json.mapper.toString(element.authorizedRoles)

        return listener
    }

    override fun getElementType() = TYPE
}
