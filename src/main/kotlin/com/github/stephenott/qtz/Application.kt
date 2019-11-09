package com.github.stephenott.qtz

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("com.github.stephenott.qtz")
                .mainClass(Application.javaClass)
                .start()
    }
}