package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

data class UserDto(
    var id: String? = null,

    @AttName("userName")
    var userName: String? = null,

    @AttName("firstName")
    var firstName: String? = null,

    @AttName("lastName")
    var lastName: String? = null,

    @AttName("middleName")
    var middleName: String? = null,

    @AttName("?disp")
    var displayName: String? = null
)
