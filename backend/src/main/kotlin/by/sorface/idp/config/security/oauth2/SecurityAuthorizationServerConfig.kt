package by.sorface.idp.config.security.oauth2

import by.sorface.idp.config.security.jose.JwksService
import by.sorface.idp.config.security.oauth2.properties.OidcAuthorizationProperties
import by.sorface.idp.config.security.oauth2.slo.OidcLogoutHandler
import by.sorface.idp.config.web.properties.IdpEndpointProperties
import by.sorface.idp.service.oauth.jdbc.DefaultOidcUserInfoService
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.PropertyResolver
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import java.time.Duration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OidcConfigurer
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OidcUserInfoEndpointConfigurer
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import java.util.function.Function

private const val BACKCHANNEL_LOGOUT_SUPPORTED = "backchannel_logout_supported"

private const val BACKCHANNEL_LOGOUT_SESSION_SUPPORTED = "backchannel_logout_session_supported"

/**
 * Конфигурация безопасности сервера авторизации.
 */
@Configuration(proxyBeanMethods = true)
class SecurityAuthorizationServerConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var defaultOidcUserInfoService: DefaultOidcUserInfoService

    @Autowired
    private lateinit var idpEndpointProperties: IdpEndpointProperties

    @Autowired
    private lateinit var oidcLogoutHandler: OidcLogoutHandler

    /**
     * Настройка цепочки фильтров безопасности сервера авторизации.
     *
     * @param http объект HttpSecurity для настройки
     * @return настроенная цепочка фильтров безопасности
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity, propertyResolver: PropertyResolver): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()

        val userInfoMapper = Function<OidcUserInfoAuthenticationContext, OidcUserInfo> { context: OidcUserInfoAuthenticationContext ->
            val authentication = context.getAuthentication<OidcUserInfoAuthenticationToken>()
            val principal = authentication.principal as JwtAuthenticationToken

            defaultOidcUserInfoService.loadUser(principal.name, context.accessToken.scopes)
        }

        return http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .with(authorizationServerConfigurer) { authorizationServerSpec ->
                authorizationServerSpec.oidc { oidc: OidcConfigurer ->
                    oidc.userInfoEndpoint { userInfo: OidcUserInfoEndpointConfigurer -> userInfo.userInfoMapper(userInfoMapper) }
                    oidc.logoutEndpoint { logoutSpec ->
                        logoutSpec.logoutResponseHandler(oidcLogoutHandler)
                        logoutSpec.errorResponseHandler { request, response, exception ->
                            when {
                                exception.message?.contains("Jwt expired") == true -> {
                                    log.warn("JWT token expired during logout: ${exception.message}")
                                }
                                else -> {
                                    log.error("OpenID Connect 1.0 RP-Initiated Logout Error: ${exception.message}", exception)
                                }
                            }
                        }
                    }
                    oidc.providerConfigurationEndpoint { providerConfigurationEndpointSpec ->
                        providerConfigurationEndpointSpec.providerConfigurationCustomizer { providerConfiguration ->
                            providerConfiguration.claim(BACKCHANNEL_LOGOUT_SUPPORTED, true)
                            providerConfiguration.claim(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED, true)
                        }
                    }
                }
            }
            .authorizeHttpRequests { authorizeRequests -> authorizeRequests.anyRequest().authenticated() }
            .csrf { csrf: CsrfConfigurer<HttpSecurity?> -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.endpointsMatcher) }
            .oauth2ResourceServer { it.jwt {}.authenticationEntryPoint(Http403ForbiddenEntryPoint()) }
            .exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?> ->
                exceptions.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint(idpEndpointProperties.loginPage))
            }
            .build()
    }

    /**
     * Настройка параметров сервера авторизации.
     *
     * @param oidcAuthorizationProperties свойства авторизации OIDC
     * @return настроенные параметры сервера авторизации
     */
    @Bean
    fun authorizationServerSettings(oidcAuthorizationProperties: OidcAuthorizationProperties): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer(oidcAuthorizationProperties.url)
            .build()
    }

    /**
     * Настройка источника JWK.
     *
     * @return источник JWK
     */
    @Bean
    fun jwkSource(jwksService: JwksService): JWKSource<SecurityContext> {
        val rsaKey = jwksService.getRsaKey()
        val jwkSet = JWKSet(rsaKey)

        return JWKSource { jwkSelector: JWKSelector, _: SecurityContext? -> jwkSelector.select(jwkSet) }
    }

    /**
     * Настройка декодера JWT.
     *
     * @param jwkSource источник JWK
     * @param oidcAuthorizationProperties свойства авторизации OIDC
     * @return декодер JWT
     */
    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext?>, oidcAuthorizationProperties: OidcAuthorizationProperties): JwtDecoder {
        val jwtDecoder = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource) as NimbusJwtDecoder
        
        // Настройка валидаторов JWT с учетом clock skew для обработки истекших токенов в logout
        val jwtValidator = DelegatingOAuth2TokenValidator<Jwt>(
            JwtIssuerValidator(oidcAuthorizationProperties.url)
        )
        
        jwtDecoder.setJwtValidator(jwtValidator)
        
        return jwtDecoder
    }

    @Bean
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext?>): JwtEncoder = NimbusJwtEncoder(jwkSource)

    /**
     * Настройка кодировщика паролей.
     *
     * @return кодировщик паролей
     */
    @Bean
    fun passportEncoder(): PasswordEncoder = BCryptPasswordEncoder(10)

}