package com.navi.server.domain.user

import com.mongodb.client.result.UpdateResult
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

    fun findByPrevToken(inputUserName: String, inputPrevToken: String): AggregationResults<User>? {
        return innerFileListSearch(inputUserName, "$fileListField.$fileListPrevTokenField", inputPrevToken)
    }

    fun findByToken(inputUserName: String, inputToken: String): AggregationResults<User>? {
        return innerFileListSearch(inputUserName, "$fileListField.$fileListTokenField", inputToken)
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