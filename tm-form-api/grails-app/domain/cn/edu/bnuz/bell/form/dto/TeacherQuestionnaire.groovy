package cn.edu.bnuz.bell.form.dto


import cn.edu.bnuz.bell.form.SurveyType
import grails.gorm.hibernate.HibernateEntity

class TeacherQuestionnaire implements HibernateEntity<TeacherQuestionnaire> {
    /**
     * Hash ID
     */
    String hashId

    /**
     * 调查者
     */
    String pollster

    /**
     * 所在单位
     */
    String department

    /**
     * 标题
     */
    String title

    /**
     * 欢迎词
     */
    String prologue

    /**
     * 调查类型-0:问卷;1:报名;2:投票;3:采集
     */
    SurveyType surveyType

    /**
     * 匿名调查。
     * 如果为是，则调查者不能获得被调查者的实名数据。
     * 如果为否，则调查者可以获得被调查者的实名数据。
     */
    Boolean anonymous

    /**
     * 发布日期
     */
    Date datePublished

    /**
     * 截止日期
     */
    Date dateExpired

    /**
     * 创建但未提交的响应表单ID
     */
    Long formId

    static mapping = {
        table       name: 'dv_dumb', schema: 'tm_form'
        id          name: 'hashId'
        surveyType  enumType: 'identity', comment: '调查类型-0:问卷;1:报名;2:投票;3:采集'
    }

    /**
     * 按学生获取开放的调查问卷
     * @param studentId 学生ID
     * @return 调查问卷
     */
    static List<TeacherQuestionnaire> findAvailableQuestionnaires(String teacherId) {
        findAllWithSql("select * from tm_form.sp_find_available_questionnaire_by_teacher($teacherId)")
    }

    /**
     * 学生是否可响应调查问卷
     * @param hashId 问卷HashID
     * @param teacherId 教师ID
     * @return 是否可响应
     */
    static boolean isAvailableToTeacher(String hashId, String teacherId) {
        findWithSql("select * from tm_form.sp_find_available_questionnaire_by_teacher($teacherId) where hash_id=${hashId}") != null
    }
}
