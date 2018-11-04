package cn.edu.bnuz.bell.form

/**
 * 问卷响应项目，对应一个用户对一次问卷中的一个问题的响应。
 */
class ResponseItem {
    /**
     * 所属表单
     */
    ResponseForm form

    /**
     * 调查问题
     */
    Question question

    /**
     * 单选响应
     */
    QuestionOption choice

    /**
     * 数值响应，当问题类型为量表时有值
     */
    Integer intValue

    /**
     * 文本响应，当问题类型为开放或问题开放时有值
     */
    String textValue

    static belongsTo = [
            form: ResponseForm
    ]

    static hasMany = [
            choices: ResponsePick
    ]

    static mapping = {
        comment   '问卷响应项目'
        table     schema: 'tm_form'
        id        generator: 'identity', comment: 'ID'
        form      comment: '所属表单'
        question  comment: '调查问题'
        choice    comment: '单选响应'
        intValue  comment: '数值响应'
        textValue type: 'text', comment: '文本响应'
    }

    static constraints = {
        choice    nullable: true
        intValue  nullable: true
        textValue nullable: true
    }
}
