package com.jfmart.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JfMartApplication

fun main(args: Array<String>) {
    runApplication<JfMartApplication>(*args)
}
