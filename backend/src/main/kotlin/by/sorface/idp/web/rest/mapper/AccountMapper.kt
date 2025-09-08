package by.sorface.idp.web.rest.mapper

import by.sorface.idp.dao.sql.model.UserModel
import by.sorface.idp.web.rest.model.accounts.Account
import by.sorface.idp.web.rest.model.sessions.AccountSession
import org.springframework.session.Session
import org.springframework.stereotype.Component

@Component
class UserConverter {

    fun convert(userModel: UserModel): Account {
        return Account(
            userModel.id,
            userModel.username,
            userModel.email,
            userModel.firstName,
            userModel.lastName,
            userModel.middleName,
            userModel.avatarUrl,
            userModel.roles.mapNotNull { it.code }
        )
    }

}

@Component
class UserSessionConverter {

    fun convert(activeId: String, session: Session): AccountSession {
        return AccountSession()
            .apply {
                id = session.id
                createdAt = session.creationTime.toEpochMilli()
                active = activeId.equals(session.id, ignoreCase = true)
            }
    }

}