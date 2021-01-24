package com.navi.server

import com.navi.server.component.FileConfigurationComponent
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(FileConfigurationComponent::class)
class MainServer

fun main(args: Array<String>) {
    SpringApplication.run(MainServer::class.java, *args)
}