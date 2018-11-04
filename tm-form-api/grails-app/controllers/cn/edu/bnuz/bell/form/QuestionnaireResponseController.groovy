package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.ServiceExceptionHandler

class QuestionnaireResponseController implements ServiceExceptionHandler {
    QuestionnaireResponseService questionnaireResponseService

    def index(String pollsterId, Long questionnaireFormId) {
        respond questionnaireResponseService.getQuestionnaireResponseStats(pollsterId, questionnaireFormId)
    }

    def respondents(String pollsterId, Long questionnaireFormId) {
        renderJson questionnaireResponseService.getQuestionnaireRespondents(pollsterId, questionnaireFormId)
    }

    def openResponses(String pollsterId, Long questionnaireFormId, Long questionId) {
        renderJson questionnaireResponseService.getQuestionOpenResponses(pollsterId, questionnaireFormId, questionId)
    }
}
