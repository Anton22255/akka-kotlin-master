package ru.anton22255

open class Block {
    var nonce: String
    var transactions: List<Transaction>
    var hash: String
    var prevBlock: Block? = null
    var prevHash: String? = null

    constructor(transactions: List<Transaction>, nonce: String, hash: String) {
        this.transactions = transactions
        this.nonce = nonce
        this.hash = hash
    }

    constructor(transactions: List<Transaction>, nonce: String, hash: String, prevBlock: Block?) {
        this.transactions = transactions
        this.nonce = nonce
        this.hash = hash
        this.prevBlock = prevBlock
        this.prevHash = prevBlock?.hash
    }

    override fun equals(obj: Any?): Boolean {
        val block = obj as Block?
        return hash == block!!.hash
    }

    fun linkPrevBlock(prev: Block) {
        this.prevBlock = prev
        this.prevHash = prev.hash
    }
}

