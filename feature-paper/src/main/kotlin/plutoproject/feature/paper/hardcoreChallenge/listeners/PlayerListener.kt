package plutoproject.feature.paper.hardcoreChallenge.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.paper.hardcoreChallenge.inChallenge
import plutoproject.feature.paper.hardcoreChallenge.startChallenge
import plutoproject.feature.paper.hardcoreChallenge.stopChallenge

object PlayerListener : Listener {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        startChallenge(player)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        inChallenge.remove(player)
    }

    @EventHandler
    suspend fun PlayerDeathEvent.e() {
        stopChallenge(player)
    }
}
