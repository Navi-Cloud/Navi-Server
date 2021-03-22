package com.navi.server.error

import com.navi.server.error.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionController {

    @ExceptionHandler(UnknownErrorException::class)
    fun handleUnknownException(unknownErrorException: UnknownErrorException) : ResponseEntity<ApiError> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    unknownErrorException.message!!
                )
            )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(notFoundException: NotFoundException) : ResponseEntity<ApiError> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiError(
                    HttpStatus.NOT_FOUND,
                    notFoundException.message!!
                )
            )
    }

    @ExceptionHandler(FileIOException::class)
    fun handleIOException(fileIOException: FileIOException) : ResponseEntity<ApiError> {
        // HttpStatus.INTERNAL_SERVER_ERROR is ok for FileIOException ?
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    fileIOException.message!!
                )
            )
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(conflictException: ConflictException): ResponseEntity<ApiError> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ApiError(
                    HttpStatus.CONFLICT,
                    conflictException.message!!
                )
            )
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(forbiddenException: ForbiddenException): ResponseEntity<ApiError> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ApiError(
                    HttpStatus.FORBIDDEN,
                    forbiddenException.message!!
                )
            )
    }
}