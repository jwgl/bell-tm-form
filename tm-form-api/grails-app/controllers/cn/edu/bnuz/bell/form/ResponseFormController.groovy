package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.ServiceExceptionHandler

class ResponseFormController implements ServiceExceptionHandler {
    ResponseFormService responseFormService

    def show(String respondentId, String respondentQuestionnaireId) {
        respond responseFormService.getForm(respondentId, respondentQuestionnaireId)
    }

    def save(String respondentId, Long respondentQuestionnaireId, Boolean submit) {
        def cmd = new ResponseCommand()
        bindData(cmd, request.JSON)
        def result = responseFormService.create(respondentId, respondentQuestionnaireId, cmd, submit)
        if (result) {
            respond result, view: 'show'
        } else {
            renderOk()
        }
    }

    def update(String respondentId, Long respondentQuestionnaireId, Boolean submit) {
        def cmd = new ResponseCommand()
        bindData(cmd, request.JSON)
        def result = responseFormService.update(respondentId, respondentQuestionnaireId, cmd, submit)
        if (result) {
            respond result, view: 'show'
        } else {
            renderOk()
        }
    }
}
