package com.navi.server.error

import com.navi.server.error.exception.InvalidTokenAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionController {

    @ExceptionHandler(InvalidTokenAccessException::class)
    fun handleInvalidToken(invalidTokenAccessException: InvalidTokenAccessException) : ResponseEntity <ApiError> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiError(
                    HttpStatus.BAD_REQUEST,
                    invalidTokenAccessException.message!!
                )
            )
    }

}