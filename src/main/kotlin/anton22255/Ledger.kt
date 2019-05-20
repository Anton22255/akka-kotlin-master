package ru.anton22255

import java.util.ArrayList

class Ledger(val blocksize: Long = 5) {
    var blocks: ArrayList<Block> = ArrayList()

    init {
        //adding genesis block in ledger
        val genesisBlock = Block(
                listOf(Transaction("genesis", "genesis", "genesis", null)),
                //                genesisTransactions,
                "genesis", "genesis")
        blocks.add(genesisBlock)
    }

    fun appendBlock(block: Block) {

        val lastBlock = lastBlock()
        blocks.add(block)
        //		System.out.println("lastB:"+lastBlock);
        //		if(lastBlock==null)
        //			lastBlock=block;//if no last block then reference itself ->solved with genesis block
        block.linkPrevBlock(lastBlock)
    }

    fun containsNonce(nonce: String): Boolean {
        for (block in blocks) {
            if (block.nonce == nonce) {
                return true
            }
        }
        return false
    }

    fun lastBlock(): Block {
        //		if(blocks.isEmpty())
        //			return null;// no last block  >solved with genesis block
        //		System.out.println(blocks.size());
        return blocks.last()
    }

    // It determines whether a proposed block can be linked to the last block of this ledger based on its hash
    fun canBeAppended(proposedBlock: ProposedBlock): Boolean {
        return proposedBlock.prevBlock == lastBlock()
    }
}
