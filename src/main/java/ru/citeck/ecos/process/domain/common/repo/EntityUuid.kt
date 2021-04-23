package ru.citeck.ecos.process.domain.common.repo

import java.util.*

class EntityUuid {

    var tnt: Int = -1
    lateinit var id: UUID

    constructor()

    constructor(tnt: Int, id: UUID) {
        this.tnt = tnt
        this.id = id
    }
}
