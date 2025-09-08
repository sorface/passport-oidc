package by.sorface.idp.web.graphql.api

import by.sorface.idp.graphql.model.GQRole
import by.sorface.idp.web.graphql.converters.RoleConverter
import by.sorface.idp.web.graphql.services.RoleService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL контроллер для работы с ролями пользователей.
 */
@Controller
@PreAuthorize("isAuthenticated()")
class RoleGraphqlController(private val roleService: RoleService, private val roleConverter: RoleConverter) {

    @QueryMapping
    fun roleGetMyRoles(): List<GQRole> = roleService.findAllByUser().map { roleConverter.convertToGQRole(it) }

}