package by.sorface.idp.web.graphql.services.impl

import by.sorface.idp.dao.sql.model.client.RegisteredClientModel
import by.sorface.idp.dao.sql.repository.client.JdbcRegisteredClientRepository
import by.sorface.idp.exceptions.GraphqlUserException
import by.sorface.idp.records.I18Codes
import by.sorface.idp.web.graphql.services.ApplicationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class DefaultApplicationService(
    private val jdbcRegisteredClientRepository: JdbcRegisteredClientRepository,
) : ApplicationService {

    @Transactional(readOnly = true)
    override fun getAll(pageable: Pageable): Page<RegisteredClientModel> = jdbcRegisteredClientRepository.findAll(pageable)

    @Transactional(readOnly = true)
    override fun getAllByUser(id: UUID): List<RegisteredClientModel> = jdbcRegisteredClientRepository.findByCreatedUser(id)

    @Transactional(readOnly = true)
    override fun getById(id: UUID): RegisteredClientModel {
        return jdbcRegisteredClientRepository.findById(id)
            .orElseThrow {
                GraphqlUserException(I18Codes.I18ClientCodes.NOT_FOUND)
            }
    }

    override fun delete(id: UUID): Boolean {
        TODO("Not yet implemented")
    }

}