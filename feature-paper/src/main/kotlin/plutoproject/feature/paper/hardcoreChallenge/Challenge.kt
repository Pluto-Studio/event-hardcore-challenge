package plutoproject.feature.paper.hardcoreChallenge

import ink.pmc.advkt.component.keybind
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import kotlinx.coroutines.delay
import net.coreprotect.CoreProtect
import net.kyori.adventure.util.Ticks
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.advancement.Advancement
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportManager
import plutoproject.feature.paper.api.randomTeleport.RandomTeleportOptions
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.common.util.roundTo2
import plutoproject.framework.common.util.time.currentTimestampMillis
import plutoproject.framework.common.util.time.ticks
import plutoproject.framework.common.util.trimmedString
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.world.location.Position2D
import java.math.BigDecimal
import java.time.Instant
import kotlin.random.Random

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

val PotionEffectWhenRespawn = PotionEffect(
    PotionEffectType.RESISTANCE,
    10 * 20,
    255,
    true,
    true
)

val Player.isInChallenge: Boolean get() = this in inChallenge

fun Player.hasTotemOfUndying(): Boolean = inventory.contents
    .filterNotNull()
    .any { it.type == Material.TOTEM_OF_UNDYING }

suspend fun startChallenge(player: Player) {
    inChallenge.add(player)
    var model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (!model.isInChallenge) {
        model = model.copy(
            isInChallenge = true,
            joinedChallengeBefore = true,
            lastRoundStart = currentTimestampMillis
        )
        UserRepository.saveOrUpdate(model)
        onChallengeStart(player)
    }
}

suspend fun stopChallenge(player: Player, restart: Boolean = true) {
    inChallenge.remove(player)
    var model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
    if (model.isInChallenge) {
        model = model.copy(isInChallenge = false, failedRounds = model.failedRounds + 1)
        UserRepository.saveOrUpdate(model)
        player.send {
            text("你已失败 ") with mochaSubtext0
            text("${model.failedRounds} ") with mochaText
            text("轮挑战") with mochaSubtext0
        }
        onChallengeFailed(player, restart)
    }
}

suspend fun onChallengeStart(player: Player) = player.withSync {
    player.health = player.attributeMaxHealth
    player.foodLevel = 20
    player.saturation = 5f
    player.exhaustion = 0f
    player.updateMaxHealth(2.0)
    player.resetAdvancements()
    player.clearActivePotionEffects()
    player.inventory.clear()
    performRandomTeleport(player)
    player.addPotionEffect(PotionEffectWhenRespawn)
    player.gameMode = GameMode.SURVIVAL
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

suspend fun onChallengeFailed(player: Player, restart: Boolean = true) = player.withSync {
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
    if (!restart) return@withSync
    delay(60.ticks)
    player.send {
        text("正在为你开启新一轮挑战...") with mochaSubtext0
    }
    player.clearTitle()
    runAsync {
        val model = UserRepository.findById(player.uniqueId) ?: UserModel(player.uniqueId)
        if (model.lastRoundStart == null) return@runAsync
        player.rollbackActions(Instant.ofEpochMilli(model.lastRoundStart))
    }
    startChallenge(player)
}

suspend fun performRandomTeleport(player: Player) {
    if (RandomTeleportManager.isInCooldown(player)) {
        RandomTeleportManager.getCooldown(player)?.finish()
    }
    RandomTeleportManager.launchSuspend(player, player.world, ChallengeRandomTeleportOptions)
}

val ChallengeHealthModifierKey = NamespacedKey(plugin, "challenge_health")

fun getChallengeHealthModifier(add: Double) =
    AttributeModifier(ChallengeHealthModifierKey, add, AttributeModifier.Operation.ADD_NUMBER)

val Player.attributeMaxHealth get() = getAttribute(Attribute.MAX_HEALTH)!!.value

fun Player.updateMaxHealth(amount: Double) {
    val attribute = getAttribute(Attribute.MAX_HEALTH) ?: return
    if (amount >= 20.0) {
        attribute.removeModifier(ChallengeHealthModifierKey)
        return
    }
    val modifierNumber = amount - attribute.baseValue
    attribute.removeModifier(ChallengeHealthModifierKey)
    attribute.addModifier(getChallengeHealthModifier(modifierNumber))
}

fun Player.addMaxHealth(amount: Double) {
    updateMaxHealth(attributeMaxHealth + amount)
}

suspend fun Player.getCoin(): BigDecimal {
    val model = UserRepository.findById(uniqueId) ?: UserModel(uniqueId)
    return model.coin
}

suspend fun Player.setCoin(amount: BigDecimal) {
    val model = UserRepository.findById(uniqueId) ?: UserModel(uniqueId)
    UserRepository.saveOrUpdate(model.copy(coin = model.coin + amount))
}

suspend fun Player.addCoin(amount: BigDecimal) {
    setCoin(getCoin() + amount)
}

fun Player.resetAdvancements() {
    server.advancementIterator().forEach {
        val progress = getAdvancementProgress(it)
        progress.awardedCriteria.forEach { criteria ->
            progress.revokeCriteria(criteria)
        }
    }
}

fun Player.rollbackActions(start: Instant) {
    val end = Instant.now()
    if (end < start) {
        return
    }
    val startMills = start.toEpochMilli()
    val endMills = end.toEpochMilli()
    val secs = ((endMills - startMills) / 1000).toInt()
    CoreProtect.getInstance().api.performRollback(
        secs,
        listOf(name),
        null,
        null,
        null,
        null,
        0,
        null,
    )
}

suspend fun Player.giveAdvancementReward(advancement: Advancement) {
    val key = advancement.key.key
    if (key.startsWith("recipes") || key.contains("/root")) return
    val beforeHealthReward = attributeMaxHealth
    val healthReward = Random.nextDouble(0.0, 1.5).roundTo2()
    val coinReward = Random.nextDouble(1.0, 20.0).roundTo2().toBigDecimal()
    addMaxHealth(healthReward)
    addCoin(coinReward)
    send {
        text("进度达成！你因此获得了 ") with mochaPink
        if (attributeMaxHealth > beforeHealthReward) {
            text("${healthReward.trimmedString()} ") with mochaText
            text("点额外生命值与 ") with mochaPink
        }
        text("$coinReward ") with mochaText
        text("个挑战币") with mochaPink
    }
}

val UndeadNegativeEffect = PotionEffect(
    PotionEffectType.WITHER,
    5 * 20,
    3,
    true,
    true
)

fun Player.applyUndeadNegativeEffect() {
    addPotionEffect(UndeadNegativeEffect)
}
