package io.github.chaosunity.createultimine

import io.github.chaosunity.createultimine.config.CreateUltimineServerConfig
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.spongepowered.asm.mixin.Mixins

@Mod(CreateUltimine.ID)
class CreateUltimine {
    companion object {
        const val ID = "createultimine"

        val LOGGER: Logger = LogManager.getLogger(ID)

        private const val MIXIN_CONFIG = "mixins.createultimine.json"
    }

    init {
        // In a production jar the "MixinConfigs" manifest attribute registers the config. In a dev
        // environment the mod is loaded straight from a classpath folder that has no manifest, so
        // register it here instead. Calling this twice is harmless (Mixin de-duplicates).
        Mixins.addConfiguration(MIXIN_CONFIG)

        MinecraftForge.EVENT_BUS.addListener(::onServerStarting)
    }

    private fun onServerStarting(event: ServerStartingEvent) {
        // Load the server-specific SNBT config from the world's serverconfig directory.
        // The actual Ultimine behaviour is handled by UltimineMixin (server side only).
        CreateUltimineServerConfig.load(event.server)
    }
}
