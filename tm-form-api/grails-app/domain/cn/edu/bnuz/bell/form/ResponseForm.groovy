package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.security.User

/**
 * 问卷响应表单，表示一个用户对一次问卷的响应。
 */
class ResponseForm {
    /**
     * 所属问卷
     */
    Questionnaire questionnaire

    /**
     * 调查对象
     */
    User respondent

    /**
     * 创建时间
     */
    Date dateCreated

    /**
     * 修改时间
     */
    Date dateModified

    /**
     * 提交时间
     */
    Date dateSubmitted

    static belongsTo = [
            questionnaire: Questionnaire
    ]

    static hasMany = [
            items: ResponseItem
    ]

    static mapping = {
        comment        '问卷响应表单'
        table          schema: 'tm_form'
        id             generator: 'identity', comment: 'ID'
        questionnaire  comment: '所属问卷'
        respondent     unique: ['questionnaire'], comment: '调查对象'
        dateCreated    comment: '创建日期'
        dateModified   comment: '修改日期'
        dateSubmitted  comment: '提交时间'
    }

    static constraints = {
        dateSubmitted nullable: true
    }
}
