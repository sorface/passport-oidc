package by.sorface.idp.web.graphql.api

import by.sorface.idp.dao.sql.model.client.RegisteredClientModel
import by.sorface.idp.exceptions.GraphqlUserException
import by.sorface.idp.extencions.getPrincipalIdOrThrow
import by.sorface.idp.records.I18Codes
import by.sorface.idp.web.graphql.services.ApplicationService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

@Controller
class ApplicationGraphqlController(private val applicationService: ApplicationService) {

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun applicationGetByUser(): List<RegisteredClientModel> {
        val principalId = SecurityContextHolder.getContext().getPrincipalIdOrThrow(
            GraphqlUserException(I18Codes.I18GlobalCodes.ACCESS_DENIED)
        )

        return applicationService.getAllByUser(principalId)
    }

}