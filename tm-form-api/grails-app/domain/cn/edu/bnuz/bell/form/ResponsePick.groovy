package cn.edu.bnuz.bell.form

/**
 * 问卷响应选择
 */
class ResponsePick implements Serializable {
    /**
     * 响应项目
     */
    ResponseItem item

    /**
     * 问题选项
     */
    QuestionOption option

    static belongsTo = [
            item: ResponseItem
    ]

    static mapping = {
        comment '问卷响应选择'
        table   schema: 'tm_form'
        id      composite: ['item', 'option']
        item    comment: '响应项目'
        option  comment: '问题选项'
    }

    boolean equals(other) {
        if (!(other instanceof ResponsePick)) {
            return false
        }

        other.item.id == item.id && other.option == option
    }

    int hashCode() {
        Objects.hash(item.id, option.id)
    }
}
