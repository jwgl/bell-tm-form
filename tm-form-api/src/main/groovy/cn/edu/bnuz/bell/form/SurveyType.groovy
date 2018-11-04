package cn.edu.bnuz.bell.form

enum SurveyType {
    QUESTIONNAIRE(1, '调查问卷'),
    ENTRY_FORM(2, '报名表'),
    BALLOT_SHEET(3, '投票单'),
    INFO_COLLECTION(4, '信息采集'),

    final int id
    final String label

    private SurveyType(int id, String label) {
        this.id = id
        this.label = label
    }

    static SurveyType from(int id) {
        values().find { it.id == id }
    }
}