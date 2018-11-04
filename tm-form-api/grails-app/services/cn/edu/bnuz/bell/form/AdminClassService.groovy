package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.organization.AdminClass
import grails.gorm.transactions.Transactional

@Transactional
class AdminClassService {

    def getAdminClasses(String departmentId) {
       AdminClass.executeQuery '''
select new map(
    adminClass.id as id,
    adminClass.name as name,
    major.grade as grade,
    subject.name as subject
)
from AdminClass adminClass
join adminClass.major major
join major.subject subject
where major.department.id = :departmentId
and major.grade + subject.lengthOfSchooling > (
  select id / 10 from Term where active = true
)
order by adminClass.id
''', [departmentId: departmentId]
    }
}
