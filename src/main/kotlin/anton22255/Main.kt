package ru.anton22255


import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import ru.anton22255.utils.appendToFinalLogs
import ru.anton22255.utils.appendToLogs
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit

object Main {
    //    lateinit var networkGraph: Hashtable<String, ArrayList<User>>
    lateinit var usersList: ArrayList<ActorRef>
    lateinit var mainLedger: Ledger
    var usersCount: Int = 0

    var nameOfTest = "test"

    var forkCount: Int = 0

    fun incrementForkCount() {
        forkCount++
        printMessage("fork count ${forkCount}")
    }

    const val startNonce = "00"

    fun randomInt(min: Int, max: Int): Int {
        val r = Random()
        return r.nextInt(max - min + 1) + min
    }

//    fun updateUsersLedgers() {
//
//        appendToLogs("Update Users Ledgers")
//        val mostRecentBlock = mainLedger.lastBlock()
//
//        for (user in usersList) {
//            val currentLastBlock = user.ledger.lastBlock()
//
//            if (currentLastBlock.hash != mostRecentBlock.hash) {
//                // Current last is not the most recent block
//                user.appendBlock(mostRecentBlock)
//            }
//        }
//    }

    fun updateMainLedger(block: Block) {
        mainLedger.appendBlock(block)
        appendToLogs("Update Main ledger ${mainLedger.blocks.size}")
    }

    @Throws(FileNotFoundException::class)
    fun clearLogs() {
        val writer = PrintWriter(File("logs_${nameOfTest}.txt"))
        writer.print("")
        writer.close()
    }

    // To Send a random number of transactions to some random peers
    // Generate users and select their peers and put them in the Hashtable networkGraph

    @Throws(Exception::class)
    fun sendTransactions() {

        val setSize = usersList.size
        val senders = 1000//randomInt(1, 10)
        for (i in 0 until senders) {
            val randInt = randomInt(0, setSize - 1)
            val user = usersList[randInt]
            user.tell(object : User.Command() {
                override fun run(user: User) {
                    user.announceTransaction(user.generateTransaction())
                }
            }, ActorRef.noSender())

//            val tranRand = randomInt(0, 10)
//            for (j in 0 until tranRand) {
//
//                user.tell(object : User.Command() {
//                    override fun run(user: User) {
//                        user.announceTransaction(user.generateTransaction())
//                    }
//                }, ActorRef.noSender())
//            }
        }
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
//        networkGraph = Hashtable()

//        GraphVisualisation.GraphType.values()
//                .forEach { runExperiment(it) }

        runExperiment(GraphType.BarabasiAlbert, 1)

        // To test signatures
        //		 a.announceTransaction(a.generateTransaction());
        //		 System.out.println(a.getTransactions());
        //		 Transaction t=a.getTransactions().get(0);
        //		 System.out.println(a.verifySignature(t.getSignature(),t.getContent(),a.getPublicKey()));

    }

    private fun runExperiment(graphType: GraphType, timeOfExperiment: Int = 1) {
        val finalAdvResult = (1..timeOfExperiment).map {
            phaseOfExperiment(graphType, it)
        }.toIntArray().sum().div(timeOfExperiment)


        val message = "final result ${finalAdvResult}"
        printMessage(message)
    }

    private fun phaseOfExperiment(graphType: GraphType, it: Int): Int {
        nameOfTest = graphType.name + Date().toString()

        val actorSystem = ActorSystem.create("part3", ConfigFactory.parseResources("part3.conf"))
        val peers = graphMapper(generateGraph(10, nameOfTest, graphType), actorSystem)
        val runPhaseOfExperiment = runPhaseOfExperiment(it, peers)
//        Thread.sleep(1000)
        TimeUnit.MINUTES.sleep(1)
        print("Kill all")
//        actorSystem.deadLetters()

        //shutdown the actor system
        printMessage("stop actor system")

        getUserInfo()
        actorSystem.terminate()
        return runPhaseOfExperiment
    }

    private fun printMessage(message: String) {
        appendToFinalLogs(message)
        println(message)
    }

    private fun runPhaseOfExperiment(round: Int, peers: ArrayList<ActorRef>): Int {
        printMessage("start round $round")

        usersList = ArrayList()
        mainLedger = Ledger()
        forkCount = 0
        //        clearLogs()
        //        val k: Int = 10
        //        (0..k).forEach { usersList.add(User(round.toString())) }
        //        usersList = generateNet(usersList, (k * 0.5).toInt())

        // Define total users' count
        usersList = peers
        usersCount = usersList.size
        //        GraphVisualisation().graphExplore(usersList)
        sendTransactions()

        // To test the network with many users announcing many transactions

        printMessage("\nBlocks in main ledger ${mainLedger.blocks.size}")



        printMessage("\nPlease look in logs.txt for output. It gets cleared at the start of every run.")

        printMessage(" count of fork =  ${forkCount}")
        return forkCount
    }

    private fun getUserInfo() {
        val res = StringBuffer()
        usersList.map { user ->
            user.tell(object : User.Command() {
                override fun run(user: User) {
                    print("${user.name} ${user.ledger.blocks.size}  ${user.transactions.size},")
                    res.append("${user.name} ${user.ledger.blocks.size}")
                }
            }, ActorRef.noSender())
        }
        printMessage(res.toString())
    }
}
