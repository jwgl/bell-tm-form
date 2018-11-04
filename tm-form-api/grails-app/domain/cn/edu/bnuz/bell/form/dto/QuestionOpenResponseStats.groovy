package cn.edu.bnuz.bell.form.dto

class QuestionOpenResponseStats implements Serializable {
    /**
     * 问卷ID
     */
    Long questionnaireId

    /**
     * 问题ID
     */
    Long questionId

    /**
     * 用户输入
     */
    String textValue

    /**
     * 计数
     */
    Long responseCount

    static mapping = {
        table schema: 'tm_form', name: 'dv_question_open_response_stats'
        id    composite: ['questionnaireId', 'questionId', 'textValue']
    }

    boolean equals(other) {
        if (!(other instanceof QuestionOpenResponseStats)) {
            return false
        }

        other.questionnaireId == questionnaireId && other.questionId == questionId && other.textValue == textValue
    }

    int hashCode() {
        Objects.hash(questionnaireId, questionId, textValue)
    }
}
