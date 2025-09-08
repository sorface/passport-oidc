package by.sorface.idp.dao.sql.repository.user

import by.sorface.idp.dao.sql.model.RoleModel
import by.sorface.idp.dao.sql.repository.BaseRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : BaseRepository<RoleModel> {

    fun findFirstByCodeIgnoreCase(code: String): RoleModel?

}
