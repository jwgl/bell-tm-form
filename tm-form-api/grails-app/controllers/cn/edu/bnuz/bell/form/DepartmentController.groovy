package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.organization.DepartmentService
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize('hasAuthority("PERM_QUESTIONNAIRE_WRITE")')
class DepartmentController {
    DepartmentService departmentService
    AdminClassService adminClassService

    def index(String userType) {
        RespondentType respondentType = RespondentType.valueOf(userType)
        switch (respondentType) {
            case RespondentType.TEACHER:
                renderJson departmentService.getAllDepartments()
                break
            case RespondentType.STUDENT:
                renderJson departmentService.getStudentDepartments()
                break
        }
    }

    def adminClasses(String departmentId) {
        renderJson adminClassService.getAdminClasses(departmentId)
    }
}
