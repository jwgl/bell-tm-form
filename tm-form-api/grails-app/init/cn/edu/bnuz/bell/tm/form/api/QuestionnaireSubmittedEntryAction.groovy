package cn.edu.bnuz.bell.tm.form.api

import cn.edu.bnuz.bell.form.Questionnaire
import cn.edu.bnuz.bell.form.SurveyScope
import cn.edu.bnuz.bell.workflow.Event
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.actions.AbstractEntryAction
import cn.edu.bnuz.bell.workflow.events.EventData
import cn.edu.bnuz.bell.workflow.events.SubmitEventData
import groovy.transform.CompileStatic
import org.springframework.statemachine.StateContext

@CompileStatic
class QuestionnaireSubmittedEntryAction extends AbstractEntryAction {
    @Override
    void execute(StateContext<State, Event> context) {
        def event = context.getMessageHeader(EventData.KEY) as SubmitEventData
        def workflowInstance = event.entity.workflowInstance

        if (!workflowInstance) {
            workflowInstance = workflowService.createInstance(event.entity.workflowId, event.title, event.entity.id)
            event.entity.workflowInstance = workflowInstance
        }

        Questionnaire questionnaire = context.extendedState.variables['StateObject'] as Questionnaire

        workflowService.createWorkitem(
                workflowInstance,
                event.fromUser,
                context.event,
                context.target.id,
                event.comment,
                event.ipAddress,
                event.toUser,
                questionnaire.surveyScope == SurveyScope.ADMIN_CLASS ? 'adminClassCheck' : 'check',
        )
    }
}
