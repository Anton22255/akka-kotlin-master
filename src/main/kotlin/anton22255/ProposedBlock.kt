package ru.anton22255

import java.util.ArrayList

class ProposedBlock(block: Block, var proposer: User) : Block(block.transactions, block.nonce, block.hash, block.prevBlock) {
    var uniqueVoters: MutableList<String> = ArrayList()
    var rejectedVote: MutableList<User> = ArrayList()
    var confirmations: Int = 0
    var rejections: Int = 0

}
