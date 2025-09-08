package by.sorface.idp.web.rest.facade.impl

import by.sorface.idp.dao.nosql.model.TmpRegistration
import by.sorface.idp.dao.nosql.repository.TmpRegistrationRepository
import by.sorface.idp.dao.sql.model.UserModel
import by.sorface.idp.dao.sql.model.enums.ProviderType
import by.sorface.idp.dao.sql.model.enums.UserRoles
import by.sorface.idp.dao.sql.repository.user.RoleRepository
import by.sorface.idp.dao.sql.repository.user.UserRepository
import by.sorface.idp.records.I18Codes
import by.sorface.idp.utils.OtpUtils
import by.sorface.idp.utils.json.mask.MaskerFields
import by.sorface.idp.web.rest.exceptions.I18RestException
import by.sorface.idp.web.rest.facade.RegistrationFacade
import by.sorface.idp.web.rest.model.accounts.registration.AccountOtpRefresh
import by.sorface.idp.web.rest.model.accounts.registration.AccountRegistration
import by.sorface.idp.web.rest.model.accounts.registration.AccountRegistrationResult
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

data class AccountRegistrationEvent(val otpCode: String, val locale: Locale, val username: String, val firstName: String?, val lastName: String?, val email: String)
data class AccountRegistrationConfirmEvent(val locale: Locale, val username: String, val firstName: String?, val lastName: String?, val email: String)
data class AccountRegistrationRefreshOtpEvent(
    val otpCode: String,
    val locale: Locale,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val email: String
)

@Service
class RegistrationFacadeImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tmpRegistrationRepository: TmpRegistrationRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) : RegistrationFacade {

    private val logger = LoggerFactory.getLogger(RegistrationFacadeImpl::class.java)

    override fun get(registrationId: String): AccountRegistration {
        val registration = tmpRegistrationRepository.findByIdOrNull(registrationId)

        registration ?: throw I18RestException(
            message = "Registration data not found by id $registrationId",
            i18Code = I18Codes.I18AccountRegistryCodes.ACCOUNT_DATA_NOT_FOUND_BY_ID,
            i18Args = mapOf("id" to registrationId),
            httpStatus = HttpStatus.NO_CONTENT
        )

        logger.info("getting user data for registration $registrationId")

        return AccountRegistration(
            registrationId = registrationId,
            username = registration.username,
            firstName = registration.firstName,
            lastName = registration.lastName,
            email = registration.email,
            password = registration.password,
        )
    }

    override fun create(account: AccountRegistration): AccountRegistrationResult {
        logger.info(
            "registration of a new account via an OTP code with username [${account.username}] and email " +
                    "[${MaskerFields.EMAILS.mask(account.email)}]"
        )

        if (userRepository.existsByUsername(account.username)) {
            throw I18RestException(
                message = "Username ${account.username} already exists with login",
                i18Code = I18Codes.I18UserCodes.ALREADY_EXISTS_WITH_THIS_LOGIN,
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }

        if (userRepository.existsByEmail(account.email)) {
            throw I18RestException(
                message = "Username ${account.username} already exists with email",
                i18Code = I18Codes.I18UserCodes.ALREADY_EXISTS_WITH_THIS_EMAIL,
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }

        val tmpRegistration: TmpRegistration = createOrUpdateRegistration(account, account.registrationId)

        return tmpRegistration
            .let {
                logger.info(
                    "saved registration data via an OTP code for user [username -> ${account.username}, email -> ${MaskerFields.EMAILS.mask(account.email)}]"
                )

                tmpRegistrationRepository.save(it)
            }
            .let { savedAccount ->
                logger.info(
                    "send event by new registration [id -> ${savedAccount.id}] for user [username -> ${account.username}, email -> ${MaskerFields.EMAILS.mask(account.email)}]"
                )

                val event = AccountRegistrationEvent(
                    savedAccount.otp, LocaleContextHolder.getLocale(), savedAccount.username, savedAccount.firstName, savedAccount.lastName, savedAccount.email
                )

                applicationEventPublisher.publishEvent(event)

                return@let savedAccount
            }
            .let { AccountRegistrationResult(it.id, it.otpExpTime) }
    }

    @Transactional
    override fun confirm(registrationId: String, otpCode: String) {
        logger.info("Confirming registration for registration [id -> $registrationId] and otp [code -> $otpCode]")

        val registration = tmpRegistrationRepository.findByIdOrNull(registrationId)
            ?: throw I18RestException(
                message = "Registration with id $registrationId does not exist",
                i18Code = I18Codes.I18AccountRegistryCodes.ACCOUNT_DATA_NOT_FOUND
            )

        val now = Instant.now()

        if (registration.otpExpTime.isBefore(now)) {
            throw I18RestException(
                message = "OTP has expired. [exp time -> ${registration.otpExpTime}, now -> ${now}]",
                i18Code = I18Codes.I18OtpCodes.EXPIRED_CODE
            )
        }

        if (registration.otp != otpCode) {
            throw I18RestException(
                message = "OTP has not matching code",
                i18Code = I18Codes.I18OtpCodes.INVALID_CODE
            )
        }

        if (userRepository.existsByUsername(registration.username)) {
            throw I18RestException(
                message = "Username already exist with login ${registration.username}",
                i18Code = I18Codes.I18UserCodes.ALREADY_EXISTS_WITH_THIS_LOGIN
            )
        }

        if (userRepository.existsByEmail(registration.email)) {
            throw I18RestException(
                message = "Username already exist with email ${registration.username}",
                i18Code = I18Codes.I18UserCodes.ALREADY_EXISTS_WITH_THIS_EMAIL
            )
        }

        val defaultRole = roleRepository.findFirstByCodeIgnoreCase(UserRoles.USER.value)

        val newAccount = UserModel().apply {
            this.username = registration.username
            this.email = registration.email
            this.firstName = registration.firstName
            this.lastName = registration.lastName
            this.password = passwordEncoder.encode(registration.password)
            this.roles = if (defaultRole != null) listOf(defaultRole) else emptyList()
            this.confirm = true
            this.enabled = true
            this.providerType = ProviderType.INTERNAL
        }

        userRepository.save(newAccount)
        tmpRegistrationRepository.deleteById(registrationId)

        logger.debug("registration confirmed successfully for registration [id -> $registrationId] and otp [code -> $otpCode]")

        val event = AccountRegistrationConfirmEvent(
            LocaleContextHolder.getLocale(), newAccount.username!!, newAccount.firstName, newAccount.lastName, newAccount.email!!
        )

        logger.info(
            "send event by confirm registration [id -> $registrationId] for user [username -> ${newAccount.username}, email -> ${
                MaskerFields.EMAILS.mask(newAccount.email)
            }]"
        )

        applicationEventPublisher.publishEvent(event)
    }

    override fun refreshOtp(registrationId: String): AccountOtpRefresh {
        logger.info("refreshing OTP for registration [id -> $registrationId]")

        val registration = tmpRegistrationRepository.findByIdOrNull(registrationId)
            ?: throw I18RestException(
                message = "Registration with id $registrationId does not exist",
                i18Code = I18Codes.I18AccountRegistryCodes.ACCOUNT_DATA_NOT_FOUND
            )

        return registration
            .apply {
                this.otp = OtpUtils.generateOTP(5)
                this.otpExpTime = Instant.now().plusSeconds(120)
            }
            .let {
                logger.info("saving new OTP [code -> ${it.otp}, exp time -> ${it.otpExpTime}] for registration [id -> $registrationId]")

                tmpRegistrationRepository.save(it)
            }
            .let { savedAccount ->
                logger.info(
                    "send event by OTP refresh for registration [id -> $registrationId] for user [username -> ${savedAccount.username}, email -> ${
                        MaskerFields.EMAILS.mask(
                            savedAccount.email
                        )
                    }]"
                )

                val event = AccountRegistrationRefreshOtpEvent(
                    savedAccount.otp, LocaleContextHolder.getLocale(), savedAccount.username, savedAccount.firstName, savedAccount.lastName, savedAccount.email
                )

                applicationEventPublisher.publishEvent(event)

                return@let savedAccount
            }
            .let {
                AccountOtpRefresh(it.otpExpTime)
            }
    }

    private fun createOrUpdateRegistration(account: AccountRegistration, registrationId: String?): TmpRegistration {
        var registration: TmpRegistration? = null

        if (registrationId != null) {
            logger.info("getting registration data by id $registrationId")
            registration = tmpRegistrationRepository.findByIdOrNull(registrationId)
        }

        if (registration == null) {
            logger.info("creating new registration data")

            registration = TmpRegistration()
        }

        return registration.apply {
            this.firstName = account.firstName
            this.lastName = account.lastName
            this.email = account.email
            this.username = account.username
            this.password = account.password
            this.otpExpTime = Instant.now().plusSeconds(120)
            otp = OtpUtils.generateOTP(5)
        }
    }

}