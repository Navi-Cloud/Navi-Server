package com.navi.server.domain.user

import com.mongodb.client.result.UpdateResult
import com.navi.server.domain.FileObject
import javassist.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class UserTemplateRepository {
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val userIdField: String = "id"
    private val userNameField: String = "userName"
    private val fileListField: String = "fileList"
    private val fileListTokenField: String = "token"
    private val fileListPrevTokenField: String = "prevToken"

    fun clearAll() {
        mongoTemplate.remove(Query(), User::class.java)
    }

    fun save(user: User): User {
        return mongoTemplate.save(user)
    }

    fun findAllFileList(inputUserName: String): List<FileObject> {
        val user: User = findByUserName(inputUserName) ?: run {
            throw NotFoundException("Cannot find username with: $inputUserName")
        }

        return user.fileList.toList()
    }

    fun innerFileListSearch(
        inputUserName: String,
        inputSearchKey: String,
        inputSearchValue: String
    ): AggregationResults<User> {
        // Match user name [Filter username first]
        val userNameMatchCriteria: Criteria = Criteria.where(userNameField).`is`(inputUserName)
        val matchOperation: MatchOperation = Aggregation.match(userNameMatchCriteria)

        // Unwind
        val unwindOperation: UnwindOperation = Aggregation.unwind(fileListField)

        // Match Token [Filter token]
        val fileTokenMatchCriteria: Criteria = Criteria.where(inputSearchKey).`is`(inputSearchValue)
        val fileTokenMatchOperation: MatchOperation = Aggregation.match(fileTokenMatchCriteria)

        // Group
        val groupOperation: GroupOperation = Aggregation.group(userIdField)
            .push(
                fileListField
            ).`as`(fileListField)

        return mongoTemplate.aggregate(
            Aggregation.newAggregation(
                matchOperation,
                unwindOperation,
                fileTokenMatchOperation,
                groupOperation
            ),
            User::class.java,
            User::class.java
        )
    }

    fun findAllByPrevToken(inputUserName: String, inputPrevToken: String): List<FileObject>? {
        val results: AggregationResults<User> = innerFileListSearch(inputUserName, "$fileListField.$fileListPrevTokenField", inputPrevToken)

        if (results.mappedResults.size > 1) {
            return null // Error
        }

        return results.mappedResults[0].fileList
    }

    fun findByToken(inputUserName: String, inputToken: String): FileObject? {
        val results: AggregationResults<User> = innerFileListSearch(inputUserName, "$fileListField.$fileListTokenField", inputToken)
        if (results.mappedResults.size > 1) {
            return null
        }

        if (results.mappedResults[0].fileList.size > 1) {
            return null
        }

        return results.mappedResults[0].fileList[0]
    }

    fun findByUserName(inputUserName: String): User? {
        val findNameQuery: Query = Query()
        findNameQuery.addCriteria(
            Criteria.where(userNameField).`is`(inputUserName)
        )
        return mongoTemplate.findOne(findNameQuery, User::class.java)
    }

    fun deleteByToken(inputUserName: String, inputToken: String): UpdateResult {
        val updateQuery: Query = Query().apply {
            addCriteria(
                Criteria.where(userNameField).`is`(inputUserName)
            )
        }

        val update: Update = Update().pull(
            fileListField,
            Query.query(Criteria.where(fileListTokenField).`is`(inputToken))
        )

        return mongoTemplate.updateMulti(updateQuery, update, User::class.java)
    }
}