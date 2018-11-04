package cn.edu.bnuz.bell.form

import groovy.transform.CompileStatic

@CompileStatic
enum RespondentType  {
    TEACHER(1),
    STUDENT(2),

    final int id

    private RespondentType(int id) {
        this.id = id
    }
}