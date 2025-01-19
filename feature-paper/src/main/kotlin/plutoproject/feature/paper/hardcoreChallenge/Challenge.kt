package plutoproject.feature.paper.hardcoreChallenge

import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import kotlinx.coroutines.delay
import net.kyori.adventure.util.Ticks
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportOptions
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.common.util.time.ticks
import plutoproject.framework.paper.util.world.location.Position2D

const val CHALLENGE_NO_AUTO_START_PERMISSION = "hardcore.no_auto_start"

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

val Player.isInChallenge: Boolean get() = this in inChallenge

fun Player.hasTotemOfUndying(): Boolean = inventory.contents
    .filterNotNull()
    .any { it.type == Material.TOTEM_OF_UNDYING }

suspend fun startChallenge(player: Player) {
    inChallenge.add(player)
    val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (!model.isInChallenge) {
        UserRepository.saveOrUpdate(model.copy(isInChallenge = true))
        onChallengeStart(player)
    }
}

suspend fun stopChallenge(player: Player, restart: Boolean = true) {
    inChallenge.remove(player)
    val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (model.isInChallenge) {
        UserRepository.saveOrUpdate(model.copy(isInChallenge = false, failedRounds = model.failedRounds + 1))
        player.send {
            text("你已失败 ") with mochaSubtext0
            text("${model.failedRounds + 1} ") with mochaText
            text("轮挑战") with mochaSubtext0
        }
        onChallengeFailed(player, restart)
    }
}

suspend fun onChallengeStart(player: Player) {
    player.gameMode = GameMode.SURVIVAL
    performRandomTeleport(player)
    player.send {
        text("挑战已开始，利用规则存活下去吧！") with mochaPink
    }
    player.send {
        text("你可以按下 ") with mochaText
        keybind("key.sneak") with mochaLavender
        text(" + ") with mochaLavender
        keybind("key.swapOffhand") with mochaLavender
        text(" 或 ") with mochaText
        text("/menu ") with mochaLavender
        text("来打开「挑战手账」") with mochaText
    }
}

suspend fun onChallengeFailed(player: Player, restart: Boolean = true) {
    player.gameMode = GameMode.SPECTATOR
    player.showTitle {
        mainTitle {
            text("\uD83D\uDC80 挑战失败") with mochaMaroon
        }
        times {
            fadeIn(Ticks.duration(5))
            stay(Ticks.duration(35))
            fadeOut(Ticks.duration(20))
        }
    }
    if (!restart) return
    delay(60.ticks)
    player.send {
        text("正在为你开启新一轮挑战...") with mochaSubtext0
    }
    player.clearTitle()
    startChallenge(player)
}

suspend fun performRandomTeleport(player: Player) {
    RandomTeleportManager.launchSuspend(player, player.world, ChallengeRandomTeleportOptions)
}
