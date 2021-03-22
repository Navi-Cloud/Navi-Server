package com.navi.server.component

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("navi")
class FileConfigurationComponent {
    lateinit var serverRoot: String
}