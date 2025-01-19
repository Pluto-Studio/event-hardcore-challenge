package plutoproject.feature.paper.hardcoreChallenge

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.provider.getCollection
import java.util.*

object UserRepository {
    private val collection = Provider.getCollection<UserModel>("hardcore_challenge_users")
    private val replaceOptions = ReplaceOptions().upsert(true)

    suspend fun findById(uniqueId: UUID): UserModel? =
        collection.find(eq("uniqueId", uniqueId)).firstOrNull()

    suspend fun saveOrUpdate(model: UserModel) {
        collection.replaceOne(eq("uniqueId", model.uniqueId), model, replaceOptions)
    }
}
