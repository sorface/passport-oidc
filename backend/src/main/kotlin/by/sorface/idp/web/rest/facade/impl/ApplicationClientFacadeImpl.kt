package by.sorface.idp.web.rest.facade.impl

import by.sorface.idp.dao.sql.model.client.ClientRedirectUrlModel
import by.sorface.idp.dao.sql.repository.client.ClientSettingRepository
import by.sorface.idp.dao.sql.repository.client.ClientTokenSettingRepository
import by.sorface.idp.dao.sql.repository.client.JdbcRegisteredClientRepository
import by.sorface.idp.dao.sql.repository.client.JpaRegisteredClientRepository
import by.sorface.idp.records.I18Codes
import by.sorface.idp.utils.PasswordGenerator
import by.sorface.idp.web.rest.exceptions.I18RestException
import by.sorface.idp.web.rest.facade.ApplicationClientFacade
import by.sorface.idp.web.rest.mapper.ApplicationConverter
import by.sorface.idp.web.rest.model.apps.ApplicationClient
import by.sorface.idp.web.rest.model.apps.ApplicationPartialUpdate
import by.sorface.idp.web.rest.model.apps.ApplicationRefreshSecret
import by.sorface.idp.web.rest.model.apps.ApplicationRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class ApplicationClientFacadeImpl(
    private val applicationClientRepository: JdbcRegisteredClientRepository,
    private val jpaRegisteredClientRepository: JpaRegisteredClientRepository,
    private val applicationConverter: ApplicationConverter,
    private val passwordEncoder: PasswordEncoder,
    private val clientSettingRepository: ClientSettingRepository,
    private val clientTokenSettingRepository: ClientTokenSettingRepository
) : ApplicationClientFacade {

    private val logger = LoggerFactory.getLogger(ApplicationClientFacade::class.java)

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: UUID): List<ApplicationClient> {
        logger.info("find all client by user [id -> $userId]")

        return applicationClientRepository.findByCreatedUser(userId)
            .map {
                applicationConverter.convert(it)
            }
    }

    @Transactional(readOnly = true)
    override fun findByIdAndUserId(id: UUID, userId: UUID): ApplicationClient {
        val applicationClient = applicationClientRepository.findFirstByIdAndCreatedBy_Id(id, userId)
            ?: throw I18RestException(
                "Application client with id [$id] not found",
                i18Code = I18Codes.I18ClientCodes.NOT_FOUND_BY_ID,
                i18Args = mapOf("id" to id),
                httpStatus = HttpStatus.BAD_REQUEST
            )

        return applicationConverter.convert(applicationClient)
    }

    @Transactional
    override fun registry(registryClient: ApplicationRegistry): ApplicationClient {
        val clientSettings = ClientSettings.builder()
            .requireProofKey(false)
            .requireAuthorizationConsent(false)
            .build()

        val tokenSettings = TokenSettings.builder()
            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
            .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
            .accessTokenTimeToLive(Duration.ofHours(1))
            .refreshTokenTimeToLive(Duration.ofDays(7))
            .reuseRefreshTokens(true)
            .build()

        val rawPassword = PasswordGenerator.generateCommonLangPassword()

        val registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(this.generateClientId())
            .clientName(registryClient.name)
            .clientSecret(passwordEncoder.encode(rawPassword))
            .redirectUris { customizer ->
                customizer.addAll(registryClient.redirectionUrls)
            }
            .clientAuthenticationMethods { customizer ->
                customizer.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                customizer.add(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            }
            .authorizationGrantTypes { customizer ->
                customizer.add(AuthorizationGrantType.AUTHORIZATION_CODE)
                customizer.add(AuthorizationGrantType.REFRESH_TOKEN)
            }
            .scopes { customizer ->
                customizer.add(OidcScopes.OPENID)
                customizer.add(OidcScopes.PROFILE)
                customizer.add(OidcScopes.EMAIL)
            }
            .clientSettings(clientSettings)
            .tokenSettings(tokenSettings)
            .build()

        return jpaRegisteredClientRepository.save(registeredClient).let {
            applicationConverter.convert(registeredClient, rawPassword)
        }
    }

    @Transactional
    override fun partialUpdateByIdAndUserId(
        id: UUID,
        userId: UUID,
        request: ApplicationPartialUpdate
    ): ApplicationClient {
        val applicationClient = applicationClientRepository.findFirstByIdAndCreatedBy_Id(id, userId)
            ?: throw I18RestException(
                "Application client with id $id not found",
                i18Code = I18Codes.I18ClientCodes.NOT_FOUND_BY_ID,
                i18Args = mapOf("id" to id),
                httpStatus = HttpStatus.BAD_REQUEST
            )

        if (!request.name.isNullOrBlank()) {
            applicationClient.clientName = request.name
        }

        val redirectionUrls = request.redirectionUrls

        if (!redirectionUrls.isNullOrEmpty()) {
            applicationClient.redirectUris = redirectionUrls
                .map {
                    ClientRedirectUrlModel().apply { url = it }
                }
                .toSet()
        }

        return applicationClientRepository.save(applicationClient)
            .let {
                applicationConverter.convert(it)
            }
    }

    @Transactional
    override fun refreshSecretByIdAndUserId(id: UUID, userId: UUID): ApplicationRefreshSecret {
        val applicationClient = applicationClientRepository.findFirstByIdAndCreatedBy_Id(id, userId)
            ?: throw I18RestException(
                "Application client with id $id not found",
                i18Code = I18Codes.I18ClientCodes.NOT_FOUND_BY_ID,
                i18Args = mapOf("id" to id),
                httpStatus = HttpStatus.BAD_REQUEST
            )

        val rawPassword = PasswordGenerator.generateCommonLangPassword()

        applicationClient.clientSecret = passwordEncoder.encode(rawPassword)
        applicationClient.clientIdIssueAt = Instant.now()
        applicationClient.clientSecretExpiresAt = Instant.now().plus(300, ChronoUnit.DAYS)

        return ApplicationRefreshSecret().apply {
            this.clientSecret = rawPassword
            this.expiresAt = LocalDateTime.ofInstant(applicationClient.clientSecretExpiresAt, ZoneOffset.UTC)
        }
    }

    @Transactional
    override fun deleteByIdAndUserId(id: UUID, userId: UUID) {
        val applicationClient = applicationClientRepository.findFirstByIdAndCreatedBy_Id(id, userId)
            ?: throw I18RestException(
                "Application client with id $id not found",
                i18Code = I18Codes.I18ClientCodes.NOT_FOUND_BY_ID,
                i18Args = mapOf("id" to id),
                httpStatus = HttpStatus.BAD_REQUEST
            )

        clientSettingRepository.deleteById(applicationClient.id!!)
        clientTokenSettingRepository.deleteById(applicationClient.id!!)
        applicationClientRepository.deleteById(applicationClient.id!!)
    }

    private fun generateClientId(): String {
        return UUID.randomUUID().toString().replace("-", "").lowercase(Locale.getDefault()) + "@sorface.oauth.client"
    }
}