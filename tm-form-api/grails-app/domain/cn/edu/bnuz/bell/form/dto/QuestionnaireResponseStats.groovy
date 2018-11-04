package cn.edu.bnuz.bell.form.dto

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType

class QuestionnaireResponseStats {
    /**
     * 问卷响应数量
     */
    Long responseCount

    /**
     * 问题响应统计。格式：
     * <pre>
     * {
     *     [questionId]: {
     *         [questionOptionId]: question_option_response_count
     *     }
     * }

     * </pre>
     * 其中questionOptionId:
     * <ul>
     *   <li>开放问题：0</li>
     *   <li>单选或多选选项：选项ID</li>
     *   <li>量表：量表值</li>
     * </ul>
     */
    JsonNode questionStats

    static mapping = {
        table         schema: 'tm_form', name: 'dv_questionnaire_response_stats'
        questionStats sqlType: 'jsonb', type: JsonNodeBinaryType
    }
}
