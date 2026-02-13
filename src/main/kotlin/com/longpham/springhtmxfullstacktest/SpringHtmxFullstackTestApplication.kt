package com.longpham.springhtmxfullstacktest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SpringHtmxFullstackTestApplication

fun main(args: Array<String>) {
    runApplication<SpringHtmxFullstackTestApplication>(*args)
}
