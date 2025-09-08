package by.sorface.idp.web.graphql.api

import by.sorface.idp.graphql.model.GQRole
import by.sorface.idp.graphql.model.GQRoleCreateInput
import by.sorface.idp.graphql.model.GQRoleDeleteResult
import by.sorface.idp.graphql.model.GQRoleUpdateInput
import by.sorface.idp.web.graphql.converters.RoleConverter
import by.sorface.idp.web.graphql.services.RoleService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.*

@Controller
@PreAuthorize("hasRole('ADMIN')")
class AdminRoleGraphQLController(
    private val roleService: RoleService,
    private val roleConverter: RoleConverter
) {

    @QueryMapping
    fun roleGetAll(): List<GQRole> = roleService.findAll().map { roleConverter.convertToGQRole(it) }

    @MutationMapping
    fun roleUpdate(@Argument input: GQRoleUpdateInput): GQRole? {
        val role = roleService.update(input.id, input.code, input.name)
        return role?.let { roleConverter.convertToGQRole(it) }
    }

    @MutationMapping
    fun roleDelete(@Argument id: UUID): GQRoleDeleteResult {
        val success = roleService.delete(id)
        return GQRoleDeleteResult.builder()
            .setSuccess(success)
            .setMessage(if (success) "Role deleted successfully" else "Role not found")
            .build()
    }

    @MutationMapping
    fun roleCreate(@Argument input: GQRoleCreateInput): GQRole =
        roleConverter.convertToGQRole(
            roleService.create(input.code, input.name)
        )

    @QueryMapping
    fun roleGetById(@Argument id: UUID): GQRole? = roleService.findById(id)?.let { roleConverter.convertToGQRole(it) }

    @QueryMapping
    fun roleGetByCode(@Argument code: String): GQRole? = roleService.findByCode(code)?.let { roleConverter.convertToGQRole(it) }

}
