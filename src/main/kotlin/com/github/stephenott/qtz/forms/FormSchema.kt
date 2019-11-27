package com.github.stephenott.qtz.forms

data class FormSchema(
        val display:String,
        val components: List<Map<String, Any>>,
        val settings: Map<String, Any>? = null
){}