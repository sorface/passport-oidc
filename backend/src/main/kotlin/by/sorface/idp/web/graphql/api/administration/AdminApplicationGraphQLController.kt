package by.sorface.idp.web.graphql.api.administration

import by.sorface.idp.dao.sql.model.client.RegisteredClientModel
import by.sorface.idp.graphql.model.GQApplicationUpdateInput
import by.sorface.idp.web.graphql.services.ApplicationService
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
class AdminApplicationGraphQLController(private val applicationService: ApplicationService) {

    @QueryMapping
    fun applicationGetAll(@Argument page: Int?, @Argument size: Int?): List<RegisteredClientModel> {
        val pageNumber = page ?: 0
        val pageSize = size ?: 20

        val pageable = PageRequest.of(pageNumber, pageSize)

        return applicationService.getAll(pageable).content
    }

    @MutationMapping
    fun applicationUpdate(@Argument application: GQApplicationUpdateInput): RegisteredClientModel? {
        return null
    }

}