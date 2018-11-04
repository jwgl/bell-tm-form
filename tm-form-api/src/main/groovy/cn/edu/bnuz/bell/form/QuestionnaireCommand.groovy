package cn.edu.bnuz.bell.form

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil
import grails.databinding.BindUsing
import grails.databinding.BindingFormat

class QuestionnaireCommand {
    Long id
    String title

    @BindUsing({ obj, source ->
        source['prologue'] // prevent trim
    })
    String prologue

    @BindUsing({ obj, source ->
        source['epilogue'] // prevent trim
    })
    String epilogue

    SurveyType surveyType
    SurveyScope surveyScope
    RespondentType respondentType

    @BindUsing({ obj, source ->
        JacksonUtil.toJsonNode(source['oriented'].toString())
    })
    JsonNode oriented

    @BindUsing({ obj, source ->
        JacksonUtil.toJsonNode(source['restricted'].toString())
    })
    JsonNode restricted

    Boolean anonymous
    ResponseVisibility responseVisibility

    @BindingFormat("yyyy-MM-dd'T'HH:mm")
    Date dateExpired

    List<Question> addedQuestions
    List<Question> updatedQuestions
    List<Long> removedQuestions

    class Question {
        Long id
        Integer ordinal
        String title
        String content
        Integer type
        Boolean mandatory
        Boolean openEnded
        String openLabel
        Integer minValue
        Integer maxValue
        Integer stepValue

        List<QuestionOption> addedOptions
        List<QuestionOption> updatedOptions
        List<Long> removedOptions

        class QuestionOption {
            Long id
            Integer ordinal
            String content
            String label
            Integer value
        }
    }
}
