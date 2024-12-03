package ink.pmc.essentials.screens.teleport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.COMMAND_TPAHERE_SUCCEED
import ink.pmc.essentials.COMMAND_TPA_SUCCEED
import ink.pmc.essentials.api.teleport.TeleportDirection
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.inventory.*
import ink.pmc.framework.interactive.inventory.click.clickable
import ink.pmc.framework.interactive.inventory.layout.list.ListMenu
import ink.pmc.framework.interactive.inventory.layout.list.ListMenuOptions
import ink.pmc.framework.utils.chat.DURATION
import ink.pmc.framework.utils.chat.UI_SUCCEED_SOUND
import ink.pmc.framework.utils.chat.replace
import ink.pmc.framework.utils.dsl.itemStack
import ink.pmc.framework.utils.visual.*
import ink.pmc.framework.utils.world.aliasOrName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.meta.SkullMeta
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

class TeleportRequestScreen : ListMenu<Player, TeleportRequestScreenModel>(
    options = ListMenuOptions(title = Component.text("选择玩家"))
) {
    @Composable
    override fun modelProvider(): TeleportRequestScreenModel {
        val player = LocalPlayer.current
        return TeleportRequestScreenModel(player)
    }

    @Composable
    override fun MenuLayout() {
        val model = model.current
        LaunchedEffect(model.onlinePlayers.size) {
            model.loadPageContents()
        }
        super.MenuLayout()
    }

    @Composable
    override fun Element(obj: Player) {
        val model = model.current
        val coroutineScope = rememberCoroutineScope()
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        val manager = koinInject<TeleportManager>()
        if (model.isRequestSent && model.requestSentTo != obj) {
            Spacer(modifier = Modifier.height(1).width(1))
            return
        }
        Item(
            itemStack = itemStack(Material.PLAYER_HEAD) {
                displayName = if (model.isRequestSent) component {
                    text("√ 已发送") with mochaGreen without italic()
                } else component {
                    text(obj.name) with mochaFlamingo without italic()
                }
                lore(
                    if (model.isRequestSent) {
                        emptyList()
                    } else buildList {
                        add(component {
                            val world = obj.world.aliasOrName
                            val x = obj.location.blockX
                            val y = obj.location.blockY
                            val z = obj.location.blockZ
                            text("$world $x, $y, $z") with mochaSubtext0 without italic()
                        })
                        add(Component.empty())
                        add(component {
                            text("左键 ") with mochaLavender without italic()
                            text("请求传送至其位置") with mochaText without italic()
                        })
                        add(component {
                            text("右键 ") with mochaLavender without italic()
                            text("请求其传送至你这里") with mochaText without italic()
                        })
                    }
                )
                meta {
                    this as SkullMeta
                    playerProfile = obj.playerProfile
                    setEnchantmentGlintOverride(model.requestSentTo == obj)
                }
            },
            modifier = Modifier.clickable {
                if (model.isRequestSent || model.requestSentTo != null) return@clickable
                if (TeleportManager.hasUnfinishedRequest(obj)) return@clickable
                val direction = when (clickType) {
                    ClickType.LEFT -> TeleportDirection.GO
                    ClickType.RIGHT -> TeleportDirection.COME
                    else -> return@clickable
                }
                val message = when (direction) {
                    TeleportDirection.GO -> COMMAND_TPA_SUCCEED
                    TeleportDirection.COME -> COMMAND_TPAHERE_SUCCEED
                }
                TeleportManager.createRequest(player, obj, direction)
                model.isRequestSent = true
                model.requestSentTo = obj
                coroutineScope.launch {
                    delay(1.seconds)
                    navigator.pop()
                }
                player.playSound(UI_SUCCEED_SOUND)
                player.sendMessage(
                    message
                        .replace("<player>", obj.name)
                        .replace("<expire>", DURATION(manager.defaultRequestOptions.expireAfter))
                )
            }
        )
    }
}