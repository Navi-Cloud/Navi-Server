package com.navi.server.error

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus

class ApiError (
    @JsonIgnore
    val status : HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val message: String = ""
        ){
    var statusCode: String = status.value().toString()
    var statusMessage: String = status.reasonPhrase
}