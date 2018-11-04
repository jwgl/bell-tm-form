package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.service.DataAccessService
import cn.edu.bnuz.bell.workflow.*
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.gorm.transactions.Transactional

import javax.annotation.Resource

@Transactional
class QuestionnaireAdminClassCheckService {
    QuestionnaireFormService questionnaireFormService
    DataAccessService dataAccessService

    @Resource(name = 'questionnaireStateHandler')
    DomainStateMachineHandler domainStateMachineHandler

    protected getCounts(String userId) {
        def todo = dataAccessService.getInteger '''
select count(*)
from Questionnaire form
where form.pollster.id in (
  select student.id
  from Student student
  join student.adminClass adminClass
  where adminClass.supervisor.id = :userId
)
and surveyScope = :scope
and form.status = :status
''', [userId: userId, status: State.SUBMITTED, scope: SurveyScope.ADMIN_CLASS]

        def done = Questionnaire.countByCheckerAndSurveyScope(Teacher.load(userId), SurveyScope.ADMIN_CLASS)

        [
                (ListType.TODO): todo,
                (ListType.DONE): done,
        ]
    }

    def list(String userId, ListCommand cmd) {
        switch (cmd.type) {
            case ListType.TODO:
                return findTodoList(userId, cmd.args)
            case ListType.DONE:
                return findDoneList(userId, cmd.args)
            default:
                throw new BadRequestException()
        }
    }

    def findTodoList(String userId, Map args) {
        def forms = Questionnaire.executeQuery '''
select new map(
  form.id as id,
  pollster.name as pollster,
  form.surveyType as surveyType,
  form.title as title,
  form.respondentType as respondentType,
  form.surveyScope as surveyScope,
  form.dateSubmitted as date,
  form.status as status
)
from Questionnaire form
join form.pollster pollster
where form.pollster.id in (
  select student.id
  from Student student
  join student.adminClass adminClass
  where adminClass.supervisor.id = :userId
)
and form.surveyScope = :scope
and form.status = :status
order by form.dateSubmitted
''', [userId: userId, status: State.SUBMITTED, scope: SurveyScope.ADMIN_CLASS], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def findDoneList(String userId, Map args) {
        def forms = Questionnaire.executeQuery '''
select new map(
  form.id as id,
  pollster.name as pollster,
  form.surveyType as surveyType,
  form.title as title,
  form.respondentType as respondentType,
  form.surveyScope as surveyScope,
  form.dateSubmitted as date,
  form.status as status
)
from Questionnaire form
join form.pollster pollster
where form.checker.id = :userId
and form.surveyScope = :scope
order by form.dateChecked desc
''', [userId: userId, scope: SurveyScope.ADMIN_CLASS], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def getFormForCheck(String userId, Long id, ListType type) {
        def form = questionnaireFormService.getForm(id)

        def workitem = Workitem.findByInstanceAndActivityAndToAndDateProcessedIsNull(
                WorkflowInstance.load(form.workflowInstanceId),
                WorkflowActivity.load("${Questionnaire.WORKFLOW_ID}.adminClassCheck"),
                User.load(userId),
        )
        domainStateMachineHandler.checkReviewer(id, userId, Activities.CHECK)

        return [
                form      : form,
                counts    : getCounts(userId),
                workitemId: workitem ? workitem.id : null,
                prevId    : getPrevCheckId(userId, id, type),
                nextId    : getNextCheckId(userId, id, type),
        ]
    }

    def getFormForCheck(String userId, Long id, ListType type, UUID workitemId) {
        def form = questionnaireFormService.getForm(id)

        def activity = Workitem.get(workitemId).activitySuffix
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        return [
                form      : form,
                counts    : getCounts(userId),
                workitemId: workitemId,
                prevId    : getPrevCheckId(userId, id, type),
                nextId    : getNextCheckId(userId, id, type),
        ]
    }

    private Long getPrevCheckId(String userId, Long id, ListType type) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select id
from Questionnaire
where pollster.id in (
  select student.id
  from Student student
  join student.adminClass adminClass
  where adminClass.supervisor.id = :userId
)
and surveyScope = :scope
and status = :status
and dateSubmitted < (select dateSubmitted from Questionnaire where id = :id)
order by dateSubmitted desc
''', [userId: userId, id: id, status: State.SUBMITTED, scope: SurveyScope.ADMIN_CLASS])
            case ListType.DONE:
                return dataAccessService.getLong('''
select id
from Questionnaire
where checker.id = :userId
and surveyScope = :scope
and dateChecked > (select dateChecked from Questionnaire where id = :id)
order by dateChecked asc
''', [userId: userId, id: id, scope: SurveyScope.ADMIN_CLASS])
        }
    }

    private Long getNextCheckId(String userId, Long id, ListType type) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select id
from Questionnaire
where pollster.id in (
  select student.id
  from Student student
  join student.adminClass adminClass
  where adminClass.supervisor.id = :userId
)
and surveyScope = :scope
and status = :status
and dateSubmitted > (select dateSubmitted from Questionnaire where id = :id)
order by dateSubmitted asc
''', [userId: userId, id: id, status: State.SUBMITTED, scope: SurveyScope.ADMIN_CLASS])
            case ListType.DONE:
                return dataAccessService.getLong('''
select id
from Questionnaire
where checker.id = :userId
and surveyScope = :scope
and dateChecked < (select dateChecked from Questionnaire where id = :id)
order by dateChecked desc
''', [userId: userId, id: id, scope: SurveyScope.ADMIN_CLASS])
        }
    }

    def accept(String userId, AcceptCommand cmd, UUID workitemId) {
        Questionnaire form = Questionnaire.get(cmd.id)
        def now = new Date()

        if (form.surveyScope != SurveyScope.ADMIN_CLASS) {
            throw new BadRequestException()
        }

        domainStateMachineHandler.approve(form, userId, cmd.comment, workitemId, form.pollsterId as String)
        form.approver = Teacher.load(userId)
        form.dateApproved = now
        form.checker = Teacher.load(userId)
        form.dateChecked = now
        form.save(flush: true)

        return [
                form      : form,
                counts    : getCounts(userId),
                workitemId: workitemId,
                prevId    : getPrevCheckId(userId, cmd.id, ListType.DONE),
                nextId    : getNextCheckId(userId, cmd.id, ListType.DONE),
        ]
    }

    def reject(String userId, RejectCommand cmd, UUID workitemId) {
        Questionnaire form = Questionnaire.get(cmd.id)
        domainStateMachineHandler.reject(form, userId, Activities.CHECK, cmd.comment, workitemId)
        form.checker = Teacher.load(userId)
        form.dateChecked = new Date()
        form.save(flush: true)

        return [
                form      : form,
                counts    : getCounts(userId),
                workitemId: workitemId,
                prevId    : getPrevCheckId(userId, cmd.id, ListType.DONE),
                nextId    : getNextCheckId(userId, cmd.id, ListType.DONE),
        ]
    }
}
