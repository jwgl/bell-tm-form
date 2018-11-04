package cn.edu.bnuz.bell.tm.form.api

class UrlMappings {

    static mappings = {
        // 调查者
        "/pollsters"(resources: 'pollster', includes: []) {
            "/questionnaires"(resources: 'questionnaireForm') {
                "/checkers"(controller: 'questionnaireForm', action: 'checkers', method: 'GET')
                "/responses"(resources: 'questionnaireResponse', includes: ['index'])
                "/respondents"(controller: 'questionnaireResponse', action: 'respondents', method: 'GET')
                "/questions"(resources: 'question', includes: []) {
                    "/openResponses"(controller: 'questionnaireResponse', action: 'openResponses', method: 'GET')
                }
                collection {
                    "/createOptions"(controller: 'questionnaireForm', action: 'createOptions', method: 'GET')
                }
            }
        }

        // 被调查者
        "/respondents"(resources: 'respondent', includes: []) {
            "/questionnaires"(resources: 'respondentQuestionnaire', includes: ['index']) {
                "/response"(single: 'responseForm')
            }
        }

        // 院级调查审核
        "/checkers"(resources: 'checker', includes: []) {
            "/questionnaires"(resources: 'questionnaireCheck', includes: ['index']) {
                "/workitems"(resources: 'questionnaireCheck', includes: ['show', 'patch'])
                "/approvers"(controller: 'questionnaireCheck', action: 'approvers', method: 'GET')
            }
        }

        // 校级调查审批
        "/approvers"(resources: 'approver', includes: []) {
            "/questionnaires"(resources: 'questionnaireApproval', includes: ['index']) {
                "/workitems"(resources: 'questionnaireApproval', includes: ['show', 'patch'])
            }
        }

        // 班级调查审核
        "/supervisors"(resources: 'supervisor', inclcudes: []) {
            "/questionnaires"(resources: 'questionnaireAdminClassCheck') {
                "/workitems"(resources: 'questionnaireAdminClassCheck', includes: ['show', 'patch'])
            }
            "/adminClasses"(controller: 'questionnaireAdminClassCheck', action: 'adminClasses', method: 'GET')
        }

        // 学院信息
        "/departments"(resources: 'department', include: ['index']) {
            "/adminClasses"(controller: 'department', action: 'adminClasses', method: 'GET')
        }

        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
