package by.sorface.idp.web.graphql.services.impl

import by.sorface.idp.config.security.constants.SessionAttributes.USER_AGENT
import by.sorface.idp.graphql.model.GQSession
import by.sorface.idp.web.graphql.services.SessionService
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder


@Service
class DefaultSessionService(
    private val sessionRepository: FindByIndexNameSessionRepository<out org.springframework.session.Session>,
    private val userAgentAnalyzer: UserAgentAnalyzer
) : SessionService {

    override fun getAllByUsername(username: String): List<GQSession> =
        sessionRepository.findByPrincipalName(username)
            .mapNotNull { entry -> entry.value }
            .map { session -> buildUserSession(RequestContextHolder.currentRequestAttributes().sessionId, session) }
            .toList()

    private fun <T : Session> buildUserSession(activeId: String, session: T): GQSession {
        val attribute: String? = session.getAttribute(USER_AGENT)

        val immutableUserAgent = userAgentAnalyzer.parse(attribute)


        return GQSession.builder()
            .setId(session.id)
            .setCreatedAt(session.creationTime?.epochSecond)
            .setLastAccessTime(session.lastAccessedTime?.epochSecond)
            .setBrowser(immutableUserAgent.getValue("AgentName"))
            .setDeviceBrand(immutableUserAgent.getValue("DeviceBrand"))
            .setDeviceType(immutableUserAgent.getValue("DeviceClass"))
            .setDevice(immutableUserAgent.getValue("DeviceName"))
            .setActive(activeId.equals(session.id, ignoreCase = true))
            .build()
    }

}