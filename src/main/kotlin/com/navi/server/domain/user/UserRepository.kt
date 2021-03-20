package com.navi.server.domain.user

import com.navi.server.domain.FileObject
import com.navi.server.error.exception.NotFoundException
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByUserName(inputUserName: String): User?

    // Custom function
    fun findAllFileObject(inputUserName: String): List<FileObject> {
        val user: User = findByUserName(inputUserName) ?: run {
            throw NotFoundException("Cannot find username with: ${inputUserName}.")
        }

        return user.fileList
    }
}