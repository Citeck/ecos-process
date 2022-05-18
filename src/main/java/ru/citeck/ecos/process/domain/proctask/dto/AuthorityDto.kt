package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

data class AuthorityDto(
    var id: String? = null,

    @AttName("cm:authorityName")
    var authorityName: String? = null,

    @AttName("cm:authorityName")
    var userName: String? = null,

    @AttName("cm:firstName")
    var firstName: String? = null,

    @AttName("cm:lastName")
    var lastName: String? = null,

    @AttName("cm:middleName")
    var middleName: String? = null,

    @AttName(".disp")
    var displayName: String,

    @AttName("containedUsers")
    var containedUsers: MutableList<UserDto> = mutableListOf()
)

