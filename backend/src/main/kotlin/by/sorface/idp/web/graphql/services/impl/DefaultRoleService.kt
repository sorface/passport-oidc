package by.sorface.idp.web.graphql.services.impl

import by.sorface.idp.dao.sql.model.RoleModel
import by.sorface.idp.dao.sql.repository.user.RoleRepository
import by.sorface.idp.dao.sql.repository.user.UserRepository
import by.sorface.idp.extencions.getPrincipalIdOrNull
import by.sorface.idp.web.graphql.services.RoleService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

/**
 * Реализация сервиса для работы с ролями пользователей.
 */
@Service
class DefaultRoleService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository
) : RoleService {

    override fun findAll(): List<RoleModel> {
        return roleRepository.findAll()
    }

    override fun findAllByUser(): List<RoleModel> {
        val userId = SecurityContextHolder.getContext().getPrincipalIdOrNull() ?: return emptyList()
        
        return userRepository.findRolesByUserId(userId)
    }

    override fun findById(id: UUID): RoleModel? {
        return roleRepository.findById(id).orElse(null)
    }

    override fun findByCode(code: String): RoleModel? {
        return roleRepository.findFirstByCodeIgnoreCase(code)
    }

    override fun create(code: String, name: String?): RoleModel {
        val role = RoleModel()
        role.code = code
        role.name = name
        return roleRepository.save(role)
    }

    override fun update(id: UUID, code: String, name: String?): RoleModel? {
        val existingRole = roleRepository.findById(id).orElse(null) ?: return null
        existingRole.code = code
        existingRole.name = name
        return roleRepository.save(existingRole)
    }

    override fun delete(id: UUID): Boolean {
        return if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id)
            true
        } else {
            false
        }
    }
}
