package by.sorface.idp.service.oauth.jdbc

import by.sorface.idp.config.security.oauth2.records.ExternalOAuth2User
import by.sorface.idp.dao.sql.model.UserModel
import by.sorface.idp.dao.sql.model.enums.ProviderType
import by.sorface.idp.dao.sql.repository.user.RoleRepository
import by.sorface.idp.dao.sql.repository.user.UserRepository
import by.sorface.idp.service.oauth.SocialOAuth2UserService
import by.sorface.idp.utils.PasswordGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.util.*

abstract class AbstractSocialOAuth2UserService<T : ExternalOAuth2User> protected constructor(
    private val providerType: ProviderType,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : SocialOAuth2UserService<T> {

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Transactional
    override fun findOrCreate(oAuth2user: T): UserModel {
        var user = userRepository.findByProviderTypeAndExternalId(providerType, oAuth2user.id)
            ?: userRepository.findFirstByEmailIgnoreCase(oAuth2user.email)

        if (user != null) {
            return user
        }

        user = userRepository.findFirstByEmailIgnoreCase(oAuth2user.email)



        val newUser = this.createNewUser(oAuth2user).apply {
            val defaultRole = roleRepository.findFirstByCodeIgnoreCase(DEFAULT_ROLE_USER)

            roles = listOfNotNull(defaultRole)
        }

        return userRepository.save(newUser)
    }

    private fun createNewUser(socialOAuth2User: T): UserModel {
        return UserModel().apply {
            username = buildUserName(socialOAuth2User.username!!, socialOAuth2User.id)
            avatarUrl = socialOAuth2User.avatarUrl
            lastName = socialOAuth2User.lastName
            firstName = socialOAuth2User.firstName
            middleName = socialOAuth2User.middleName
            externalId = socialOAuth2User.id
            avatarUrl = socialOAuth2User.avatarUrl
            email = socialOAuth2User.email
            password = passwordEncoder.encode(PasswordGenerator.generateCommonLangPassword())
            providerType = this@AbstractSocialOAuth2UserService.providerType
        }
    }

    private fun buildUserName(username: String, id: String?): String {
        val existUsername = userRepository.existsByUsername(username)

        return if (existUsername) username + "_" + id?.replace("-".toRegex(), "")?.substring(4) else username
    }

    companion object {
        private const val DEFAULT_ROLE_USER = "ROLE_USER"
    }
}