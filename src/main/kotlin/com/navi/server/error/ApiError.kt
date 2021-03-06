package com.navi.server.error

import org.springframework.http.HttpStatus

class ApiError (
    val status : HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val message: String = ""
        ){
    var statusCode: String = status.value().toString()
    var statusMessage: String = status.reasonPhrase
}