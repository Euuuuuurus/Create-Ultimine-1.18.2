package io.github.chaosunity.createultimine.config

import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig
import net.minecraft.server.MinecraftServer

/**
 * Server-side configuration, stored in the world's serverconfig directory as
 * createultimine-server.snbt (same file name as the 1.20.x/1.21.x releases).
 *
 * Ported to FTB Library 1.18.2 API: SNBTConfig#getGroup / getBoolean instead of
 * addGroup / addBoolean.
 */
object CreateUltimineServerConfig {
    @JvmStatic
    val CONFIG: SNBTConfig = SNBTConfig.create("createultimine-server").comment<SNBTConfig>(
        "Server-specific configuration for Create Ultimine",
        "This file is meant for server administrators to control user behaviour.",
        "Changes in this file currently require a server restart to take effect"
    )

    @JvmStatic
    val FEATURES: SNBTConfig = CONFIG.getGroup("features")

    @JvmStatic
    val RIGHT_CLICK_ALLOY: BooleanValue = FEATURES
        .getBoolean("right_click_alloy", true)
        .comment("Right-click with an alloy ingot (e.g. andesite alloy) with the Ultimine key held to apply on stripped logs or casings")

    @JvmStatic
    val RIGHT_CLICK_WRENCH: BooleanValue = FEATURES
        .getBoolean("right_click_wrench", true)
        .comment("Right-click with an wrench with the Ultimine key held to interact blocks (e.g. rotate, breaking)")

    fun load(server: MinecraftServer) {
        ConfigUtil.loadDefaulted(
            CONFIG,
            server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR),
            "createultimine"
        )
    }
}
