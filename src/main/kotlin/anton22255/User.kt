package ru.anton22255

import akka.actor.AbstractActor
import akka.actor.ActorRef
import anton22255.Peers
import anton22255.cipher.CipherHelpers
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.*
import java.util.*
import ru.anton22255.Main.startNonce
import ru.anton22255.utils.appendToLogs

class User(val name: String?) : AbstractActor() {

    val transactions: ArrayList<Transaction>
    val ledger: Ledger
    private var privateKey: PrivateKey? = null
    var publicKeyDSA: PublicKey? = null
        private set
    internal var cache: ArrayList<ArrayList<Block>>

    var peers: List<ActorRef>? = null

    override fun createReceive(): Receive {
        return receiveBuilder()
                .match(ProposedBlock::class.java) { block -> handleProposedBlock(block) }
                .match(Transaction::class.java) { transaction -> receiveTransaction(transaction) }
                .match(Peers::class.java) { this.peers = it.peers }
                .match(Command::class.java) { it.run(this) }
                .build();
    }

    init {
        this.transactions = ArrayList()
        this.ledger = Ledger()
        this.cache = ArrayList()
        generateKeys()
        initCache(5)

        //uncomment this segment to have peers with public key modulus and exponent as name //TODO: uncomment
        //		RSAPublicKey rspk=(RSAPublicKey)  publicKey;
        //		this.name=rspk.getModulus().toString()+rspk.getPublicExponent().toString();
    }

    //    public void appendToLogs(String text) throws IOException {
    //        text = "\n" + text + "\n";
    //        Files.write(Paths.get("logs.txt"), text.getBytes(), StandardOpenOption.APPEND);
    //    }

    fun initCache(size: Int) {
        for (i in 0 until size) {
            cache.add(ArrayList())
        }
    }

    fun generateKeysDSA() {
        val kg: KeyPairGenerator
        try {
            kg = KeyPairGenerator.getInstance("DSA")
            val keyPair = kg.genKeyPair()
            privateKey = keyPair.private
            publicKeyDSA = keyPair.public
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

    }

    @Throws(Exception::class)
    fun signStringDSA(content: String): ByteArray {
        val sign = Signature.getInstance("DSA")
        sign.initSign(privateKey)
        sign.update(content.toByteArray())
        return sign.sign()
    }

    @Throws(Exception::class)
    fun verifySignatureDSA(signature: ByteArray, content: String, pk: PublicKey): Boolean {
        val sign = Signature.getInstance("DSA")
        sign.initVerify(pk)
        sign.update(content.toByteArray())
        return sign.verify(signature)
    }

    private fun generateKeys() { //RSA
        try {
            val keyPair = CipherHelpers.generateKeyPair()
            privateKey = keyPair.private
            publicKeyDSA = keyPair.public
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun verifySignature(signature: ByteArray?, content: String, pk: PublicKey?): Boolean {
        val sign = Signature.getInstance("SHA256withRSA")
        sign.initVerify(pk)
        sign.update(content.toByteArray())
        return sign.verify(signature)
    }

    fun getPublicKey(): PublicKey? {
        return publicKeyDSA
    }

    fun randomInt(max: Int): Int {
        val r = Random()
        return r.nextInt(max)
    }

    fun selectTargetPeers(): List<ActorRef>? = peers
//            peers?.takeIf { it.isNotEmpty() }
//                    ?.shuffled()
//                    ?.take(randomInt(peers?.size ?: 0))


//            {
//        val nearPeers = peers
//        val nearPeersCopy = ArrayList<User>()
//        nearPeersCopy.addAll(nearPeers!!)
//
//        val targets = ArrayList<User>()
//        if (nearPeers.size > 0) {
//            val targetsCount = randomInt(nearPeers.size)
//
//            for (i in 0..targetsCount) {
//                val targetIndex =
//                targets.add(nearPeersCopy[targetIndex])
//                nearPeersCopy.removeAt(targetIndex)
//            }
//        }
//
//        return targets
//    }

    private fun getOriginatorPublicKey(transaction: Transaction): PublicKey? {
        val originatorName = transaction.originator
        //todo
//        for (u in Main.usersList) {
//            if (u.name == originatorName) {
//                return u.getPublicKey()
//            }
//        }
        return null
    }

    @Throws(Exception::class)
    fun announceTransaction(transaction: Transaction) {
        selectTargetPeers()?.let { targets ->
            appendToLogs(name + " : Announcing transaction " + transaction.id + " to " + targets)

            for (current in targets) {
                current.tell(transaction, self)
//                this.self.tell(transaction, current)
//                current.receiveTransaction(transaction)
            }
        }
    }

    @Throws(Exception::class)
    fun receiveTransaction(transaction: Transaction) {
        if (!transactions.contains(transaction)) {
            // Receive and see if a block can be formed
            val pk = getOriginatorPublicKey(transaction)
            appendToLogs("User " + name + " authenticating transaction " + transaction.id + " first before accepting and announcing to peers")
            //            System.out.print(".");
//            if (verifySignature(transaction.signature, transaction.content, pk))
//            {
            appendToLogs("Authentication successful, user " + name + " will add transaction " + transaction.id + " and forward to peers")
            transactions.add(transaction)

            if (transactions.size.toLong() == ledger.blocksize) {
                createBlock()
                appendToLogs("User $name created a block and appended it to the ledger")
            }

            // Forward to some near peers
            transaction.announcer = this.name
            announceTransaction(transaction)
//            }
//            else {
//                appendToLogs("Authentication failed therfore user " + name + " will not forward to peers transaction" + transaction.id)
//            }
        } else {
            appendToLogs("Transaction " + transaction.id + " already in " + name + " so not added or announced from user " + name)
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun generateNonce(): String {
        val dateTimeNameString = java.lang.Long.toString(Date().time) + name!! //in-order to be very unique and assured not in ledger
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(dateTimeNameString.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    @Throws(NoSuchAlgorithmException::class)
    fun generateHash(nonce: String): String {
        val hashStr = transactions.toString() + ledger.lastBlock().hash + nonce //TODO: handle genesis block
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(hashStr.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    fun authenticateBlockTransactions(block: Block): Boolean? {
        for (transaction in block.transactions) {
            val pk = getOriginatorPublicKey(transaction)
            try {
                if (!verifySignature(transaction.signature, transaction.content, pk))
                //if verification false
                    return false
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

        }
        return true
    }

    //verify that the hashing of block is consistent to what the block contains
    fun verifyBlockHash(block: Block): Boolean? {
        val hashStr = block.transactions.toString() + block.prevBlock!!.hash + block.nonce
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(hashStr.toByteArray(StandardCharsets.UTF_8))
            val encoded = Base64.getEncoder().encodeToString(hash)
            return block.hash == encoded
        } catch (e: NoSuchAlgorithmException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return false
        }

    }

    @Throws(IOException::class)
    fun createBlock() {
        try {
            var isContainsNonce = false
            var nonce: String
            do {
                nonce = generateNonce()
                isContainsNonce = ledger.containsNonce(nonce)
            } while (isContainsNonce || nonce.substring(0, 2) != Main.startNonce) //make sure starts with 00 and not in ledger before (puzzle)
            val hash = generateHash(nonce)
            val block = Block(transactions, nonce, hash)
            block.linkPrevBlock(ledger.lastBlock())
            appendBlock(block)
            announceBlock(block)
        } catch (e: NoSuchAlgorithmException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun createBlock(trans: List<Transaction>) {
        try {
            var isContainsNonce = false
            var nonce: String
            do {
                nonce = generateNonce()
                isContainsNonce = ledger.containsNonce(nonce)
            } while (isContainsNonce || !nonce.startsWith(startNonce))// Make sure the nonce starts with 00 and not in ledger before (puzzle)
            val hash = generateHash(nonce)
            val block = Block(trans, nonce, hash)

            block.linkPrevBlock(ledger.lastBlock())
            appendBlock(block)
            announceBlock(block)
        } catch (e: NoSuchAlgorithmException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun announceBlock(block: Block) {
        // If managed to solve a complex puzzle, a user can then announce a block
        val proposedBlock = ProposedBlock(block, this)
        selectTargetPeers()?.forEach { it.tell(proposedBlock, self) }
    }

    fun checkCache(proposedBlock: ProposedBlock) {
        val removeIndexes = ArrayList<Int>()
        for (i in cache.indices) {
            val current_cache = cache[i]
            if (current_cache.isEmpty()) {
                current_cache.addAll(ledger.blocks)
            }

            // Add the proposed block to the current chain if it's possible
            val current_last = current_cache[current_cache.size - 1]
            if (proposedBlock.prevBlock == current_last) {
                current_cache.add(proposedBlock)
            } else {
                Main.incrementForkCount()
            }

            // TODO: 21/04/2019 fork event
            // If the current cache is larger than the ledger, swap them
            if (current_cache.size > ledger.blocks.size) {
                val temp = ArrayList<Block>()
                temp.addAll(ledger.blocks)

                ledger.blocks.clear()
                ledger.blocks.addAll(current_cache)
                current_cache.addAll(temp)

                //                Main.INSTANCE.setForkCount(Main.INSTANCE.getForkCount()+1);
            }

            // Drop the current cache if the ledger is larger with 3 or more blocks
            if (ledger.blocks.size - current_cache.size >= 3) {
                removeIndexes.add(i)
            }
        }

        for (i in removeIndexes.indices) {
            cache.removeAt(removeIndexes[i])
        }
    }

    fun updateLedgerAndCache(proposedBlock: ProposedBlock) {
        appendToLogs("updateLedgerAndCache" + name + " will add block " + proposedBlock.confirmations + " and forward to peers")

        if (ledger.blocks.size >= 1) {
            val last = ledger.blocks[ledger.blocks.size - 1]

            if (proposedBlock.prevBlock == last) {
                appendBlock(proposedBlock)
            }
        }

        if (ledger.blocks.size >= 2) {
            val before_last = ledger.blocks[ledger.blocks.size - 2]

            if (proposedBlock.prevBlock == before_last) {
                checkCache(proposedBlock)
            }
        }

        if (ledger.blocks.size >= 3) {
            val before_before_last = ledger.blocks[ledger.blocks.size - 3]

            if (proposedBlock.prevBlock == before_before_last) {
                checkCache(proposedBlock)
            }
        }
    }

    @Throws(IOException::class)
    fun handleProposedBlock(proposedBlock: ProposedBlock) {
        val proposerName = proposedBlock.proposer.name
        appendToLogs("$name : Received a block from $proposerName. Verifying it.")

        // Verify that the hashing of block is consistent with the contents of the block first, if not it will be ignored
//        if (verifyBlockHash(proposedBlock)!!)
//        {
        appendToLogs(name!! + " : Sucessfully verified the recieved block")
        // Users do not vote for blocks they proposed
        if (proposerName == name) {
            appendToLogs(name!! + " : Cannot vote since I proposed this block")
            return
        }

        // Users do not vote for blocks they voted for previously
        if (proposedBlock.uniqueVoters.contains(name)) {
            appendToLogs("$name : Cannot vote since I already voted for this block proposed by $proposerName")
            return
        }

        proposedBlock.uniqueVoters.add(name)
        if (proposedBlock.uniqueVoters.size == Main.usersCount - 1) {
            // All peers -except the proposer- voted for the block
            if (proposedBlock.confirmations > proposedBlock.rejections) {
                appendToLogs(proposerName + " : My block is accepted")
                proposedBlock.proposer.transactions.clear()

                // Update all peers to include the accepted block
//                    Main.updateMainLedger(proposedBlock)
//                    Main.updateUsersLedgers()
            } else {
                appendToLogs(proposerName + " : My block is orphaned. Trying again.")
                proposedBlock.proposer.createBlock(proposedBlock.transactions)
            }
        } else {
            if (ledger.canBeAppended(proposedBlock)) {
                proposedBlock.confirmations = proposedBlock.confirmations + 1
                updateLedgerAndCache(proposedBlock)
            } else {
                proposedBlock.rejections = proposedBlock.rejections + 1
            }

            // After voting, pass the block to a random set of near peers
            selectTargetPeers()?.forEach {
                it.tell(proposedBlock, self)
            }
        }
//        }
//        else {
//            appendToLogs(name!! + " : Ignored block since computed hash and block's hash are different")
//        }
    }

    override fun equals(obj: Any?): Boolean {
        return (obj as User).name == name
    }

    fun appendBlock(block: Block) {

        ledger.appendBlock(block)
    }

    @Throws(Exception::class)
    fun generateTransaction(): Transaction {
        val rdmStr = generateRandomString(20)
        val sig = signString(rdmStr)
        return Transaction(name!!, name, rdmStr, sig)
    }

    @Throws(Exception::class)
    private fun signString(content: String): ByteArray {
        val sign = Signature.getInstance("SHA256withRSA")
        sign.initSign(privateKey)
        sign.update(content.toByteArray())
        return sign.sign()
    }

    override fun toString(): String {
        return name ?: "Unnamed"
    }

    abstract class Command {
        abstract fun run(user: User)
    }

    companion object {

        fun generateRandomString(length: Long): String {
            val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            val salt = StringBuilder()
            val rnd = Random()
            while (salt.length < length) {
                val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
                salt.append(SALTCHARS[index])
            }

            return salt.toString()
        }
    }
}
