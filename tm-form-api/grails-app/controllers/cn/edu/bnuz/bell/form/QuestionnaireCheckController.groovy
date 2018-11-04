package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_QUESTIONNAIRE_CHECK")')
class QuestionnaireCheckController implements ServiceExceptionHandler {
    QuestionnaireCheckService questionnaireCheckService
    QuestionnaireReviewerService questionnaireReviewerService

    def index(String checkerId, ListCommand cmd) {
        renderJson questionnaireCheckService.list(checkerId, cmd)
    }

    def show(String checkerId, Long questionnaireCheckId, String id, String type) {
        ListType listType = ListType.valueOf(type)
        if (id == 'undefined') {
            respond questionnaireCheckService.getFormForCheck(checkerId, questionnaireCheckId, listType, Activities.CHECK)
        } else {
            respond questionnaireCheckService.getFormForCheck(checkerId, questionnaireCheckId, listType, UUID.fromString(id))
        }
    }

    def patch(String checkerId, Long questionnaireCheckId, String id, String op) {
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.ACCEPT:
                def cmd = new AcceptCommand()
                bindData(cmd, request.JSON)
                cmd.id = questionnaireCheckId
                respond questionnaireCheckService.accept(checkerId, cmd, UUID.fromString(id)), view: 'show'
                break
            case Event.REJECT:
                def cmd = new RejectCommand()
                bindData(cmd, request.JSON)
                cmd.id = questionnaireCheckId
                respond questionnaireCheckService.reject(checkerId, cmd, UUID.fromString(id)), view: 'show'
                break
            default:
                throw new BadRequestException()
        }
    }

    def approvers(String checkerId, Long questionnaireCheckId) {
        renderJson questionnaireReviewerService.getApprovers()
    }
}
