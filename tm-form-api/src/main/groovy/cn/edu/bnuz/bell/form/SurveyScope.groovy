package cn.edu.bnuz.bell.form

import groovy.transform.CompileStatic

@CompileStatic
enum SurveyScope {
    SCHOOL(0),
    DEPARTMENT(1),
    ADMIN_CLASS(2),

    final int id

    private SurveyScope(int id) {
        this.id = id
    }
}