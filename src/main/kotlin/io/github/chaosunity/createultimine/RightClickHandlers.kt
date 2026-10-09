package io.github.chaosunity.createultimine

import com.simibubi.create.AllRecipeTypes
import com.simibubi.create.AllTags
import com.simibubi.create.content.equipment.wrench.WrenchItem
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe
import dev.ftb.mods.ftbultimine.FTBUltiminePlayerData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.event.entity.player.PlayerInteractEvent

/**
 * Helpers that apply Create's "manual application" (deployer-on-stick, e.g. andesite alloy on
 * stripped logs/casings, sandpaper, ...) and Create wrench interactions to every position cached
 * by FTB Ultimine.
 *
 * Ported from the 1.20-forge (v1.3.x) implementation; adapted to the FTB Ultimine 1.18.2 API:
 *  - [FTBUltiminePlayerData.cachedBlocks] and [FTBUltiminePlayerData.pressed] are public fields
 *    on 1.18.2 instead of cachedPositions()/isPressed() methods.
 *  - AllTags.AllItemTags.WRENCH exposes a TagKey (tag) field instead of a matches() method.
 */
object RightClickHandlers {
    fun isManualApplicable(level: Level, blockState: BlockState, heldItem: ItemStack): ManualApplicationRecipe? {
        val recipeType: RecipeType<ManualApplicationRecipe> = AllRecipeTypes.ITEM_APPLICATION.getType()

        return level.recipeManager
            .getAllRecipesFor(recipeType)
            .firstOrNull { it.testBlock(blockState) && it.ingredients[1].test(heldItem) }
    }

    fun itemApplication(
        player: ServerPlayer,
        hand: InteractionHand,
        blockHitResult: BlockHitResult,
        data: FTBUltiminePlayerData,
    ): Int {
        var didWork = 0
        val positions = data.cachedBlocks ?: return 0

        for (pos in positions) {
            val simulatedClickEvent = PlayerInteractEvent.RightClickBlock(
                player,
                hand,
                pos,
                blockHitResult.withPosition(pos)
            )
            ManualApplicationRecipe.manualApplicationRecipesApplyInWorld(simulatedClickEvent)

            if (simulatedClickEvent.isCancelable && simulatedClickEvent.isCanceled) didWork++
            else break
        }

        return didWork
    }

    fun onWrenchUse(
        player: ServerPlayer,
        hand: InteractionHand,
        blockHitResult: BlockHitResult,
        data: FTBUltiminePlayerData
    ): Int {
        var didWork = 0
        val itemStack = player.getItemInHand(hand)

        if (itemStack.item !is WrenchItem ||
            !itemStack.`is`(AllTags.AllItemTags.WRENCH.tag))
            return 0

        val isPressed = data.pressed
        data.pressed = false

        for (pos in data.cachedBlocks ?: emptyList()) {
            val currentHitResult = blockHitResult.withPosition(pos)
            val context = UseOnContext(player, hand, currentHitResult)
            val result = itemStack.useOn(context)

            if (result != InteractionResult.SUCCESS)
                continue

            didWork++
        }

        data.pressed = isPressed

        return didWork
    }
}
