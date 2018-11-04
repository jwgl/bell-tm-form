package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.organization.AdminClass
import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.StateObject
import cn.edu.bnuz.bell.workflow.StateUserType
import cn.edu.bnuz.bell.workflow.WorkflowInstance
import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType

/**
 * 调查问卷
 */
class Questionnaire implements StateObject {
    /**
     * Hash ID，当提交时设置
     */
    String hashId

    /**
     * 发起人
     */
    User pollster

    /**
     * 所在单位
     */
    Department department

    /**
     * 所在班级
     */
    AdminClass adminClass

    /**
     * 标题
     */
    String title

    /**
     * 欢迎词
     */
    String prologue

    /**
     * 结束语
     */
    String epilogue

    /**
     * 调查类型-0:问卷;1:报名;2:投票;3:采集
     */
    SurveyType surveyType

    /**
     * 调查范围-0:校级;1:院级;2:班级
     */
    SurveyScope surveyScope

    /**
     * 调查对象-0:用户;1:教师;2:学生
     */
    RespondentType respondentType

    /**
     * 面向对象
     */
    JsonNode oriented

    /**
     * 限制对象
     */
    JsonNode restricted

    /**
     * 匿名调查。
     * 如果为是，则调查者不能获得被调查者的实名数据。
     * 如果为否，则调查者可以获得被调查者的实名数据。
     */
    Boolean anonymous

    /**
     * 响应对被调查者的可见性-0:不可见;1:提交后可见;2:提交前可见。
     * 对于信息采集类型，可见性为不可见。
     */
    ResponseVisibility responseVisibility

    /**
     * 创建日期
     */
    Date dateCreated

    /**
     * 修改日期
     */
    Date dateModified

    /**
     * 提交时间
     */
    Date dateSubmitted

    /**
     * 审核人
     */
    Teacher checker

    /**
     * 审核时间
     */
    Date dateChecked

    /**
     * 审批人
     */
    Teacher approver

    /**
     * 审批时间
     */
    Date dateApproved

    /**
     * 是否发布
     */
    Boolean published

    /**
     * 发布日期
     */
    Date datePublished

    /**
     * 截止日期
     */
    Date dateExpired

    /**
     * 状态
     */
    State status

    /**
     * 工作流实例
     */
    WorkflowInstance workflowInstance

    static hasMany = [
            questions: Question,
            responses: ResponseForm,
    ]

    static mapping = {
        comment            '调查问卷'
        table              schema: 'tm_form'
        dynamicUpdate      true
        id                 generator: 'identity', comment: 'ID'
        hashId             unique: true, type: 'text', comment: 'Hash ID'
        pollster           comment: '发起人'
        department         comment: '所在单位'
        adminClass         comment: '所在班级'
        title              type: 'text', comment: '问卷题目'
        prologue           type: 'text', comment: '欢迎词'
        epilogue           type: 'text', comment: '结束语'
        surveyType         enumType: 'identity', comment: '调查类型-0:问卷;1:报名;2:投票;3:采集'
        surveyScope        enumType: 'identity', comment: '调查范围-0:校级;1:院级;2:班级'
        respondentType     enumType: 'identity', comment: '对象类型-0:用户;1:教师;2:学生'
        oriented           sqlType: 'jsonb', type: JsonNodeBinaryType, comment: '面向对象'
        restricted         sqlType: 'jsonb', type: JsonNodeBinaryType, comment: '限制对象'
        anonymous          defaultValue: "true", comment: '是否匿名'
        responseVisibility enumType: 'identity', comment: '可见性-0:不可见;1:提交后可见;2:提交前可见'
        dateCreated        comment: '创建日期'
        dateModified       comment: '修改日期'
        dateSubmitted      comment: '提交时间'
        checker            comment: '审核人'
        dateChecked        comment: '审核时间'
        approver           comment: '审批人'
        dateApproved       comment: '审批时间'
        published          defaultValue: "false", comment: '是否发布'
        datePublished      comment: '发布日期'
        dateExpired        comment: '截止日期'
        status             sqlType: 'state', type: StateUserType, comment: '状态'
        workflowInstance   comment: '工作流实例'
        questions          sort: 'ordinal'
    }

    static constraints = {
        adminClass         nullable: true
        title              maxSize: 50
        prologue           maxSize: 500, nullable: true
        epilogue           maxSize: 250, nullable: true
        oriented           nullable: true
        restricted         nullable: true
        dateSubmitted      nullable: true
        checker            nullable: true
        dateChecked        nullable: true
        approver           nullable: true
        dateApproved       nullable: true
        hashId             nullable: true
        datePublished      nullable: true
        workflowInstance   nullable: true
    }

    String getWorkflowId() {
        WORKFLOW_ID
    }

    static final WORKFLOW_ID = 'form.questionnaire'
}
