package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ForbiddenException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.organization.Student
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.security.UserType
import cn.edu.bnuz.bell.workflow.DomainStateMachineHandler
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.commands.SubmitCommand
import grails.gorm.transactions.Transactional
import org.apache.commons.lang3.time.DateUtils
import org.hashids.Hashids
import org.springframework.beans.factory.annotation.Value
import javax.annotation.Resource

@Transactional
class QuestionnaireFormService {
    SecurityService securityService

    @Value('${bell.hash.salt}')
    String hashSalt

    @Value('${bell.hash.length}')
    Integer hashLength

    @Resource(name = 'questionnaireStateHandler')
    DomainStateMachineHandler domainStateMachineHandler

    def list(String userId, Integer offset, Integer max) {
        def list = Questionnaire.executeQuery '''
select new map(
  q.id as id,
  q.title as title,
  case
    when length(q.prologue) > 97 then concat(substring(q.prologue, 1, 97), '...')
    else q.prologue
  end as prologue,
  q.surveyType as surveyType,
  q.surveyScope as surveyScope,
  q.respondentType as respondentType,
  coalesce(
    q.datePublished,
    q.dateApproved,
    q.dateChecked,
    q.dateModified
  ) as dateModified,
  q.status as status,
  q.published as published,
  case
    when status = 'APPROVED' then (
      select count(*)
      from ResponseForm
      where questionnaire.id = q.id
      and dateSubmitted is not null
    )
    else
      null
  end as responseCount
)
from Questionnaire q
where q.pollster.id = :userId
order by 7 desc
''', [userId: userId], [offset: offset, max: max]
        def count = Questionnaire.countByPollster(User.load(userId))
        return [
                list : list,
                count: count,
        ]
    }

    Questionnaire getForm(Long id) {
        Questionnaire.findById(id, [
                fetch: [
                        'pollster'         : 'join',
                        'department'       : 'join',
                        'adminClass'       : 'join',
                        'questions'        : 'join',
                        'questions.options': 'join',
                ]
        ])
    }

    def getFormForShow(String userId, Long id) {
        def form = getForm(id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.pollsterId != userId) {
            throw new ForbiddenException()
        }

        [
                form    : form,
                editable: domainStateMachineHandler.canUpdate(form),
        ]
    }

    def getCreateOptions(String userId) {
        [
                surveyTypes    : getSurveyTypes(),
                surveyScopes   : getSurveyScopes(),
                respondentTypes: getRespondentTypes(),
        ]
    }

    def getFormForCreate(String userId) {
        [
                form: new Questionnaire(
                        pollster: User.get(userId),
                        department: Department.get(securityService.departmentId),
                        adminClass: securityService.userType == UserType.STUDENT ? Student.get(userId).adminClass: null,
                        dateExpired: DateUtils.addDays(new Date(),30),
                ),
        ]
    }

    def getFormForEdit(String userId, Long id) {
        def form = getForm(id)

        if (form.pollsterId != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canUpdate(form)) {
            throw new BadRequestException()
        }

        [form: form]
    }

    def create(String userId, QuestionnaireCommand cmd) {
        def now = new Date()

        def questionnaire = new Questionnaire(
                pollster: User.load(userId),
                department: Department.load(securityService.departmentId),
                adminClass: securityService.userType == UserType.STUDENT ? Student.get(userId).adminClass: null,
                title: cmd.title,
                prologue: cmd.prologue,
                epilogue: cmd.epilogue,
                surveyType: cmd.surveyType,
                surveyScope: cmd.surveyScope,
                respondentType: cmd.respondentType,
                oriented: cmd.oriented,
                restricted: cmd.restricted,
                anonymous: cmd.anonymous,
                responseVisibility: cmd.responseVisibility,
                dateCreated: now,
                dateModified: now,
                dateExpired: cmd.dateExpired,
                published: false,
                status: domainStateMachineHandler.initialState
        )

        cmd.addedQuestions.each { addQuestion(questionnaire, it) }

        questionnaire.save()

        domainStateMachineHandler.create(questionnaire, userId)

        return questionnaire
    }

    Questionnaire update(String userId, QuestionnaireCommand cmd) {
        Questionnaire form = Questionnaire.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.pollsterId != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canUpdate(form)) {
            throw new BadRequestException()
        }

        form.title = cmd.title
        form.prologue = cmd.prologue
        form.epilogue = cmd.epilogue
        form.surveyType = cmd.surveyType
        form.surveyScope = cmd.surveyScope
        form.respondentType = cmd.respondentType
        form.oriented = cmd.oriented
        form.restricted = cmd.restricted
        form.anonymous = cmd.anonymous
        form.responseVisibility = cmd.responseVisibility
        form.dateExpired = cmd.dateExpired
        form.dateModified = new Date()

        cmd.addedQuestions.each { addQuestion(form, it) }
        cmd.updatedQuestions.each { updateQuestion(form, it) }
        cmd.removedQuestions.each { removeQuestion(form, it) }

        domainStateMachineHandler.update(form, userId)

        form.save()
    }

    private addQuestion(Questionnaire questionnaire, QuestionnaireCommand.Question q) {
        def question = new Question(
                ordinal: q.ordinal,
                title: q.title,
                content: q.content,
                type: q.type,
                mandatory: q.mandatory,
                openEnded: q.openEnded,
                openLabel: q.openLabel,
                minValue: q.minValue,
                maxValue: q.maxValue,
                stepValue: q.stepValue,
        )
        q.addedOptions.each { addOption(question, it) }
        questionnaire.addToQuestions(question)
    }

    void delete(String userId, Long id) {
        Questionnaire form = Questionnaire.get(id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.pollsterId != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canUpdate(form)) {
            throw new BadRequestException()
        }

        if (form.workflowInstance) {
            form.workflowInstance.delete()
        }

        form.delete()
    }

    def submit(String userId, SubmitCommand cmd) {
        Questionnaire form = Questionnaire.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.pollsterId != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canSubmit(form)) {
            throw new BadRequestException("Can not submit.")
        }

        domainStateMachineHandler.submit(form, userId, cmd.to, cmd.comment, cmd.title)

        form.hashId = new Hashids(this.hashSalt, this.hashLength).encode(form.id)
        form.dateSubmitted = new Date()
        form.save()
    }

    def publish(String userId, Long id, Boolean publish) {
        Questionnaire form = Questionnaire.get(id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.pollsterId != userId) {
            throw new ForbiddenException()
        }

        if (publish && form.status != State.APPROVED || !publish && !form.published) {
            throw new BadRequestException("表单状态不正确。")
        }

        if (publish && form.datePublished > form.dateExpired) {
            throw new BadRequestException("时间已过期")
        }

        form.published = publish
        form.datePublished = publish ? new Date() : null
        form.save()
    }

    private static updateQuestion(Questionnaire questionnaire, QuestionnaireCommand.Question q) {
        def question = questionnaire.questions.find { it.id == q.id }
        if (question) {
            question.ordinal = q.ordinal
            question.title = q.title
            question.content = q.content
            question.type = q.type
            question.mandatory = q.mandatory
            question.openEnded = q.openEnded
            question.openLabel = q.openLabel
            question.minValue = q.minValue
            question.maxValue = q.maxValue
            question.stepValue = q.stepValue

            q.addedOptions.each { addOption(question, it) }
            q.updatedOptions.each { updateOption(question, it) }
            q.removedOptions.each { removeOption(question, it) }
        }
    }

    private static removeQuestion(Questionnaire questionnaire, Long questionId) {
        def question = Question.load(questionId)
        questionnaire.removeFromQuestions(question)
        question.delete()
    }

    private static updateOption(Question question, QuestionnaireCommand.Question.QuestionOption o) {
        def option = question.options.find { it.id == o.id }
        if (option) {
            option.ordinal = o.ordinal
            option.content = o.content
            option.label = o.label
            option.value = o.value
        }
    }

    private static addOption(Question question, QuestionnaireCommand.Question.QuestionOption o) {
        question.addToOptions(new QuestionOption(
                ordinal: o.ordinal,
                content: o.content,
                label: o.label,
                value: o.value,
        ))
    }

    private static removeOption(Question question, Long optionId) {
        def option = QuestionOption.load(optionId)
        question.removeFromOptions(option)
        option.delete()
    }

    private List<String> getSurveyTypes() {
        List<String> surveyTypes = [
                SurveyType.QUESTIONNAIRE.name(),
                SurveyType.ENTRY_FORM.name(),
                SurveyType.BALLOT_SHEET.name(),
        ]

        surveyTypes
    }

    private List<String> getSurveyScopes() {
        List<String> surveyScopes = [
                SurveyScope.SCHOOL.name(),
                SurveyScope.DEPARTMENT.name()
        ]

        if (securityService.hasRole('ROLE_IN_SCHOOL_STUDENT')) {
            surveyScopes << SurveyScope.ADMIN_CLASS.name()
        }

        surveyScopes
    }

    private List<String> getRespondentTypes() {
        List<String> respondentTypes = []

        if (securityService.userType == UserType.TEACHER) {
            respondentTypes << RespondentType.TEACHER.name()
        }

        respondentTypes << RespondentType.STUDENT.name()

        respondentTypes
    }
}
