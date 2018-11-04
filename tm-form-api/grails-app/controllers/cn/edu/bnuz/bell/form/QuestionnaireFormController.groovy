package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.commands.SubmitCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_QUESTIONNAIRE_WRITE")')
class QuestionnaireFormController implements ServiceExceptionHandler {
    QuestionnaireFormService questionnaireFormService
    QuestionnaireReviewerService questionnaireReviewerService

    def index(String pollsterId) {
        def offset = params.int('offset') ?: 0
        def max = params.int('max') ?: 10

        respond questionnaireFormService.list(pollsterId, offset, max)
    }

    def show(String pollsterId, Long id) {
        respond questionnaireFormService.getFormForShow(pollsterId, id)
    }

    def create(String pollsterId) {
        respond questionnaireFormService.getFormForCreate(pollsterId)
    }

    def save(String pollsterId) {
        def cmd = new QuestionnaireCommand()
        bindData(cmd, request.JSON)
        respond questionnaireFormService.create(pollsterId, cmd)
    }

    def edit(String pollsterId, Long id) {
        respond questionnaireFormService.getFormForEdit(pollsterId, id)
    }

    def update(String pollsterId, Long id) {
        def cmd = new QuestionnaireCommand()
        bindData(cmd, request.JSON)
        cmd.id = id
        questionnaireFormService.update(pollsterId, cmd)
        renderOk()
    }

    def delete(String pollsterId, Long id) {
        questionnaireFormService.delete(pollsterId, id)
        renderOk()
    }

    def patch(String pollsterId, Long id, String op) {
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.SUBMIT:
                def cmd = new SubmitCommand()
                bindData(cmd, request.JSON)
                cmd.id = id
                questionnaireFormService.submit(pollsterId, cmd)
                break
            case Event.OPEN:
                questionnaireFormService.publish(pollsterId, id, true)
                break
            case Event.CLOSE:
                questionnaireFormService.publish(pollsterId, id, false)
                break
        }
        renderOk()
    }

    def createOptions(String pollsterId) {
        renderJson questionnaireFormService.getCreateOptions(pollsterId)
    }

    def checkers(String pollsterId, Long questionnaireFormId) {
        renderJson questionnaireReviewerService.getCheckers(questionnaireFormId)
    }
}
