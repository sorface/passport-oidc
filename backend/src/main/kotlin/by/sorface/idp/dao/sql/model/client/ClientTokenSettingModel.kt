package by.sorface.idp.dao.sql.model.client

import by.sorface.idp.dao.sql.model.BaseModel
import jakarta.persistence.*
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat

/**
 * Класс модели настроек токена клиента.
 * Этот класс представляет таблицу "T_CLIENTTOKENSETTINGSTORE" в базе данных.
 */
@Entity
@Table(name = "T_CLIENTTOKENSETTINGSTORE")
class ClientTokenSettingModel : BaseModel() {

    /**
     * Формат токена доступа.
     * Это поле указывает формат токена доступа, который должен быть использован.
     * Значение по умолчанию - SELF_CONTAINED.
     */
    @Column(name = "C_ACCESSTOKENFORMAT", nullable = false)
    var accessTokenFormat: String = OAuth2TokenFormat.SELF_CONTAINED.value

    /**
     * Алгоритм подписи токена идентификации.
     * Это поле указывает алгоритм подписи, который должен быть использован для токена идентификации.
     * Значение по умолчанию - RS256.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "C_IDTOKENSIGNATUREALGORITHM", nullable = false)
    var idTokenSignatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.RS256

    /**
     * Время жизни токена доступа.
     * Это поле указывает время жизни токена доступа в секундах.
     * Значение по умолчанию - 3600 секунд (1 час).
     */
    @Column(name = "C_ACCESSTOKENTIMETOLIVE", nullable = false)
    var accessTokenTimeToLive: Long = 3600

    /**
     * Время жизни токена обновления.
     * Это поле указывает время жизни токена обновления в секундах.
     * Значение по умолчанию - 604800 секунд (7 дней).
     */
    @Column(name = "C_REFRESHTOKENTIMETOLIVE", nullable = false)
    var refreshTokenTimeToLive: Long = 604800

    /**
     * Повторное использование токенов обновления.
     * Это поле указывает, следует ли повторно использовать токены обновления.
     * Значение по умолчанию - true.
     */
    @Column(name = "C_REUSEREFRESHTOKENS", nullable = false)
    var reuseRefreshTokens: Boolean = true

    /**
     * Приложение клиент
     **/
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "C_ID")
    var registeredClient: RegisteredClientModel? = null

}