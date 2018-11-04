package cn.edu.bnuz.bell.form

enum ResponseVisibility {
    INVISIBLE(0, '不可见'),
    VISIBLE_AFTER_SUBMIT(1, '提交后可见'),
    VISIBLE_BEFORE_SUBMIT(2, '提交前可见'),

    final int id
    final String label

    private ResponseVisibility(int id, String label) {
        this.id = id
        this.label = label
    }
}