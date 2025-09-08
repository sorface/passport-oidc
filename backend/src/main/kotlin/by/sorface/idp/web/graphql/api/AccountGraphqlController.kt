package by.sorface.idp.web.graphql.api

import by.sorface.idp.dao.sql.model.UserModel
import by.sorface.idp.extencions.getPrincipalIdOrNull
import by.sorface.idp.extencions.getPrincipalIdOrThrow
import by.sorface.idp.graphql.model.GQAccountAuthenticated
import by.sorface.idp.graphql.model.GQAccountExists
import by.sorface.idp.graphql.model.GQAccountUsername
import by.sorface.idp.graphql.model.GQAccountUsernameUpdate
import by.sorface.idp.graphql.model.GQPatchUpdateAccount
import by.sorface.idp.records.I18Codes
import by.sorface.idp.web.graphql.services.impl.DefaultAccountService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class AccountGraphqlController(private val defaultAccountService: DefaultAccountService) {

    @QueryMapping
    fun accountGetAuthorized(): UserModel? {
        val principalId = SecurityContextHolder.getContext().getPrincipalIdOrThrow(
            AccessDeniedException(I18Codes.I18AuthenticationCodes.NOT_AUTHENTICATED)
        )

        return defaultAccountService.findById(principalId)
    }

    @QueryMapping
    fun accountGetById(@Argument id: UUID): UserModel? = defaultAccountService.findById(id)

    @QueryMapping
    fun accountGetByUsername(@Argument username: String): UserModel? = defaultAccountService.findByUsername(username)

    @QueryMapping
    fun accountIsAuthenticated(): GQAccountAuthenticated = GQAccountAuthenticated
        .builder()
        .setAccess(SecurityContextHolder.getContext().getPrincipalIdOrNull() != null)
        .build()

    @QueryMapping
    fun accountExistsByUsername(@Argument username: String): GQAccountExists = GQAccountExists.builder()
        .setExists(defaultAccountService.isExistByUsername(username)).build()

    @MutationMapping
    @PreAuthorize("@advancedSecurityEvaluator.hasPrincipalId(#account.id) || hasRole('ROLE_ADMIN')")
    fun accountUpdateDetails(@Argument account: GQPatchUpdateAccount): UserModel? = defaultAccountService.updateInformation(account)

    @MutationMapping
    @PreAuthorize("@advancedSecurityEvaluator.hasPrincipalId(#accountUsernameUpdate.id) || hasRole('ADMIN')")
    fun accountUpdateUsernameById(@Argument accountUsernameUpdate: GQAccountUsernameUpdate): GQAccountUsername =
        defaultAccountService.updateUsername(accountUsernameUpdate.id, accountUsernameUpdate.username)

}