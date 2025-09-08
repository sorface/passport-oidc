package by.sorface.idp.web.graphql.api.user

import by.sorface.idp.exceptions.GraphqlUserException
import by.sorface.idp.extencions.getPrincipalOrThrow
import by.sorface.idp.graphql.model.GQSession
import by.sorface.idp.records.I18Codes
import by.sorface.idp.web.graphql.services.SessionService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

@Controller
class SessionGraphqlController(private val sessionService: SessionService) {

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun sessionGetAllByUser(): List<GQSession> {
        val principal = SecurityContextHolder.getContext().getPrincipalOrThrow(
            GraphqlUserException(I18Codes.I18GlobalCodes.ACCESS_DENIED)
        )

        return sessionService.getAllByUsername(principal.username)
    }

}