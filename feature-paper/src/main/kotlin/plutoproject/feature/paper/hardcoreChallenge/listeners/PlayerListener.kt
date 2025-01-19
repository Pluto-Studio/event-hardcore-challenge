package plutoproject.feature.paper.hardcoreChallenge.listeners

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.paper.hardcoreChallenge.*
import plutoproject.framework.paper.util.coroutine.runSync

object PlayerListener : Listener {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        if (!player.hasPermission(CHALLENGE_NO_AUTO_START_PERMISSION)) {
            startChallenge(player)
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        inChallenge.remove(player)
    }

    @EventHandler
    suspend fun PlayerDeathEvent.e() {
        isCancelled = true
        player.health = player.getAttribute(Attribute.MAX_HEALTH)!!.value
        stopChallenge(player)
    }

    @EventHandler
    fun EntityDamageEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        if (!player.isInChallenge) return
        if (finalDamage < player.health) return
        if (!player.hasTotemOfUndying()) return
        val inventory = player.inventory
        if (inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING
            || inventory.itemInOffHand.type == Material.TOTEM_OF_UNDYING
        ) return
        val totemSlot = inventory.first(Material.TOTEM_OF_UNDYING)
        val totem = inventory.getItem(totemSlot) ?: return
        val keepOffhandItem = inventory.itemInOffHand.clone()
        inventory.removeItem(totem)
        inventory.setItemInOffHand(totem)
        runSync {
            inventory.setItemInOffHand(keepOffhandItem)
        }
    }
}
