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
        respond responseFormService.create(respondentId, respondentQuestionnaireId, cmd, submit), view: 'show'
    }

    def update(String respondentId, Long respondentQuestionnaireId) {
        def cmd = new ResponseCommand()
        bindData(cmd, request.JSON)
        respond responseFormService.update(respondentId, respondentQuestionnaireId, cmd), view: 'show'
    }

    def patch(String respondentId, Long respondentQuestionnaireId) {
        respond responseFormService.submit(respondentId, respondentQuestionnaireId), view: 'show'
    }
}
