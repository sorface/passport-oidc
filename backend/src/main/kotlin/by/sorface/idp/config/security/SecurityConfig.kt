package by.sorface.idp.config.security

import by.sorface.idp.config.security.csrf.CsrfCookieFilter
import by.sorface.idp.config.security.csrf.SpaCsrfTokenRequestHandler
import by.sorface.idp.config.security.entrypoints.JsonUnauthorizedAuthenticationEntryPoint
import by.sorface.idp.config.security.formlogin.JsonFormLoginFailureHandler
import by.sorface.idp.config.security.formlogin.SessionRedirectSuccessHandler
import by.sorface.idp.config.web.properties.CsrfCookieProperties
import by.sorface.idp.config.web.properties.IdpEndpointProperties
import by.sorface.idp.config.web.properties.SessionCookieProperties
import by.sorface.idp.extencions.jsonLogin
import by.sorface.idp.service.oauth.OAuth2UserDatabaseStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseCookie.ResponseCookieBuilder
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.session.data.redis.RedisIndexedSessionRepository
import org.springframework.session.security.SpringSessionBackedSessionRegistry
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer
import org.springframework.web.cors.CorsConfigurationSource

@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity(debug = true)
@Profile("development")
class SecurityDevelopmentConfig {

    @Bean
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests.requestMatchers("/h2-console/**", "**.css", "**.js", "**,jsp", "/graphiql/**", "/graphql/**").permitAll()
                    .anyRequest().authenticated()
            }
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.disable() }
            .formLogin(Customizer.withDefaults())

        return http.build()
    }

}

/**
 * Конфигурация безопасности для production и песочницы
 */
@EnableMethodSecurity
@Configuration(proxyBeanMethods = true)
@EnableWebSecurity
@Profile("production", "sandbox")
class SecurityProductionConfig {

    @Autowired
    private lateinit var jsonUnauthorizedAuthenticationEntryPoint: JsonUnauthorizedAuthenticationEntryPoint

    @Autowired
    private lateinit var csrfCookieProperties: CsrfCookieProperties

    @Autowired
    private lateinit var corsConfigurationSource: CorsConfigurationSource

    @Autowired
    private lateinit var sessionRedirectSuccessHandler: SessionRedirectSuccessHandler

    @Autowired
    private lateinit var jsonFormLoginFailureHandler: JsonFormLoginFailureHandler

    @Autowired
    private lateinit var idpEndpointProperties: IdpEndpointProperties

    @Autowired
    private lateinit var oAuth2UserDatabaseStrategy: OAuth2UserDatabaseStrategy

    /**
     * Создает цепочку фильтров безопасности по умолчанию
     *
     * @param http Объект HttpSecurity для настройки безопасности.
     * @return Объект SecurityFilterChain, представляющий цепочку фильтров безопасности.
     */
    @Bean
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http

            /**
             * Настраивает OAuth2-аутентификацию в Spring Security.
             *
             * Эта конфигурация определяет параметры входа через OAuth2, такие как:
             * - URL страницы входа.
             * - Обработчик получения информации о пользователе (user info).
             * - Обработчики успешной и неудачной аутентификации.
             */
            .oauth2Login { oauth2LoginSpec ->

                /**
                 * Настраивает эндпоинт для получения информации о пользователе после OAuth2-аутентификации.
                 *
                 * Используется для настройки пользовательского сервиса, который будет обрабатывать получение данных пользователя.
                 *
                 * @param userInfoEndpointSpec спецификация эндпоинта получения информации о пользователе
                 */
                oauth2LoginSpec.userInfoEndpoint { userInfoEndpointSpec ->

                    /**
                     * Устанавливает пользовательский сервис для получения информации о пользователе.
                     *
                     * @param oAuth2UserDatabaseStrategy сервис, реализующий логику получения данных пользователя из базы данных
                     */
                    userInfoEndpointSpec.userService(oAuth2UserDatabaseStrategy)
                }

                /**
                 * Устанавливает URL страницы входа для OAuth2.
                 * Эта страница отображается, если требуется авторизация перед выполнением защищённого действия.
                 */
                oauth2LoginSpec.loginPage(idpEndpointProperties.loginPage)

                /**
                 * Устанавливает обработчик успешной OAuth2-аутентификации.
                 * После успешного входа пользователь перенаправляется или получает ответ согласно логике этого обработчика.
                 */
                oauth2LoginSpec.successHandler(sessionRedirectSuccessHandler)

                /**
                 * Устанавливает обработчик неудачной OAuth2-аутентификации.
                 * Используется для обработки ошибок, таких как неправильные данные или проблемы с сервером.
                 */
                oauth2LoginSpec.failureHandler(jsonFormLoginFailureHandler)
            }
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/h2-console/**", "**.css", "**.js", "**,jsp", "/graphiql/**", "/graphql/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/csrf", "/api/accounts/login/{login}/exists", "/api/accounts/authenticated"
                    )
                    .permitAll()
                    .requestMatchers("/graphiql/**", "/graphql/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/registrations").anonymous()
                    .requestMatchers(HttpMethod.POST, "/api/registrations").anonymous()
                    .requestMatchers(HttpMethod.PUT, "/api/registrations/otp").anonymous()
                    .requestMatchers(HttpMethod.POST, "/api/registrations/confirm").anonymous()
                    .requestMatchers(HttpMethod.POST, idpEndpointProperties.loginPath).anonymous()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }

            /**
             * Настраивает кэширование запросов в Spring Security.
             *
             * Эта конфигурация используется для сохранения исходного запроса пользователя перед перенаправлением
             * на страницу входа или авторизации. После успешной аутентификации пользователь будет перенаправлен
             * обратно на этот запрос.
             */
            .requestCache { httpSecurityRequestCacheConfigurer: RequestCacheConfigurer<HttpSecurity?> ->

                /**
                 * Создаёт и настраивает объект [HttpSessionRequestCache], который хранит запросы в HTTP-сессии.
                 */
                val httpSessionRequestCache = HttpSessionRequestCache()

                httpSessionRequestCache.setRequestMatcher(AntPathRequestMatcher("/oauth2/**"))
                httpSecurityRequestCacheConfigurer.requestCache(httpSessionRequestCache)
            }
            .csrf { csrfSpec -> csrfConfigurer(csrfSpec) }
            .addFilterAfter(CsrfCookieFilter(), BasicAuthenticationFilter::class.java) // csrf filter for SPA
            .cors { cors -> cors.configurationSource(corsConfigurationSource) }
            /**
             * Настраивает JSON-аутентификацию через форму входа в Spring Security.
             *
             * Этот блок конфигурации определяет параметры аутентификации, такие как:
             * - URL страницы входа.
             * - URL обработки запроса аутентификации.
             * - Обработчики успешной и неудачной аутентификации.
             *
             * Используется для интеграции с формой входа, где данные отправляются в формате JSON.
             */
            .jsonLogin { jsonLoginSpec ->
                /**
                 * Устанавливает URL страницы входа.
                 * Эта страница отображается пользователю для ввода логина и пароля.
                 */
                jsonLoginSpec.loginPage(idpEndpointProperties.loginPage)

                /**
                 * Устанавливает URL, по которому будет происходить обработка запроса аутентификации.
                 * Это адрес, на который отправляется POST-запрос с данными формы.
                 */
                jsonLoginSpec.loginProcessingUrl(idpEndpointProperties.loginPath)

                /**
                 * Устанавливает обработчик успешной аутентификации.
                 * После успешного входа пользователь перенаправляется или получает ответ согласно логике этого обработчика.
                 */
                jsonLoginSpec.successHandler(sessionRedirectSuccessHandler)

                /**
                 * Устанавливает обработчик неудачной аутентификации.
                 * Используется для обработки ошибок, таких как неверный логин или пароль.
                 */
                jsonLoginSpec.failureHandler(jsonFormLoginFailureHandler)
            }
            .exceptionHandling { exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(jsonUnauthorizedAuthenticationEntryPoint)
            }

        return http.build()
    }

    @Bean
    @Primary
    fun sessionRegistry(sessionRepository: RedisIndexedSessionRepository): SessionRegistry {
        return SpringSessionBackedSessionRegistry(sessionRepository);
    }

    /**
     * Настройка конфигурации CSRF
     *
     * @param csrfConfigurer Объект CsrfConfigurer для настройки CSRF.
     */
    private fun csrfConfigurer(csrfConfigurer: CsrfConfigurer<HttpSecurity>) {
        val cookieCsrfTokenRepository = getCookieCsrfTokenRepository(csrfCookieProperties)
        val csrfAuthenticationStrategy = CsrfAuthenticationStrategy(cookieCsrfTokenRepository)
        val spaCsrfTokenRequestHandler = SpaCsrfTokenRequestHandler(XorCsrfTokenRequestAttributeHandler())

        csrfConfigurer
            .ignoringRequestMatchers(
                AntPathRequestMatcher.antMatcher(HttpMethod.GET),
                AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/logout"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/graphiql/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/graphql/**")
            )
            .csrfTokenRepository(cookieCsrfTokenRepository)
            .csrfTokenRequestHandler(spaCsrfTokenRequestHandler)
            .sessionAuthenticationStrategy(csrfAuthenticationStrategy)
    }

    @Configuration
    class CookieConfiguration {
        @Bean
        fun sessionCookieSerializer(sessionCookieProperties: SessionCookieProperties): CookieSerializer {
            return sessionCookieProperties.run {
                val serializer = DefaultCookieSerializer()
                serializer.setPartitioned(false)
                serializer.setUseBase64Encoding(false)

                serializer.setCookieName(this.name)
                serializer.setCookiePath(this.path)
                serializer.setDomainNamePattern(this.domainPattern)
                serializer.setUseHttpOnlyCookie(this.httpOnly)
                serializer.setSameSite(sessionCookieProperties.sameSite.name)
                serializer.setCookieMaxAge(this.maxAge.toSeconds().toInt())

                serializer
            }
        }
    }

    /**
     * Создает репозиторий CSRF-токенов на основе свойств CSRF.
     *
     * @param csrfCookieProperties Объект CsrfCookieProperties, содержащий свойства CSRF.
     * @return Объект CookieCsrfTokenRepository, представляющий репозиторий CSRF-токенов.
     */
    private fun getCookieCsrfTokenRepository(csrfCookieProperties: CsrfCookieProperties): CookieCsrfTokenRepository {
        val cookieCsrfTokenRepository = CookieCsrfTokenRepository()

        cookieCsrfTokenRepository.setCookieName(csrfCookieProperties.name)
        cookieCsrfTokenRepository.cookiePath = csrfCookieProperties.path

        cookieCsrfTokenRepository.setCookieCustomizer { cookieBuilder: ResponseCookieBuilder ->
            cookieBuilder.httpOnly(csrfCookieProperties.httpOnly)
            cookieBuilder.domain(csrfCookieProperties.domain)
            cookieBuilder.secure(csrfCookieProperties.secure)
        }

        return cookieCsrfTokenRepository
    }

    /**
     * Создает обработчик запросов CSRF для SPA
     *
     * @return Объект CsrfTokenRequestHandler, представляющий обработчик запросов CSRF для SPA.
     */
    @Bean
    fun spaCsrfTokenRequestHandler(): CsrfTokenRequestHandler {
        return SpaCsrfTokenRequestHandler(XorCsrfTokenRequestAttributeHandler())
    }

}
