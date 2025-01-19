package plutoproject.feature.paper.hardcoreChallenge

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import plutoproject.feature.paper.hardcoreChallenge.listeners.PlayerListener
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "hardcore_challenge",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "random_teleport", load = Load.BEFORE, required = true),
    ]
)
@Suppress("UNUSED")
class HardcoreChallengeFeature : PaperFeature() {
    override fun onEnable() {
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
    }
}
