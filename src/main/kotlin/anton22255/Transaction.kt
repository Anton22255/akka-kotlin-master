package ru.anton22255

import java.util.*

class Transaction(val originator: String, var announcer: String?, val content: String, val signature: ByteArray?) {
    var id: Long = 0

    init {
        this.id = counter++
    }

    override fun toString(): String {
        return "ID: $id, Announcer: $announcer, Originator: $originator, Content: $content"
    }

    override fun equals(obj: Any?): Boolean {
        return id == (obj as Transaction).id
    }

    override fun hashCode(): Int {
        var result = originator.hashCode()
        result = 31 * result + (announcer?.hashCode() ?: 0)
        result = 31 * result + content.hashCode()
        result = 31 * result + Arrays.hashCode(signature)
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {
        private var counter: Long = 1
    }
}
