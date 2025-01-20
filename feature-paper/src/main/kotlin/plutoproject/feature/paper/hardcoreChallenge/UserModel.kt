package plutoproject.feature.paper.hardcoreChallenge

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.JavaUuidBsonSerializer
import java.math.BigDecimal
import java.util.*

@Serializable
data class UserModel(
    @Serializable(JavaUuidBsonSerializer::class) val uniqueId: UUID,
    val isInChallenge: Boolean = false,
    val joinedChallengeBefore: Boolean = false,
    val failedRounds: Int = 0,
    val lastRoundStart: Long? = null,
    @Serializable(BigDecimalSerializer::class) val coin: BigDecimal = BigDecimal.ZERO,
)
