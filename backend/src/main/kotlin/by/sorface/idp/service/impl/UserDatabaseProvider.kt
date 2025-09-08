package by.sorface.idp.service.impl

import by.sorface.idp.dao.sql.repository.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sorface.boot.security.core.principal.SorfacePrincipal

@Service
class UserDatabaseProvider(private val userRepository: UserRepository) : UserDetailsService {

    private val logger = LoggerFactory.getLogger(UserDatabaseProvider::class.java)

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails? =
        (userRepository.findFirstByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
            ?: throw UsernameNotFoundException("user with username or email $username} not found"))
            .let {
                logger.info("user with username/email [$username/${it.email}] and ID [${it.id}] was loaded")

                SorfacePrincipal(it.id, it.username!!, it.password!!, it.roles.mapNotNull { role -> role.code }.toMutableSet())
            }

}