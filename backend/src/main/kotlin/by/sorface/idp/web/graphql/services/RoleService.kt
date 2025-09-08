package by.sorface.idp.web.graphql.services

import by.sorface.idp.dao.sql.model.RoleModel
import java.util.*

/**
 * Интерфейс сервиса для работы с ролями пользователей.
 */
interface RoleService {

    /**
     * Метод для получения всех ролей.
     *
     * @return Список всех ролей.
     */
    fun findAll(): List<RoleModel>

    /**
     * Возвращает список всех ролей, связанных с текущим пользователем.
     *
     * Эта функция предназначена для использования в GraphQL-запросах и позволяет
     * получить доступные роли пользователя в системе. Роли могут использоваться
     * для реализации логики авторизации и управления правами доступа.
     *
     * @return Список объектов [RoleModel], представляющих роли пользователя.
     */
    fun findAllByUser(): List<RoleModel>

    /**
     * Метод для поиска роли по идентификатору.
     *
     * @param id Идентификатор роли.
     * @return RoleModel или null, если роль не найдена.
     */
    fun findById(id: UUID): RoleModel?

    /**
     * Метод для поиска роли по коду.
     *
     * @param code Код роли.
     * @return RoleModel или null, если роль не найдена.
     */
    fun findByCode(code: String): RoleModel?

    /**
     * Метод для создания новой роли.
     *
     * @param code Код роли.
     * @param name Название роли.
     * @return Созданная роль.
     */
    fun create(code: String, name: String? = null): RoleModel

    /**
     * Метод для обновления роли.
     *
     * @param id Идентификатор роли.
     * @param code Новый код роли.
     * @param name Новое название роли.
     * @return Обновленная роль или null, если роль не найдена.
     */
    fun update(id: UUID, code: String, name: String? = null): RoleModel?

    /**
     * Метод для удаления роли.
     *
     * @param id Идентификатор роли.
     * @return true, если роль была удалена, false в противном случае.
     */
    fun delete(id: UUID): Boolean
}
