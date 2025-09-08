package by.sorface.idp.dao.sql.repository.client

import by.sorface.idp.dao.sql.model.client.ClientSettingModel
import by.sorface.idp.dao.sql.model.client.ClientTokenSettingModel
import by.sorface.idp.dao.sql.model.client.PostLogoutRedirectUrlModel
import by.sorface.idp.dao.sql.model.client.RegisteredClientModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class JpaRegisteredClientRepository : RegisteredClientRepository {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var clientAuthenticationGrantTypeRepository: ClientAuthenticationGrantTypeRepository

    @Autowired
    private lateinit var clientAuthenticationMethodRepository: ClientAuthenticationMethodRepository

    @Autowired
    private lateinit var clientScopeRepository: ClientScopeRepository

    @Autowired
    private lateinit var jdbcRegisteredClientRepository: JdbcRegisteredClientRepository

    @Transactional
    override fun save(registeredClient: RegisteredClient) {
        logger.info("save new client with id {}", registeredClient.id)

        val basicRegisteredClientModel = RegisteredClientModel().apply {
            clientId = registeredClient.clientId
            clientSecret = registeredClient.clientSecret
            clientName = registeredClient.clientName
            clientIdIssueAt = Instant.now()
        }

        try {
            val registeredClientModel = jdbcRegisteredClientRepository.save(basicRegisteredClientModel)

            registeredClientModel.redirectUris.plus(
                registeredClient.redirectUris
                    .mapNotNull { redirectUrl -> by.sorface.idp.dao.sql.model.client.ClientRedirectUrlModel().apply { this.url = redirectUrl } }
                    .toMutableSet()
            )

            registeredClientModel.postLogoutRedirectUrls.plus(
                registeredClient.postLogoutRedirectUris
                    .mapNotNull { redirectUrl -> PostLogoutRedirectUrlModel().apply { this.url = redirectUrl } }
                    .toMutableSet()
            )

            registeredClientModel.methods.plus(
                registeredClient.clientAuthenticationMethods
                    .mapNotNull { method ->
                        val clientAuthenticationMethodModel = clientAuthenticationMethodRepository.findFirstByMethodEndingWithIgnoreCase(method.value)

                        clientAuthenticationMethodModel ?: RuntimeException("method [$method] not registered")

                        return@mapNotNull clientAuthenticationMethodModel
                    }
                    .toMutableSet()
            )

            registeredClientModel.grantTypes.plus(
                registeredClient.authorizationGrantTypes
                    .mapNotNull { grantType ->
                        val clientAuthenticationGrantTypeModel = clientAuthenticationGrantTypeRepository.findFirstByGrantTypeEndingWithIgnoreCase(grantType.value)

                        clientAuthenticationGrantTypeModel ?: RuntimeException("grant type [$grantType] not registered")

                        return@mapNotNull clientAuthenticationGrantTypeModel
                    }
                    .toMutableSet()
            )

            registeredClientModel.scopes.plus(
                registeredClient.scopes
                    .mapNotNull { scope ->
                        val clientScopeModel = clientScopeRepository.findFirstByScope(scope)

                        clientScopeModel ?: RuntimeException("scope [$scope] not registered")

                        return@mapNotNull clientScopeModel
                    }
                    .toMutableSet()
            )

            val clientSetting = ClientSettingModel().apply {
                requireConcept = false
                requireProofKey = false
            }

            val tokenSetting = ClientTokenSettingModel().apply {
                accessTokenFormat = OAuth2TokenFormat.SELF_CONTAINED.value
                idTokenSignatureAlgorithm = SignatureAlgorithm.RS256
                accessTokenTimeToLive = Duration.ofHours(1).toSeconds()
                refreshTokenTimeToLive = Duration.ofDays(7).toSeconds()
                reuseRefreshTokens = false
            }

            registeredClientModel.clientSetting = clientSetting
            registeredClientModel.tokenSetting = tokenSetting

            tokenSetting.registeredClient = registeredClientModel
            clientSetting.registeredClient = registeredClientModel

            jdbcRegisteredClientRepository.save(registeredClientModel)
        } catch (e: Exception) {
            logger.error("error save client [$registeredClient]", e)

            throw e
        }

        logger.info("client [id {}] saved success", registeredClient.id)
    }

    override fun findById(id: String): RegisteredClient? {
        logger.info("searching for client by id [{}]", id)

        val registeredClient = jdbcRegisteredClientRepository.findById(UUID.fromString(id))
            .map {
                logger.info("client found by id [{}]", id)

                map(it)
            }.orElseGet {
                logger.info("client not fount by id [{}]", id)

                null
            }

        return registeredClient
    }

    override fun findByClientId(clientId: String): RegisteredClient? {
        logger.info("searching for application clientId [{}]", clientId)

        val registeredClient = jdbcRegisteredClientRepository.findByClientId(clientId)

        if (registeredClient == null) {
            logger.info("no client with application clientId [{}]", clientId)

            return null
        }

        logger.info("found client with clientId [{}]", registeredClient.clientId)

        return map(registeredClient)
    }

    private fun map(registeredClientModel: RegisteredClientModel): RegisteredClient {
        return RegisteredClient.withId(registeredClientModel.id.toString()).apply {
            clientId(registeredClientModel.clientId)
            clientName(registeredClientModel.clientName)
            clientSecret(registeredClientModel.clientSecret)
            clientIdIssuedAt(registeredClientModel.clientIdIssueAt)
            clientSecretExpiresAt(registeredClientModel.clientSecretExpiresAt)
            val methods = registeredClientModel.methods.map { ClientAuthenticationMethod(it.method) }.toSet()

            clientAuthenticationMethods { it.addAll(methods) }

            val grantTypes = registeredClientModel.grantTypes.map { AuthorizationGrantType(it.grantType) }.toMutableSet()

            authorizationGrantTypes { it.addAll(grantTypes) }

            val redirectUrls = registeredClientModel.redirectUris.mapNotNull { model -> model.url }.toMutableSet()

            redirectUris { it.addAll(redirectUrls) }

            val postLogoutRedirectUrls = registeredClientModel.postLogoutRedirectUrls.mapNotNull { model -> model.url }.toMutableSet()

            postLogoutRedirectUris { it.addAll(postLogoutRedirectUrls) }

            val scopes = registeredClientModel.scopes.map { it.scope }.toMutableSet()

            scopes { it.addAll(scopes) }

            val registeredClientSettings: ClientSettingModel = registeredClientModel.clientSetting!!

            clientSettings(
                ClientSettings.builder()
                    .requireProofKey(registeredClientSettings.requireProofKey)
                    .requireAuthorizationConsent(registeredClientSettings.requireConcept)
                    .build()
            )

            val registeredTokenSettingModel = registeredClientModel.tokenSetting!!

            tokenSettings(
                TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat(registeredTokenSettingModel.accessTokenFormat))
                    .idTokenSignatureAlgorithm(registeredTokenSettingModel.idTokenSignatureAlgorithm)
                    .accessTokenTimeToLive(Duration.ofSeconds(registeredTokenSettingModel.accessTokenTimeToLive))
                    .refreshTokenTimeToLive(Duration.ofSeconds(registeredTokenSettingModel.refreshTokenTimeToLive))
                    .reuseRefreshTokens(registeredTokenSettingModel.reuseRefreshTokens)
                    .build()
            )
        }.build()
    }

}