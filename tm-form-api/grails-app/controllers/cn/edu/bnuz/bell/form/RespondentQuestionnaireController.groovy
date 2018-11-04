package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.security.UserType

class RespondentQuestionnaireController implements ServiceExceptionHandler {
    RespondentQuestionnaireService respondentQuestionnaireService
    SecurityService securityService

    def index(String respondentId, String category) {
        switch (category) {
            case 'open':
                switch (securityService.userType) {
                    case UserType.STUDENT:
                        renderJson respondentQuestionnaireService.listByStudent(respondentId)
                        break
                    case UserType.TEACHER:
                        renderJson respondentQuestionnaireService.listByTeacher(respondentId)
                        break
                }
                break
            case 'submitted':
                renderJson respondentQuestionnaireService.findSubmitted(respondentId)
                break
            default:
                throw new BadRequestException()
        }
    }
}