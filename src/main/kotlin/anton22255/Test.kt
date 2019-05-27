package anton22255

import ru.anton22255.Main
import ru.anton22255.User
import ru.anton22255.User.Companion.generateNonce
import java.math.BigInteger
import java.util.*

object Test {


    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val countAll = 1000
        val fold = (1..countAll).fold(BigInteger.ZERO) { acc, index ->
            var nonce: String
            val random = Random()
            var count = 0
            do {
                nonce = generateNonce(random)
                count++
            } while (!nonce.startsWith("000"))

            println("count $count, nonce $nonce")

            return@fold acc.plus(BigInteger.valueOf(count.toLong()))
        }

        val divide = fold.divide(BigInteger.valueOf(countAll.toLong()))
        println("result ${divide}")
    }

}