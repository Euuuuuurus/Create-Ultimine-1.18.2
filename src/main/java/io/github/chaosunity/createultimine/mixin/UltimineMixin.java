package io.github.chaosunity.createultimine.mixin;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbultimine.FTBUltimine;
import dev.ftb.mods.ftbultimine.FTBUltiminePlayerData;
import dev.ftb.mods.ftbultimine.shape.ShapeContext;
import io.github.chaosunity.createultimine.RightClickHandlers;
import io.github.chaosunity.createultimine.config.CreateUltimineServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Mixin into FTB Ultimine's right-click handler for MC 1.18.2 (FTB Ultimine 1802.3.x).
 *
 * The 1.18.2 method signature differs from 1.20.x:
 *   EventResult blockRightClick(Player player, InteractionHand hand, BlockPos clickPos, Direction face)
 * and the local variables right after `updateBlocks(...)` is assigned are:
 *   serverPlayer, result (HitResult), data (FTBUltiminePlayerData), shapeContext (ShapeContext)
 *
 * FTB Ultimine 1.18.2 has no CooldownTracker / addPendingXPCost (those were added in 2001.x),
 * so this port simply swings the arm and cancels the right-click when work was done.
 *
 * All targets are mod classes (never obfuscated on 1.17+), so remap = false and no refmap is needed.
 */
@Mixin(value = FTBUltimine.class, remap = false)
public class UltimineMixin {
    @Inject(method = "blockRightClick",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Ldev/ftb/mods/ftbultimine/FTBUltiminePlayerData;updateBlocks(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;ZI)Ldev/ftb/mods/ftbultimine/shape/ShapeContext;"
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false)
    private void injectBlockRightClick(
            Player player,
            InteractionHand hand,
            BlockPos clickPos,
            Direction face,
            CallbackInfoReturnable<EventResult> cir,
            ServerPlayer serverPlayer,
            HitResult result,
            FTBUltiminePlayerData data,
            ShapeContext shapeContext) {
        if (shapeContext != null && data.pressed && data.cachedBlocks != null && !data.cachedBlocks.isEmpty()) {
            Level level = serverPlayer.level;
            ManualApplicationRecipe recipe = RightClickHandlers.INSTANCE
                    .isManualApplicable(
                            level,
                            level.getBlockState(clickPos),
                            serverPlayer.getItemInHand(hand)
                    );
            int didWork = 0;

            if (CreateUltimineServerConfig.getRIGHT_CLICK_ALLOY().get() && recipe != null) {
                didWork = RightClickHandlers.INSTANCE.itemApplication(serverPlayer, hand, (BlockHitResult) result, data);
            } else if (CreateUltimineServerConfig.getRIGHT_CLICK_WRENCH().get() &&
                    serverPlayer.getItemInHand(hand).getItem() == AllItems.WRENCH.get()) {
                didWork = RightClickHandlers.INSTANCE.onWrenchUse(serverPlayer, hand, (BlockHitResult) result, data);
            }

            if (didWork > 0) {
                player.swing(hand);
                cir.setReturnValue(EventResult.interruptFalse());
            }
        }
    }
}
