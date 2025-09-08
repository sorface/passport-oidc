package by.sorface.idp.dao.sql.model.client

import by.sorface.idp.dao.sql.model.BaseModel
import jakarta.persistence.*

@Entity
@Table(name = "T_CLIENTREDIRECTURLSTORE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "C_TYPE", discriminatorType = DiscriminatorType.STRING)
abstract class RedirectUrlModel : BaseModel() {

    /**
     * URL перенаправления клиента.
     * Это поле не может быть null и должно быть уникальным.
     */
    @Column(name = "C_URL", nullable = false, unique = true)
    var url: String? = null

    /**
     * Связь с зарегистрированным клиентом.
     * Это поле представляет отношение "многие к одному" с RegisteredClientModel.
     * Загрузка связанных данных выполняется лениво.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "C_FK_REGISTEREDCLIENT")
    var registeredClient: RegisteredClientModel? = null

    /**
     * Тип URL-адреса перенаправления
     */
    @Column(name = "C_TYPE", insertable = false, updatable = false)
    var type: String? = null

}
