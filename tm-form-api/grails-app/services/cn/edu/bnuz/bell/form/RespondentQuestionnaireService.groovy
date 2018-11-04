package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.form.dto.StudentQuestionnaire
import cn.edu.bnuz.bell.form.dto.TeacherQuestionnaire
import grails.gorm.transactions.Transactional

@Transactional
class RespondentQuestionnaireService {

    def listByStudent(String studentId) {
        StudentQuestionnaire.findAvailableQuestionnaires(studentId)
    }

    def listByTeacher(String teacherId) {
        TeacherQuestionnaire.findAvailableQuestionnaires(teacherId)
    }

    def findSubmitted(String userId) {
        ResponseForm.executeQuery '''
select new Map(
  q.hashId as hashId,
  p.name as pollster,
  d.name as department,
  q.title as title,
  case
    when length(q.prologue) > 97 then concat(substring(q.prologue, 1, 97), '...')
    else q.prologue
  end as prologue,
  q.surveyType as surveyType,
  q.anonymous as anonymous,
  q.datePublished as datePublished,
  q.dateExpired as dateExpired,
  q.dateSubmitted as dateSubmitted,
  form.id as formId
)
from ResponseForm form
join form.questionnaire q
join q.pollster p
join q.department d
where form.respondent.id = :userId
and form.dateSubmitted is not null
order by form.dateSubmitted desc
''', [userId: userId]
    }
}
