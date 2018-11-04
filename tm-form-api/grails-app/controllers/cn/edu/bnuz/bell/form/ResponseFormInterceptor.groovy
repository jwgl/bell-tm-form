package cn.edu.bnuz.bell.form

import cn.edu.bnuz.bell.security.SecurityService
import org.springframework.http.HttpStatus

class ResponseFormInterceptor {
    SecurityService securityService

    boolean before() {
        if (params.respondentId != securityService.userId) {
            render(status: HttpStatus.FORBIDDEN)
            return false
        } else {
            return true
        }
    }

    boolean after() { true }

    void afterView() {}
}
