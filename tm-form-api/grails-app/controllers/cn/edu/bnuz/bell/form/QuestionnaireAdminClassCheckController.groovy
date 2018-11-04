package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_QUESTIONNAIRE_ADMIN_CLASS_CHECK")')
class QuestionnaireAdminClassCheckController implements ServiceExceptionHandler {
    QuestionnaireAdminClassCheckService questionnaireAdminClassCheckService

    def index(String supervisorId, ListCommand cmd) {
        renderJson questionnaireAdminClassCheckService.list(supervisorId, cmd)
    }

    def show(String supervisorId, Long questionnaireAdminClassCheckId, String id, String type) {
        ListType listType = ListType.valueOf(type)
        if (id == 'undefined') {
            respond questionnaireAdminClassCheckService.getFormForCheck(supervisorId, questionnaireAdminClassCheckId, listType)
        } else {
            respond questionnaireAdminClassCheckService.getFormForCheck(supervisorId, questionnaireAdminClassCheckId, listType, UUID.fromString(id))
        }
    }

    def patch(String supervisorId, Long questionnaireAdminClassCheckId, String id, String op) {
        def operation = Event.valueOf(op)
        switch (operation) {
            case Event.ACCEPT:
                def cmd = new AcceptCommand()
                bindData(cmd, request.JSON)
                cmd.id = questionnaireAdminClassCheckId
                respond questionnaireAdminClassCheckService.accept(supervisorId, cmd, UUID.fromString(id)), view: 'show'
                break
            case Event.REJECT:
                def cmd = new RejectCommand()
                bindData(cmd, request.JSON)
                cmd.id = questionnaireAdminClassCheckId
                respond questionnaireAdminClassCheckService.reject(supervisorId, cmd, UUID.fromString(id)), view: 'show'
                break
            default:
                throw new BadRequestException()
        }
    }
}
