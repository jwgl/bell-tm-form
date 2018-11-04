package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.*
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import cn.edu.bnuz.bell.workflow.commands.RevokeCommand
import grails.gorm.transactions.Transactional
import org.springframework.orm.hibernate5.HibernateJdbcException

@Transactional
class QuestionnaireApprovalService extends QuestionnaireCheckService {
    private getCounts(String userId, ListType type, String query) {
        return [
                (ListType.TODO): query && type == ListType.TODO
                        ? Questionnaire.countByStatusAndTitleLike(State.CHECKED, "%${query}%")
                        : Questionnaire.countByStatus(State.CHECKED),
                (ListType.DONE): query && type == ListType.DONE
                        ? Questionnaire.countByApproverAndTitleLike(Teacher.load(userId), "%${query}%")
                        : Questionnaire.countByApprover(Teacher.load(userId)),
        ]
    }

    def list(String userId, ListCommand cmd) {
        switch (cmd.type) {
            case ListType.TODO:
                return findTodoList(userId, cmd)
            case ListType.DONE:
                return findDoneList(userId, cmd)
            default:
                throw new BadRequestException()
        }
    }

    def findTodoList(String userId, ListCommand cmd) {
        def forms = Questionnaire.executeQuery '''
select new map(
  form.id as id,
  department.name as department,
  pollster.name as pollster,
  form.surveyType as surveyType,
  form.title as title,
  form.respondentType as respondentType,
  form.surveyScope as surveyScope,
  form.dateChecked as date,
  form.status as status
)
from Questionnaire form
join form.pollster pollster
join form.department department
where form.status = :status
and form.title like :query
order by form.dateChecked
''', [status: State.CHECKED, query: "%${cmd.query?:''}%"],
                [offset: cmd.offset, max: cmd.max]

        return [forms: forms, counts: getCounts(userId, cmd.type, cmd.query)]
    }

    def findDoneList(String userId, ListCommand cmd) {
        def forms = Questionnaire.executeQuery '''
select new map(
  form.id as id,
  department.name as department,
  pollster.name as pollster,
  form.surveyType as surveyType,
  form.title as title,
  form.respondentType as respondentType,
  form.surveyScope as surveyScope,
  form.dateChecked as date,
  form.dateApproved as date,
  form.status as status
)
from Questionnaire form
join form.pollster pollster
join form.department department
where form.approver.id = :userId
and form.title like :query
order by form.dateApproved desc
''', [userId: userId, query: "%${cmd.query?:''}%"],
                [offset: cmd.offset, max: cmd.max]

        return [forms: forms, counts: getCounts(userId, cmd.type, cmd.query)]
    }

    def getFormForApproval(String userId, Long id, ListType type, String query) {
        def form = questionnaireFormService.getForm(id)

        if (!form) {
            throw new NotFoundException()
        }

        def activity = Activities.APPROVE
        def workitem = Workitem.findByInstanceAndActivityAndToAndDateProcessedIsNull(
                WorkflowInstance.load(form.workflowInstanceId),
                WorkflowActivity.load("${Questionnaire.WORKFLOW_ID}.${activity}"),
                User.load(userId),
        )
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        return [
                form      : form,
                counts    : getCounts(userId, type, query),
                workitemId: workitem ? workitem.id : null,
                prevId    : getPrevApprovalId(userId, id, type, query),
                nextId    : getNextApprovalId(userId, id, type, query),
        ]
    }

    def getFormForApproval(String userId, Long id, ListType type, UUID workitemId) {
        def form = questionnaireFormService.getForm(id)

        if (!form) {
            throw new NotFoundException()
        }

        domainStateMachineHandler.checkReviewer(id, userId, Activities.APPROVE)

        return [
                form      : form,
                counts    : getCounts(userId, type, null),
                workitemId: workitemId,
                prevId    : getPrevApprovalId(userId, id, type, null),
                nextId    : getNextApprovalId(userId, id, type, null),
        ]
    }

    private Long getPrevApprovalId(String userId, Long id, ListType type, String query) {
        switch (type) {
            case ListType.TODO:
                dataAccessService.getLong '''
select id
from Questionnaire
where status = :status
and title like :query
and dateChecked < (select dateChecked from Questionnaire where id = :id)
order by dateChecked desc
''', [id: id, status: State.CHECKED, query: "%${query}%"]
                break
            case ListType.DONE:
                dataAccessService.getLong '''
select id
from Questionnaire
where approver.id = :userId
and title like :query
and dateApproved > (select dateApproved from Questionnaire where id = :id)
order by dateApproved asc
''', [id: id, userId: userId, query: "%${query}%"]
                break
        }
    }

    private Long getNextApprovalId(String userId, Long id, ListType type, String query) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select id
from Questionnaire
where status = :status
and title like :query
and dateChecked > (select dateChecked from Questionnaire where id = :id)
order by dateChecked asc
''', [id: id, status: State.CHECKED, query: "%${query}%"])
            case ListType.DONE:
                return dataAccessService.getLong('''
select id
from Questionnaire
where approver.id = :userId
and title like :query
and dateApproved < (select dateApproved from Questionnaire where id = :id)
order by dateApproved desc
''', [id: id, userId: userId, query: "%${query}%"])
        }
    }

    def accept(String userId, AcceptCommand cmd, UUID workitemId) {
        Questionnaire form = Questionnaire.get(cmd.id)

        domainStateMachineHandler.accept(form, userId, Activities.APPROVE, cmd.comment, workitemId)
        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save(flush: true)

        return [
                form      : form,
                counts    : getCounts(userId),
                workitemId: workitemId,
                prevId    : getPrevApprovalId(userId, cmd.id, ListType.DONE, ''),
                nextId    : getPrevApprovalId(userId, cmd.id, ListType.DONE, ''),
        ]
    }

    def reject(String userId, RejectCommand cmd, UUID workitemId) {
        Questionnaire form = Questionnaire.get(cmd.id)
        domainStateMachineHandler.reject(form, userId, Activities.APPROVE, cmd.comment, workitemId)
        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save(flush: true)

        return [
                form      : form,
                counts    : getCounts(userId),
                workitemId: workitemId,
                prevId    : getPrevApprovalId(userId, cmd.id, ListType.DONE, ''),
                nextId    : getPrevApprovalId(userId, cmd.id, ListType.DONE, ''),
        ]
    }
}

