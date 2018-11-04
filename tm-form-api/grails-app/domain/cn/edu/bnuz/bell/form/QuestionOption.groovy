package cn.edu.bnuz.bell.form

/**
 * 问题选项
 */
class QuestionOption {
    /**
     * 所属问题
     */
    Question question

    /**
     * 序号
     */
    Integer ordinal

    /**
     * 内容
     */
    String content

    /**
     * 标签
     */
    String label

    /**
     * 数值
     */
    Integer value

    static belongsTo = [
            question: Question
    ]

    static mapping = {
        comment      '问题选项'
        table        schema: 'tm_form'
        id           generator: 'identity', comment: 'ID'
        question     comment: '所属问题'
        ordinal      comment: '选项序号'
        content      type: 'text', comment: '选项内容'
        label        type: 'text', comment: '选项标签'
        value        comment: '选项数值'
    }

    static constraints = {
        label        nullable: true
        value        nullable: true
    }
}
