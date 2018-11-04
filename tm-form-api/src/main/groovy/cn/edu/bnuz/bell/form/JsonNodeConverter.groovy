package cn.edu.bnuz.bell.form

import com.fasterxml.jackson.databind.JsonNode
import grails.converters.JSON
import grails.plugin.json.builder.JsonGenerator

class JsonNodeConverter implements JsonGenerator.Converter {
    @Override
    boolean handles(Class<?> type) {
        JsonNode.isAssignableFrom(type)
    }

    @Override
    Object convert(Object value, String key) {
        JSON.parse(value.toString())
    }
}
