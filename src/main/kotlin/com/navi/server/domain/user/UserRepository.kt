package com.navi.server.domain.user

import com.mongodb.client.result.UpdateResult
import com.navi.server.domain.FileObject
import com.navi.server.error.exception.NotFoundException
import com.navi.server.error.exception.UnknownErrorException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class UserTemplateRepository {
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val objectIdField: String = "id"
    private val userIdField: String = "userId"
    private val userNameField: String = "userName"
    private val userEmailField: String = "userEmail"
    private val fileListField: String = "fileList"
    private val fileListTokenField: String = "token"
    private val fileListPrevTokenField: String = "prevToken"

    /**
     * clearAll() : Remove Every entity/collection in DB, including file information
     */
    fun clearAll() {
        mongoTemplate.remove(Query(), User::class.java)
    }

    /**
     * save(user: User): User
     * Save/Update user and return saved/updated user.
     */
    fun save(user: User): User {
        return mongoTemplate.save(user)
    }

    /**
     * findAllUserOnly(): List<User>
     * Find All user, but excluding fileList
     *
     * The reason we exclude fileList is to reduce memory usage if fileList is really large.
     * Therefore, the list it return will have just empty list of fileObject.
     */
    fun findAllUserOnly(): List<User> {
        val findQuery: Query =  Query()
        findQuery.fields().exclude(fileListField)

        return mongoTemplate.find(findQuery, User::class.java)
    }

    /**
     * findAll(): List<User>
     * unlike findAllUserOnly, it returns pure-collection to object. Including fileList.
     *
     * Warning:
     * If each user's fileList is LARGE, avoid using this.
     * To find inside file list, use findAllByPrevToken / findAllByToken instead.
     */
    fun findAll(): List<User> {
        val findQuery: Query =  Query()
        return mongoTemplate.find(findQuery, User::class.java)
    }

    /**
     * findByUserName(inputUserName: String): User?
     * Returns user document[full document] where user name = inputUserName.
     */
    fun findByUserName(inputUserName: String): User {
        val findNameQuery: Query = Query()
        findNameQuery.addCriteria(
            Criteria.where(userNameField).`is`(inputUserName)
        )

        lateinit var user: User
        runCatching {
            mongoTemplate.findOne(findNameQuery, User::class.java)
        }.onSuccess {
            if (it != null) {
                user = it
            } else {
                throw NotFoundException("Cannot find user with username: $inputUserName")
            }
        }

        return user
    }

    /**
     * findByUserId(inputUserId: String): User?
     * Returns user document[full document] where user userId = inputUserId.
     */
    fun findByUserId(inputUserId: String): User {
        val findNameQuery: Query = Query()
        findNameQuery.addCriteria(
            Criteria.where(userIdField).`is`(inputUserId)
        )

        lateinit var user: User
        runCatching {
            mongoTemplate.findOne(findNameQuery, User::class.java)
        }.onSuccess {
            if (it != null) {
                user = it
            } else {
                throw NotFoundException("Cannot find user with userid: $inputUserId")
            }
        }

        return user
    }

    /**
     * findByUserEmail(inputUserEmail: String): User?
     * Returns user document[full document] where user email = inputUserEmail.
     */
    fun findByUserEmail(inputUserEmail: String): User {
        val findEmailQuery: Query = Query()
        findEmailQuery.addCriteria(
            Criteria.where(userEmailField).`is`(inputUserEmail)
        )

        lateinit var user: User
        runCatching {
            mongoTemplate.findOne(findEmailQuery, User::class.java)
        }.onSuccess {
            if (it != null) {
                user = it
            } else {
                throw NotFoundException("Cannot find user with useremail: $inputUserEmail")
            }
        }

        return user
    }
}