package by.sorface.idp.web.graphql.api.administration

import by.sorface.idp.graphql.model.GQRole
import by.sorface.idp.graphql.model.GQRoleCreateInput
import by.sorface.idp.graphql.model.GQRoleDeleteResult
import by.sorface.idp.graphql.model.GQRoleUpdateInput
import by.sorface.idp.web.graphql.converters.RoleConverter
import by.sorface.idp.web.graphql.services.RoleService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.*

/**
 * GraphQL контроллер для административных операций с ролями пользователей.
 * Предоставляет полный набор методов для управления ролями: создание, обновление, удаление и поиск.
 * Доступ ограничен только для пользователей с ролью администратора.
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
class AdminRoleGraphQLController(
    private val roleService: RoleService,
    private val roleConverter: RoleConverter
) {

    /**
     * Получает список всех ролей в системе.
     *
     * @return список всех ролей в формате GraphQL
     * @throws SecurityException если у текущего пользователя нет прав администратора
     */
    @QueryMapping
    fun roleGetAll(): List<GQRole> = roleService.findAll().map { roleConverter.convertToGQRole(it) }

    /**
     * Обновляет существующую роль по её идентификатору.
     *
     * @param input объект с данными для обновления роли (id, code, name)
     * @return обновленная роль в формате GraphQL или null, если роль не найдена
     * @throws SecurityException если у текущего пользователя нет прав администратора
     * @throws IllegalArgumentException если input равен null или содержит некорректные данные
     */
    @MutationMapping
    fun roleUpdate(@Argument input: GQRoleUpdateInput): GQRole? {
        val role = roleService.update(input.id, input.code, input.name)
        return role?.let { roleConverter.convertToGQRole(it) }
    }

    /**
     * Удаляет роль по её идентификатору.
     *
     * @param id уникальный идентификатор роли для удаления
     * @return результат операции удаления с информацией об успехе и сообщением
     * @throws SecurityException если у текущего пользователя нет прав администратора
     * @throws IllegalArgumentException если id равен null
     */
    @MutationMapping
    fun roleDelete(@Argument id: UUID): GQRoleDeleteResult {
        val success = roleService.delete(id)
        return GQRoleDeleteResult.builder()
            .setSuccess(success)
            .setMessage(if (success) "Role deleted successfully" else "Role not found")
            .build()
    }

    /**
     * Создает новую роль в системе.
     *
     * @param input объект с данными для создания роли (code, name)
     * @return созданная роль в формате GraphQL
     * @throws SecurityException если у текущего пользователя нет прав администратора
     * @throws IllegalArgumentException если input равен null или содержит некорректные данные
     * @throws IllegalStateException если роль с таким кодом уже существует
     */
    @MutationMapping
    fun roleCreate(@Argument input: GQRoleCreateInput): GQRole =
        roleConverter.convertToGQRole(
            roleService.create(input.code, input.name)
        )

    /**
     * Получает роль по её уникальному идентификатору.
     *
     * @param id уникальный идентификатор роли
     * @return роль в формате GraphQL или null, если роль не найдена
     * @throws SecurityException если у текущего пользователя нет прав администратора
     * @throws IllegalArgumentException если id равен null
     */
    @QueryMapping
    fun roleGetById(@Argument id: UUID): GQRole? = roleService.findById(id)?.let { roleConverter.convertToGQRole(it) }

    /**
     * Получает роль по её коду.
     *
     * @param code код роли для поиска
     * @return роль в формате GraphQL или null, если роль не найдена
     * @throws SecurityException если у текущего пользователя нет прав администратора
     * @throws IllegalArgumentException если code равен null или пустой строке
     */
    @QueryMapping
    fun roleGetByCode(@Argument code: String): GQRole? = roleService.findByCode(code)?.let { roleConverter.convertToGQRole(it) }

}
