package com.navi.server

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class MainServer

fun main(args: Array<String>) {
    SpringApplication.run(MainServer::class.java, *args)
}