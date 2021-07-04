package com.navi.server.service

import org.springframework.stereotype.Service
import java.nio.charset.Charset
import java.util.*

@Service
class PathService {

    // Base 64 Encode
    private fun encodeString(inputString: String): String {
        return Base64.getEncoder().encodeToString(inputString.toByteArray(Charset.forName("UTF-8")))
    }

    // Base 64 Decode
    private fun decodeString(encodedString: String): String {
        return String(Base64.getDecoder().decode(encodedString.toByteArray()))
    }

    fun getRootToken(): String = encodeString("/")

    // Append input file name to prevToken and return encoded token.
    fun appendPath(fileName: String, prevToken: String): String {
        val decodedPrevPath: String = decodeString(prevToken)
        val pathSplit: MutableList<String> = decodedPrevPath.split("/").toMutableList().apply {
            add(fileName)
        }

        // Handle Root or else - case
        val fullPath: String = if (decodedPrevPath == "/") {
            "/${fileName}"
        } else {
            pathSplit.joinToString(
                separator = "/"
            )
        }

        return encodeString(fullPath)
    }
}