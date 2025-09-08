package by.sorface.idp.web.graphql.services

import by.sorface.idp.dao.sql.model.client.RegisteredClientModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * Интерфейс сервиса для работы с приложениями.
 */
interface ApplicationService {

    /**
     * Метод для получения зарегистрированных приложений
     *
     * @return Список зарегистрированных клиентов.
     */
    fun getAll(pageable: Pageable): Page<RegisteredClientModel>

    /**
     * Метод для получения всех зарегистрированных клиентов по идентификатору пользователя.
     *
     * @param id Идентификатор пользователя.
     * @return Список зарегистрированных клиентов, связанных с пользователем.
     */
    fun getAllByUser(id: UUID): List<RegisteredClientModel>

    /**
     * Возвращает модель зарегистрированного приложения по его идентификатору.
     *
     * Эта функция предназначена для использования в GraphQL-запросах и позволяет
     * получить полную информацию о клиенте, зарегистрированном в системе.
     * Данные клиента могут быть использованы для отображения информации или
     * дальнейшей обработки в логике приложения.
     *
     * @param id Идентификатор клиента в виде объекта [UUID]
     * @return Объект [RegisteredClientModel], представляющий данные клиента
     */
    fun getById(id: UUID): RegisteredClientModel
    
    /**
     * Удаляет зарегистрированного клиента по указанному идентификатору.
     *
     * Эта функция предназначена для использования в GraphQL-запросах и позволяет
     * удалять клиентов из системы. Удаление происходит по уникальному идентификатору,
     * переданному в виде параметра.
     *
     * @param id Идентификатор клиента в виде объекта [UUID]
     * @return `true`, если клиент был успешно удален, иначе `false`
     */
    fun delete(id: UUID): Boolean

}