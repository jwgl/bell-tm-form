package cn.edu.bnuz.bell.form

class ResponseCommand {
    Long id
    List<ResponseItem> addedItems
    List<ResponseItem> updatedItems
    List<Long> removedItems

    class ResponseItem {
        Long id
        Long question
        Integer intValue
        String textValue
        Long choice
        Long[] choices
    }
}
