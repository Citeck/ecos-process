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

    override fun toString(): String {
        return "EntityUuid(tnt=$tnt, id=$id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityUuid) return false

        if (tnt != other.tnt) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tnt
        result = 31 * result + id.hashCode()
        return result
    }
}
