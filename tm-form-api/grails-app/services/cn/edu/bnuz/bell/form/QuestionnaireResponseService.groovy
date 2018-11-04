package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.form.dto.QuestionOpenResponseStats
import cn.edu.bnuz.bell.form.dto.QuestionnaireResponseStats
import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ForbiddenException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Student
import cn.edu.bnuz.bell.organization.Teacher
import grails.gorm.transactions.Transactional

@Transactional
class QuestionnaireResponseService {
    QuestionnaireFormService questionnaireFormService

    def getQuestionnaireResponseStats(String userId, Long questionnaireId) {
        def questionnaire = questionnaireFormService.getForm(questionnaireId)

        if (!questionnaire) {
            throw new NotFoundException()
        }

        if (questionnaire.pollsterId != userId) {
            throw new ForbiddenException()
        }

        [
                questionnaire: questionnaire,
                stats        : QuestionnaireResponseStats.get(questionnaireId),
        ]
    }

    def getQuestionnaireRespondents(String userId, Long questionnaireId) {
        def questionnaire = Questionnaire.get(questionnaireId)
        if (!questionnaire) {
            throw new NotFoundException()
        }

        if (questionnaire.pollsterId != userId) {
            throw new ForbiddenException()
        }

        if (questionnaire.anonymous) {
            throw new BadRequestException()
        }

        switch (questionnaire.respondentType) {
            case RespondentType.STUDENT:
                return Student.executeQuery('''
select new map(
  student.id as id,
  student.name as name,
  student.sex as sex, 
  department.name as department,
  major.grade as grade,
  subject.name as subject,
  adminClass.name as adminClass
)
from Student student
join student.adminClass adminClass
join student.department department
join student.major major
join major.subject subject,
Questionnaire questionnaire
join questionnaire.responses responseForm
where student.id = responseForm.respondent.id
and questionnaire.id = :questionnaireId
order by department.name, student.id
''', [questionnaireId: questionnaireId])
            case RespondentType.TEACHER:
                return Teacher.executeQuery('''
select new map(
  teacher.id as id,
  teacher.name as name,
  teacher.sex as sex, 
  department.name as department
)
from Teacher teacher
join teacher.department department,
Questionnaire questionnaire
join questionnaire.responses responseForm
where teacher.id = responseForm.respondent.id
and questionnaire.id = :questionnaireId
order by department.name, teacher.id
''', [questionnaireId: questionnaireId])
        }
    }

    def getQuestionOpenResponses(String userId, Long questionnaireId, Long questionId) {
        QuestionOpenResponseStats.executeQuery '''
select new map(
  textValue as value,
  responseCount as count
)
from QuestionOpenResponseStats
where questionnaireId = :questionnaireId
and questionId = :questionId
order by responseCount desc, textValue
''', [questionnaireId: questionnaireId, questionId: questionId]
    }
}
