//package com.navi.server.web
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.navi.server.component.FileConfigurationComponent
//import com.navi.server.domain.user.User
//import com.navi.server.domain.user.UserTemplateRepository
//import com.navi.server.dto.LoginRequest
//import com.navi.server.dto.UserRegisterRequest
//import com.navi.server.dto.UserRegisterResponse
//import com.navi.server.service.UserService
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.http.HttpStatus
//import org.springframework.test.context.junit4.SpringRunner
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
//import org.springframework.test.web.servlet.result.MockMvcResultHandlers
//import org.springframework.test.web.servlet.setup.MockMvcBuilders
//import org.springframework.web.context.WebApplicationContext
//import org.springframework.http.ResponseEntity.status
//import org.assertj.core.api.Assertions.assertThat;
//import org.springframework.http.MediaType
//import java.io.File
//
//@RunWith(SpringRunner::class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class UserApiControllerTest {
//    @Autowired
//    private lateinit var userService: UserService
//
//    @Autowired
//    private lateinit var userTemplateRepository: UserTemplateRepository
//
//    @Autowired
//    private lateinit var objectMapper: ObjectMapper;
//
//    @Autowired
//    private lateinit var webApplicationContext: WebApplicationContext
//
//    @Autowired
//    private lateinit var fileConfigurationComponent: FileConfigurationComponent
//
//    private lateinit var trashRootObject: File
//
//    private lateinit var mockMvc : MockMvc
//
//    @Before
//    fun initEnvironment() {
//        fileConfigurationComponent.serverRoot = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").absolutePath
//        // Create trash directory
//        trashRootObject = File(fileConfigurationComponent.serverRoot)
//        trashRootObject.mkdir()
//        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
//    }
//
//    @After
//    fun clearAllDB() {
//        if (trashRootObject.exists()) {
//            trashRootObject.deleteRecursively()
//        }
//        userTemplateRepository.clearAll()
//    }
//
//    @Test
//    fun testRegister_ok() {
//        // Register User fot test
//        val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
//            userId = "je",
//            userName = "JE",
//            userEmail = "user@gmail.com",
//            userPassword = "userPW"
//        )
//        val content : String = objectMapper.writeValueAsString(userRegisterRequest)
//        mockMvc.perform(
//            MockMvcRequestBuilders.post("/api/navi/join")
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(content)
//        ).andExpect { status(HttpStatus.OK) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
//                val result: UserRegisterResponse = objectMapper.readValue(it.response.contentAsString, UserRegisterResponse::class.java)
//                assertThat(result.registeredId).isEqualTo(userRegisterRequest.userId)
//                assertThat(result.registeredEmail).isEqualTo(userRegisterRequest.userEmail)
//            }
//    }
//
//    @Test
//    fun invalidRegister_conflict(){
//        val userEmail: String = "user@gmail.com"
//        // Already registered user
//        val fastUser: User = User(
//            userId = "je",
//            userName = "JE",
//            userEmail = userEmail,
//            userPassword = "userPW",
//            roles = setOf("ROLE_ADMIN")
//        )
//        userTemplateRepository.save(fastUser)
//
//        // Save Check
//        assertThat(userTemplateRepository.findByUserId(fastUser.userId)).isNotEqualTo(null)
//
//        // Register User that have save email
//        val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(
//            userId = "je",
//            userName = "JE",
//            userEmail = userEmail,
//            userPassword = "userPW"
//        )
//        val requestString : String = objectMapper.writeValueAsString(userRegisterRequest)
//        mockMvc.perform(
//            MockMvcRequestBuilders.post("/api/navi/join")
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(requestString)
//        ).andExpect { status(HttpStatus.CONFLICT) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.CONFLICT.value())
//                //val result: UserRegisterResponse = objectMapper.readValue(it.response.contentAsString, UserRegisterResponse::class.java)
//            }
//    }
//
//    @Test
//    fun testLogin_ok(){
//        // Register User for Test
//        val testUser: User = User(
//            userId = "je",
//            userName = "JE",
//            userEmail = "user@gamil.com",
//            userPassword = "userPW",
//            roles = setOf("ROLE_ADMIN")
//        )
//        userTemplateRepository.save(testUser)
//
//        // login
//        val loginRequest: LoginRequest = LoginRequest(
//            userId = testUser.userId,
//            userPassword = testUser.userPassword
//        )
//        val requestString : String = objectMapper.writeValueAsString(loginRequest)
//        mockMvc.perform(
//            MockMvcRequestBuilders.post("/api/navi/login")
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(requestString)
//        ).andExpect { status(HttpStatus.OK) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.OK.value())
//                assertThat(it.response.contentAsString).isNotEqualTo("")
//            }
//
//    }
//
//    @Test
//    fun invalidLogin_NOTFOUND_no_user(){
//        // Error 1: No User
//
//        // login
//        val loginRequest: LoginRequest = LoginRequest(
//            userId = "none",
//            userPassword = "none"
//        )
//        val requestString : String = objectMapper.writeValueAsString(loginRequest)
//        mockMvc.perform(
//            MockMvcRequestBuilders.post("/api/navi/login")
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(requestString)
//        ).andExpect { status(HttpStatus.NOT_FOUND) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.NOT_FOUND.value())
//            }
//    }
//
//    @Test
//    fun invalidLogin_FORBIDDEN_wrong_password(){
//        // Error 2: Wrong Password
//
//        // Register User for Test
//        val testUser: User = User(
//            userId = "je",
//            userName = "JE",
//            userEmail = "user@gamil.com",
//            userPassword = "userPW",
//            roles = setOf("ROLE_ADMIN")
//        )
//        userTemplateRepository.save(testUser)
//
//        // login
//        val loginRequest: LoginRequest = LoginRequest(
//            userId = testUser.userId,
//            userPassword = "WrongPW"
//        )
//        val requestString : String = objectMapper.writeValueAsString(loginRequest)
//        mockMvc.perform(
//            MockMvcRequestBuilders.post("/api/navi/login")
//                .contentType(MediaType.APPLICATION_JSON_UTF8)
//                .content(requestString)
//        ).andExpect { status(HttpStatus.FORBIDDEN) }
//            .andDo(MockMvcResultHandlers.print())
//            .andDo{
//                assertThat(it.response.status).isEqualTo(HttpStatus.FORBIDDEN.value())
//            }
//    }
//
//}