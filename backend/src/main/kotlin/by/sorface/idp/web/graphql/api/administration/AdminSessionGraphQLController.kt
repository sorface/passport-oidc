package by.sorface.idp.web.graphql.api.administration

import by.sorface.idp.graphql.model.GQSession
import by.sorface.idp.web.graphql.services.SessionService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL контроллер для административных операций с сессиями пользователей.
 * Предоставляет методы для получения информации о сессиях с ограничением доступа только для администраторов.
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
class AdminSessionGraphQLController(private val sessionService: SessionService) {

    /**
     * Получает все активные сессии для указанного пользователя по имени пользователя.
     * 
     * @param username имя пользователя, для которого необходимо получить сессии
     * @return список всех сессий пользователя в формате GraphQL
     * @throws SecurityException если у текущего пользователя нет прав администратора
     * @throws IllegalArgumentException если username равен null или пустой строке
     */
    @QueryMapping
    fun sessionGetAllByUsername(@Argument username: String): List<GQSession> = sessionService.getAllByUsername(username)

}