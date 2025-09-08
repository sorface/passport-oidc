package by.sorface.idp.config.security.formlogin

import by.sorface.idp.config.security.constants.SessionAttributes
import by.sorface.idp.config.security.formlogin.records.AccountSuccessAuthentication
import by.sorface.idp.config.web.properties.IdpFrontendEndpointProperties
import by.sorface.idp.extencions.useJsonStream
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.savedrequest.SavedRequest
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.io.IOException
import java.util.*

/**
 * Обработчик успешной аутентификации, который перенаправляет пользователя на сохраненный URL.
 */
@Component
class SessionRedirectSuccessHandler(endpointFrontendProperties: IdpFrontendEndpointProperties)
    : AbstractAuthenticationTargetUrlRequestHandler(), AuthenticationSuccessHandler {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SessionRedirectSuccessHandler::class.java)
    }

    init {
        targetUrlParameter = "targetUrl"
        defaultTargetUrl = endpointFrontendProperties.accountPage
    }

    /**
     * Обрабатывает успешную аутентификацию.
     *
     * @param request объект запроса
     * @param response объект ответа
     * @param authentication объект аутентификации
     * @throws IOException если возникает ошибка ввода-вывода
     * @throws ServletException если возникает ошибка сервлета
     */
    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        val requestAttributes = RequestContextHolder.currentRequestAttributes()

        logger.info("authentication success for user ${authentication.name} with session ${requestAttributes.sessionId}")

        request.getHeader(HttpHeaders.USER_AGENT)?.let { userAgent ->
            LOGGER.debug("user-agent [value -> {}] for session [id -> {}]", request.requestedSessionId, userAgent)

            requestAttributes.setAttribute(SessionAttributes.USER_AGENT, userAgent, RequestAttributes.SCOPE_SESSION)
        }

        logger.info("get current  for user ${authentication.name} with session ${requestAttributes.sessionId}")

        val savedRequest = requestAttributes.getAttribute(SessionAttributes.SAVED_REQUEST, RequestAttributes.SCOPE_SESSION) as SavedRequest?

        if (savedRequest == null) {
            LOGGER.info("request [SessionAttributes.SAVED_REQUEST] is NULL for session [id -> {}]", request.requestedSessionId)

            setStrategyRedirect(authentication, request, response, defaultTargetUrl)

            return
        }

        LOGGER.info("found saved request [url -> {}]. session [id -> {}]", savedRequest.redirectUrl, request.requestedSessionId)

        LOGGER.info("target url parameter [{}], session [id -> {}]", targetUrlParameter, request.requestedSessionId)

        if (isAlwaysUseDefaultTargetUrl || (targetUrlParameter != null && StringUtils.hasText(request.getParameter(targetUrlParameter)))) {

            setStrategyRedirect(authentication, request, response, defaultTargetUrl)

            return
        }

        LOGGER.info("clean session authentication for session [id -> {}]", request.requestedSessionId)

        clearAuthenticationAttributes(requestAttributes)

        val targetUrl = savedRequest.redirectUrl

        LOGGER.info("oauth2 redirect to target url -> {}. session [id -> {}]", targetUrl, request.requestedSessionId)

        setStrategyRedirect(authentication, request, response, targetUrl)
    }

    private fun setStrategyRedirect(authentication: Authentication, request: HttpServletRequest, response: HttpServletResponse, targetUrl: String) {
        when (authentication) {
            is OAuth2AuthenticationToken -> {
                redirectStrategy.sendRedirect(request, response, targetUrl)
            }
            is UsernamePasswordAuthenticationToken -> {
                response.useJsonStream(HttpStatus.OK, AccountSuccessAuthentication(targetUrl))
            }
            else -> {
                throw IllegalStateException("Unknown authentication type")
            }
        }
    }

    /**
     * Очищает атрибуты аутентификации.
     *
     * @param requestAttributes атрибуты запроса
     */
    private fun clearAuthenticationAttributes(requestAttributes: RequestAttributes) {
        val attribute = requestAttributes.getAttribute(SessionAttributes.AUTHENTICATION_EXCEPTION, RequestAttributes.SCOPE_SESSION)

        if (Objects.nonNull(attribute)) {
            requestAttributes.removeAttribute(SessionAttributes.AUTHENTICATION_EXCEPTION, RequestAttributes.SCOPE_SESSION)
        }
    }

    /**
     * Всегда ли использовать URL по умолчанию.
     *
     * @return false
     */
    override fun isAlwaysUseDefaultTargetUrl(): Boolean {
        return false
    }

}
