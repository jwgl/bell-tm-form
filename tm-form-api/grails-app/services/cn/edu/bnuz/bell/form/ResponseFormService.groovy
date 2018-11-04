package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.form.dto.StudentQuestionnaire
import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ForbiddenException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.security.User
import grails.gorm.transactions.Transactional

@Transactional
class ResponseFormService {
    private Questionnaire getQuestionnaire(String hashId) {
        return Questionnaire.findByHashId(hashId, [
                fetch: [
                        'pollster'         : 'join',
                        'department'       : 'join',
                        'questions'        : 'join',
                        'questions.options': 'join',
                ]
        ])
    }

    def getForm(String userId, String hashId) {
        ResponseForm form = ResponseForm.find {
            (questionnaire.hashId == hashId) && (respondent.id == userId)
        }
        if (form) {
            if (!form.dateSubmitted) {
                if (!StudentQuestionnaire.isAvailableToStudent(hashId, userId)) {
                    throw new NotFoundException()
                }
            }
        }

        return [
                form         : form,
                questionnaire: getQuestionnaire(hashId),
        ]
    }

    def create(String userId, Long questionnaireId, ResponseCommand cmd, Boolean submit) {
        def now = new Date()

        def form = new ResponseForm(
                questionnaire: Questionnaire.load(questionnaireId),
                respondent: User.load(userId),
                dateCreated: now,
                dateModified: now,
                dateSubmitted: submit ? now : null,
        )

        cmd.addedItems.each { addItem(form, it) }

        form.save()

        return [
                form         : form,
                questionnaire: getQuestionnaire(form.questionnaire.hashId),
        ]
    }


    def update(String userId, Long questionnaireId, ResponseCommand cmd) {
        ResponseForm form = ResponseForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.respondentId != userId) {
            throw new ForbiddenException()
        }

        if (form.questionnaireId != questionnaireId) {
            throw new BadRequestException()
        }

        form.dateModified = new Date()

        cmd.addedItems.each { addItem(form, it) }
        cmd.updatedItems.each { updateItem(form, it) }
        cmd.removedItems.each { removeItem(form, it) }

        form.save()

        return [
                form         : form,
                questionnaire: getQuestionnaire(form.questionnaire.hashId),
        ]
    }

    def addItem(ResponseForm form, ResponseCommand.ResponseItem item) {
        def question = Question.get(item.question)
        def responseItem = new ResponseItem(question: question)
        switch (question.type) {
            case 0:
                if (!item.textValue) {
                    throw new BadRequestException("Question ${question.id}: empty text value.")
                }
                responseItem.textValue = item.textValue
                break
            case 1:
                if (question.openEnded && !item.choice && !item.textValue
                        || !question.openEnded && !item.choice
                ) {
                    throw new BadRequestException("Question ${question.id}: empty choice.")
                }
                if (item.choice) {
                    responseItem.choice = QuestionOption.load(item.choice)
                } else {
                    responseItem.textValue = item.textValue
                }
                break
            case 2:
                if (question.openEnded && !item.choices && !item.textValue
                        || !question.openEnded && !item.choices) {
                    throw new BadRequestException("Question ${question.id}: empty choice.")
                }
                if (item.choices) {
                    item.choices.each { choice ->
                        responseItem.addToChoices(new ResponsePick(
                                item: responseItem,
                                option: QuestionOption.load(choice),
                        ))
                    }
                }
                if (question.openEnded) {
                    responseItem.textValue = item.textValue
                }
                break
            case 3:
                if (!item.intValue) {
                    throw new BadRequestException("Question ${question.id}: empty choice.")
                }
                responseItem.intValue = item.intValue
                break

        }
        form.addToItems(responseItem)
    }

    def updateItem(ResponseForm form, ResponseCommand.ResponseItem item) {
        def responseItem = form.items.find { it.id == item.id }
        def question = responseItem.question
        if (responseItem) {
            switch (question.type) {
                case 0:
                    if (!item.textValue) {
                        throw new BadRequestException("Question ${question.id}: empty text value.")
                    }
                    responseItem.textValue = item.textValue
                    break
                case 1:
                    if (question.openEnded && !item.choice && !item.textValue
                            || !question.openEnded && !item.choice
                    ) {
                        throw new BadRequestException("Question ${question.id}: empty choice.")
                    }
                    responseItem.choice = QuestionOption.load(item.choice)
                    responseItem.textValue = item.textValue
                    break
                case 2:
                    if (question.openEnded && !item.choices && !item.textValue
                            || !question.openEnded && !item.choices) {
                        throw new BadRequestException("Question ${question.id}: empty choice.")
                    }
                    if (item.choices) {
                        def removedChoices = []
                        responseItem.choices.toArray().each { ResponsePick choice ->
                            if (!item.choices.find { it == choice.optionId }) {
                                responseItem.removeFromChoices(choice)
                                removedChoices << choice.optionId
                            }
                        }
                        if (removedChoices) {
                            ResponsePick.executeUpdate('delete from ResponsePick where item.id=:itemId and option.id in(:options)', [
                                    itemId : responseItem.id,
                                    options: removedChoices,
                            ])
                        }

                        item.choices.each { choice ->
                            responseItem.addToChoices(new ResponsePick(
                                    item: responseItem,
                                    option: QuestionOption.load(choice),
                            ))
                        }
                    } else {
                        if (responseItem.choices) {
                            responseItem.choices.clear()
                            ResponsePick.executeUpdate('delete from ResponsePick where item.id=:itemId', [
                                    itemId: responseItem.id,
                            ])
                        }
                    }
                    if (question.openEnded) {
                        responseItem.textValue = item.textValue
                    }
                    break
                case 3:
                    if (!item.intValue) {
                        throw new BadRequestException("Question ${question.id}: empty choice.")
                    }
                    responseItem.intValue = item.intValue
                    break
            }
        }
    }

    def removeItem(ResponseForm form, Long id) {
        def responseItem = form.items.find { it.id == id }
        if (responseItem) {
            form.removeFromItems(responseItem)
            responseItem.delete()
        }
    }

    def submit(String userId, Long questionnaireId) {
        ResponseForm form = ResponseForm.findByQuestionnaireAndRespondent(
                Questionnaire.load(questionnaireId), User.load(userId)
        )

        if (!form) {
            throw new NotFoundException()
        }

        if (form.respondentId != userId) {
            throw new ForbiddenException()
        }

        if (form.questionnaireId != questionnaireId) {
            throw new BadRequestException()
        }

        form.dateSubmitted = new Date()

        form.save()

        return [
                form         : form,
                questionnaire: getQuestionnaire(form.questionnaire.hashId),
        ]
    }
}
