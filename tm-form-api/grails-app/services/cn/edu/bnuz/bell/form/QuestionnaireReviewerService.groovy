package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.organization.AdminClass
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.security.UserRole
import cn.edu.bnuz.bell.security.UserType
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.ReviewerProvider
import grails.gorm.transactions.Transactional

@Transactional(readOnly = true)
class QuestionnaireReviewerService implements ReviewerProvider {
    List<Map> getReviewers(Object id, String activity) {
        switch (activity) {
            case Activities.CHECK:
                return getCheckers(id as Long)
            case Activities.APPROVE:
                return getApprovers()
            default:
                throw new BadRequestException()
        }
    }

    List<Map> getApprovers() {
        User.findAllWithPermission('PERM_QUESTIONNAIRE_APPROVE')
    }

    def getCheckers(Long id) {
        Questionnaire questionnaire = Questionnaire.get(id)
        String pollsterId = questionnaire.pollsterId as String
        switch (questionnaire.surveyScope) {
            case SurveyScope.SCHOOL:
            case SurveyScope.DEPARTMENT:
                getQuestionnaireDepartmentAdmin(pollsterId)
                break
            case SurveyScope.ADMIN_CLASS:
                getAdminClassSupervisor(pollsterId)
                break
        }
    }

    private List<Map> getQuestionnaireDepartmentAdmin(String userId) {
        UserRole.executeQuery '''
select new map(
  user.id as id,
  user.name as name
)
from UserRole userRole
join userRole.user user
join userRole.role role
where user.departmentId = (
  select departmentId from User where id = :userId
)
and role.id = 'ROLE_QUESTIONNAIRE_DEPT_ADMIN'
''', [userId: userId]
    }

    private List<Map> getAdminClassSupervisor(String userId) {
        User user = User.get(userId)
        switch (user.userType) {
            case UserType.TEACHER: // 教师所做的班级问卷，自身应为班主任
                AdminClass.executeQuery '''
select distinct new map(
  teacher.id as id,
  teacher.name as name
)
from Teacher teacher
where exists(
  select 1
  from AdminClass
  where supervisor.id = :userId
)''', [userId: userId]
                break
            case UserType.STUDENT: // 学生所做的班级问卷，审核人为所在班的班主任
                AdminClass.executeQuery '''
select new map(
    supervisor.id as id,
    supervisor.name as name
)
from Student student
join student.adminClass adminClass
join adminClass.supervisor supervisor
where student.id = :userId
''', [userId: userId]
                break
        }
    }


}
