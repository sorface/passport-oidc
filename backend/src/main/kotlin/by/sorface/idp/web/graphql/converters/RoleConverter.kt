package by.sorface.idp.web.graphql.converters

import by.sorface.idp.dao.sql.model.RoleModel
import by.sorface.idp.graphql.model.GQRole
import org.springframework.stereotype.Component

/**
 * Конвертер для преобразования моделей ролей в GraphQL объекты.
 */
@Component
class RoleConverter {

    /**
     * Конвертирует RoleModel в GQRole.
     * 
     * @param roleModel модель роли из базы данных
     * @return GraphQL объект роли
     */
    fun convertToGQRole(roleModel: RoleModel): GQRole {
        return GQRole.builder()
            .setId(roleModel.id)
            .setCode(roleModel.code ?: "")
            .setName(roleModel.name)
            .build()
    }
}
