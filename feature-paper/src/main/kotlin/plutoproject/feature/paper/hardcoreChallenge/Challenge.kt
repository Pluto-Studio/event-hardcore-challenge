package plutoproject.feature.paper.hardcoreChallenge

import org.bukkit.entity.Player
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportOptions
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.paper.util.world.location.Position2D

val inChallenge = mutableConcurrentSetOf<Player>()

val ChallengeRandomTeleportOptions = RandomTeleportOptions(
    center = Position2D(0.0, 0.0),
    spawnPointAsCenter = false,
    chunkPreserveRadius = 0,
    cacheAmount = 10,
    startRadius = 0,
    endRadius = 50000,
    maxHeight = 192,
    minHeight = 63,
    noCover = true,
    maxAttempts = 5,
    cost = 0.0,
    blacklistedBiomes = emptySet(),
)

suspend fun startChallenge(player: Player) {
    val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (model.isInChallenge) return
    UserRepository.saveOrUpdate(model.copy(isInChallenge = true))
    inChallenge.add(player)
}

suspend fun stopChallenge(player: Player) {
    val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (!model.isInChallenge) return
    UserRepository.saveOrUpdate(model.copy(isInChallenge = false))
    inChallenge.remove(player)
}

suspend fun performRandomTeleport(player: Player) {
    RandomTeleportManager.launch(player, player.world, ChallengeRandomTeleportOptions)
}
