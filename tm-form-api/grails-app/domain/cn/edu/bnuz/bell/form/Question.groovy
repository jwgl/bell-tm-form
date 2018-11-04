package cn.edu.bnuz.bell.form

/**
 * 问题
 */
class Question {
    /**
     * 所属问卷
     */
    Questionnaire questionnaire

    /**
     * 序号
     */
    Integer ordinal


    /**
     * 问题标题
     */
    String title

    /**
     * 问题内容
     */
    String content

    /**
     * 问题类型-0:开放;1:单选;2:多选;3:量表
     */
    Integer type

    /**
     * 是否为必选问题
     */
    Boolean mandatory

    /**
     * 是否为开放问题
     */
    Boolean openEnded

    /**
     * 开放问题提示，只在单选和多选有效
     */
    String openLabel

    /**
     * 最小值。
     * 开放问题，表示用户输入文字的最小长度；
     * 单选问题，无意义；
     * 多选问题，表示最少选项；
     * 量表问题，表示量表最小值；
     */
    Integer minValue

    /**
     * 最大值。
     * 开放问题，表示用户输入文字的最大长度；
     * 单选问题，无意义；
     * 多选问题，表示最多选项；
     * 量表问题，表示量表最大值；
     */
    Integer maxValue

    /**
     * 量表间隔。
     * 开放问题，表示textarea的rows；
     * 单选问题，表示选项列数；
     * 多选问题，表示选项列数；
     * 量表问题，表示量表间隔；
     */
    Integer stepValue

    static belongsTo = [
            questionnaire: Questionnaire
    ]

    static hasMany = [
            options: QuestionOption
    ]

    static mapping = {
        comment       '问题'
        table         schema: 'tm_form'
        id            generator: 'identity', comment: 'ID'
        questionnaire comment: '所属问卷'
        ordinal       comment: '问题序号'
        title         type: 'text', comment: '问题标题'
        content       type: 'text', comment: '问题内容'
        type          comment: '问题类型-0:开放;1:单选;2:多选;3:量表'
        mandatory     comment: '是否为必选问题'
        openEnded     comment: '是否为开放问题'
        openLabel     type: 'text', comment: '开放问题提示'
        minValue      comment: '最小值'
        maxValue      comment: '最大值'
        stepValue     comment: '量表间隔'
        options       sort: 'ordinal'
    }

    static constraints = {
        title         maxSize: 100
        content       maxSize: 500
        openLabel     nullable: true
        minValue      nullable: true
        maxValue      nullable: true
    }
}
