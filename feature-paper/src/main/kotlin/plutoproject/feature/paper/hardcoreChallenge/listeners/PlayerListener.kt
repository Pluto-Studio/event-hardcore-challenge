package plutoproject.feature.paper.hardcoreChallenge.listeners

import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.paper.hardcoreChallenge.inChallenge
import plutoproject.feature.paper.hardcoreChallenge.startChallenge
import plutoproject.framework.common.util.chat.palettes.mochaPink

object PlayerListener : Listener {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        startChallenge(player)
        player.send {
            text("挑战已开始，利用规则存活下去吧！") with mochaPink
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        inChallenge.remove(player)
    }
}
