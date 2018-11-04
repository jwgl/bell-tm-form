package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.service.DataAccessService
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.DomainStateMachineHandler
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.WorkflowActivity
import cn.edu.bnuz.bell.workflow.WorkflowInstance
import cn.edu.bnuz.bell.workflow.Workitem
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.gorm.transactions.Transactional

import javax.annotation.Resource

@Transactional
class QuestionnaireCheckService {
    QuestionnaireFormService questionnaireFormService
    DataAccessService dataAccessService

    @Resource(name = 'questionnaireStateHandler')
    DomainStateMachineHandler domainStateMachineHandler

    final static SURVEY_SCOPES = [
            SurveyScope.SCHOOL,
            SurveyScope.DEPARTMENT,
    ]

    protected getCounts(String userId) {
        def todo = dataAccessService.getInteger '''
select count(*)
from Questionnaire form
join form.pollster pollster
where form.department.id = (select department.id from Teacher where id = :userId)
and form.surveyScope in (:scopes)
and form.status = :status
''', [userId: userId, status: State.SUBMITTED, scopes: SURVEY_SCOPES]

        def done = Questionnaire.countByCheckerAndSurveyScopeInList(Teacher.load(userId), SURVEY_SCOPES)

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
where form.department.id = (select department.id from Teacher where id = :userId)
and form.surveyScope in (:scopes)
and form.status = :status
order by form.dateSubmitted
''', [userId: userId, status: State.SUBMITTED, scopes: SURVEY_SCOPES], args

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
and form.surveyScope in (:scopes)
order by form.dateChecked desc
''', [userId: userId, scopes: SURVEY_SCOPES], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def getFormForCheck(String userId, Long id, ListType type, String activity) {
        def form = questionnaireFormService.getForm(id)

        def workitem = Workitem.findByInstanceAndActivityAndToAndDateProcessedIsNull(
                WorkflowInstance.load(form.workflowInstanceId),
                WorkflowActivity.load("${Questionnaire.WORKFLOW_ID}.${activity}"),
                User.load(userId),
        )
        domainStateMachineHandler.checkReviewer(id, userId, activity)

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
where department.id = (select department.id from Teacher where id = :userId)
and surveyScope in (:scopes)
and status = :status
and dateSubmitted < (select dateSubmitted from Questionnaire where id = :id)
order by dateSubmitted desc
''', [userId: userId, id: id, status: State.SUBMITTED, scopes: SURVEY_SCOPES])
            case ListType.DONE:
                return dataAccessService.getLong('''
select id
from Questionnaire
where checker.id = :userId
and surveyScope in (:scopes)
and dateChecked > (select dateChecked from Questionnaire where id = :id)
order by dateChecked asc
''', [userId: userId, id: id, scopes: SURVEY_SCOPES])
        }
    }

    private Long getNextCheckId(String userId, Long id, ListType type) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select id
from Questionnaire
where department.id = (select department.id from Teacher where id = :userId)
and status = :status
and surveyScope in (:scopes)
and dateSubmitted > (select dateSubmitted from Questionnaire where id = :id)
order by dateSubmitted asc
''', [userId: userId, id: id, status: State.SUBMITTED, scopes: SURVEY_SCOPES])
            case ListType.DONE:
                return dataAccessService.getLong('''
select id
from Questionnaire
where checker.id = :userId
and surveyScope in (:scopes)
and dateChecked < (select dateChecked from Questionnaire where id = :id)
order by dateChecked desc
''', [userId: userId, id: id, scopes: SURVEY_SCOPES])
        }
    }

    def accept(String userId, AcceptCommand cmd, UUID workitemId) {
        Questionnaire form = Questionnaire.get(cmd.id)
        def now = new Date()

        switch (form.surveyScope) {
            case SurveyScope.DEPARTMENT:
                domainStateMachineHandler.approve(form, userId, cmd.comment, workitemId, form.pollsterId as String)
                form.approver = Teacher.load(userId)
                form.dateApproved = now
                break
            case SurveyScope.SCHOOL:
                domainStateMachineHandler.accept(form, userId, Activities.CHECK, cmd.comment, workitemId, cmd.to)
                break
            default:
                throw new BadRequestException()
        }
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
