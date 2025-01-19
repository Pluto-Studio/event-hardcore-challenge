package plutoproject.feature.paper.hardcoreChallenge

import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.mainTitle
import org.bukkit.GameMode
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportOptions
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.chat.palettes.mochaPink
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
    inChallenge.add(player)
    val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (!model.isInChallenge) {
        UserRepository.saveOrUpdate(model.copy(isInChallenge = true))
        onChallengeStart(player)
    }
}

suspend fun stopChallenge(player: Player) {
    inChallenge.remove(player)
    val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (model.isInChallenge) {
        UserRepository.saveOrUpdate(model.copy(isInChallenge = false))
        onChallengeFailed(player)
    }
}

suspend fun onChallengeStart(player: Player) {
    player.gameMode = GameMode.SURVIVAL
    performRandomTeleport(player)
    player.send {
        text("挑战已开始，利用规则存活下去吧！") with mochaPink
    }
}

suspend fun onChallengeFailed(player: Player) {
    player.gameMode = GameMode.SPECTATOR
    player.showTitle {
        mainTitle {
            text("\uD83D\uDC80 挑战失败") with mochaMaroon
        }
    }
}

suspend fun performRandomTeleport(player: Player) {
    RandomTeleportManager.launchSuspend(player, player.world, ChallengeRandomTeleportOptions)
}
