package by.sorface.idp.web.rest.mapper

import by.sorface.idp.dao.sql.model.RoleModel
import by.sorface.idp.dao.sql.model.UserModel
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class UserConverterTest {

    @InjectMockKs
    private lateinit var userConverter: UserConverter

    @Test
    fun `convert UserModel to ProfileRecord`() {
        val userModel = UserModel().apply {
            this.id = UUID.fromString("e74cf6d6-3b22-47b6-adfe-5a23c1a4c08c")
            this.firstName = "firstName"
            this.lastName = "lastName"
            this.middleName = "middleName"
            this.email = "email"
            this.username = "username"
            this.avatarUrl = "avatarUrl"
            this.roles = listOf(RoleModel().apply { this.code = "ROLE_USER" })
        }

        val profileRecord = userConverter.convert(userModel)

        Assertions.assertNotNull(profileRecord)

        Assertions.assertEquals(userModel.id, profileRecord.id)
        Assertions.assertEquals(userModel.firstName, profileRecord.firstName)
        Assertions.assertEquals(userModel.lastName, profileRecord.lastName)
        Assertions.assertEquals(userModel.middleName, profileRecord.middleName)
        Assertions.assertEquals(userModel.email, profileRecord.email)
        Assertions.assertEquals(userModel.username, profileRecord.nickname)
        Assertions.assertEquals(userModel.avatarUrl, profileRecord.avatar)
        Assertions.assertTrue(profileRecord.roles.contains("ROLE_USER"))
    }

}