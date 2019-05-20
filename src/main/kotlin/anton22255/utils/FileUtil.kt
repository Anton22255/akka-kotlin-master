package ru.anton22255.utils

import ru.anton22255.Main
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

const val isAllLogPrinting = false
const val isAllLogFilePrinting = true
@Throws(IOException::class)
fun appendToLogs(text: String) {

    if (isAllLogPrinting) println("\n" + text + "\n")

    if (isAllLogFilePrinting) {
        val fileName = "./logs/logs_${Main.nameOfTest}.txt"
        var file = File(fileName)
//     create a new file
        val isNewFileCreated: Boolean = file.createNewFile()
        Files.write(Paths.get(fileName), ("\n" + text + "\n").toByteArray(), StandardOpenOption.APPEND)
    }
}

@Throws(IOException::class)
fun appendToFinalLogs(text: String) {

//    {
    val fileName = "./logs/logs_f_${Main.nameOfTest}.txt"
//     create a new file
    val isNewFileCreated: Boolean = File(fileName).createNewFile()
    Files.write(Paths.get(fileName), ("\n" + text + "\n").toByteArray(), StandardOpenOption.APPEND)
//    }
}

