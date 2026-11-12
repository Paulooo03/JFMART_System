package com.jfmart.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    // Opening the server address in a browser lands on the admin login page
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/", "/admin/index.html")
        registry.addRedirectViewController("/admin", "/admin/index.html")
        registry.addRedirectViewController("/admin/", "/admin/index.html")
    }
}
