package by.sorface.idp.service.oauth.converters

import by.sorface.idp.dao.sql.model.RoleModel
import by.sorface.idp.dao.sql.model.UserModel
import org.springframework.stereotype.Component
import ru.sorface.boot.security.core.principal.SorfacePrincipal

@Component
class DefaultPrincipalConverter : PrincipalConverter {

    override fun convert(user: UserModel): SorfacePrincipal {
        val authorities = this.convertRoles(user.roles)

        return SorfacePrincipal(
            user.id,
            user.username!!,
            user.password,
            authorities
        )
    }

    private fun convertRoles(roles: Collection<RoleModel>): Set<String> {
        return roles.mapNotNull { role -> role.code }.toSet()
    }
}
