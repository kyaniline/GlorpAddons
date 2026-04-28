package com.glorpaddons.misc

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.client.render.model.BlockStateModel
import net.minecraft.client.texture.Sprite
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import java.util.function.Predicate

object CoalGlassModel {

    private val BLOCK_ATLAS_ID = Identifier.ofVanilla("blocks")
    private val COAL_GLASS_SPRITE_ID = Identifier.of("glorpaddons", "block/coal_glass")
    private const val W = 1f / 16f

    private var cachedSprite: Sprite? = null

    private fun getSprite(): Sprite? {
        cachedSprite?.let { return it }
        val client = MinecraftClient.getInstance()
        val atlas = client.atlasManager.getAtlasTexture(BLOCK_ATLAS_ID)
        val sprite = atlas.getSprite(COAL_GLASS_SPRITE_ID)
        cachedSprite = sprite
        return sprite
    }

    fun clearCache() {
        cachedSprite = null
    }

    fun register() {
        ModelLoadingPlugin.register { ctx ->
            ctx.modifyBlockModelAfterBake().register { model, context ->
                if (context.state().isOf(Blocks.COAL_BLOCK)) {
                    CoalGlassWrapper(model)
                } else {
                    model
                }
            }
        }
    }

    private fun perpDirections(face: Direction): Array<Direction> = when (face.axis) {
        Direction.Axis.Y -> arrayOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
        Direction.Axis.Z -> arrayOf(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST)
        Direction.Axis.X -> arrayOf(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)
        else -> emptyArray()
    }

    // For each (face, edge) pair, returns the (left, bottom, right, top) bounds passed to
    // QuadEmitter.square() to produce a 1/16-thick sub-quad along the requested edge of the face.
    private fun edgeBounds(face: Direction, edge: Direction): FloatArray {
        val top = floatArrayOf(0f, 1f - W, 1f, 1f)
        val bot = floatArrayOf(0f, 0f, 1f, W)
        val lft = floatArrayOf(0f, 0f, W, 1f)
        val rgt = floatArrayOf(1f - W, 0f, 1f, 1f)
        return when (face) {
            Direction.UP -> when (edge) {
                Direction.NORTH -> top; Direction.SOUTH -> bot
                Direction.WEST -> lft;  Direction.EAST -> rgt
                else -> top
            }
            Direction.DOWN -> when (edge) {
                Direction.NORTH -> bot; Direction.SOUTH -> top
                Direction.WEST -> lft;  Direction.EAST -> rgt
                else -> top
            }
            Direction.NORTH -> when (edge) {
                Direction.UP -> top;    Direction.DOWN -> bot
                Direction.EAST -> lft;  Direction.WEST -> rgt
                else -> top
            }
            Direction.SOUTH -> when (edge) {
                Direction.UP -> top;    Direction.DOWN -> bot
                Direction.EAST -> rgt;  Direction.WEST -> lft
                else -> top
            }
            Direction.WEST -> when (edge) {
                Direction.UP -> top;    Direction.DOWN -> bot
                Direction.NORTH -> lft; Direction.SOUTH -> rgt
                else -> top
            }
            Direction.EAST -> when (edge) {
                Direction.UP -> top;    Direction.DOWN -> bot
                Direction.NORTH -> rgt; Direction.SOUTH -> lft
                else -> top
            }
        }
    }

    private class CoalGlassWrapper(original: BlockStateModel) : WrapperBlockStateModel(original) {

        override fun emitQuads(
            emitter: QuadEmitter,
            blockView: BlockRenderView,
            pos: BlockPos,
            state: BlockState,
            random: Random,
            cullTest: Predicate<Direction?>
        ) {
            if (!MiscConfigManager.config.fuckCoalEnabled) {
                super.emitQuads(emitter, blockView, pos, state, random, cullTest)
                return
            }

            val sprite = getSprite() ?: run {
                super.emitQuads(emitter, blockView, pos, state, random, cullTest)
                return
            }

            for (face in Direction.entries) {
                if (cullTest.test(face)) continue
                val faceNeighbor = blockView.getBlockState(pos.offset(face))
                if (faceNeighbor.isOf(Blocks.COAL_BLOCK)) continue

                for (edge in perpDirections(face)) {
                    val edgeNeighbor = blockView.getBlockState(pos.offset(edge))
                    if (edgeNeighbor.isOf(Blocks.COAL_BLOCK)) {
                        // Hide the border if the adjacent coal block's same-direction face is
                        // also rendered (i.e., the diagonal block is air). Otherwise the seam
                        // would either connect to nothing (corner is occluding) or to another
                        // coal that we cull anyway (corner is coal).
                        val cornerState = blockView.getBlockState(pos.offset(edge).offset(face))
                        if (cornerState.isAir) continue
                    }

                    val b = edgeBounds(face, edge)
                    emitter.square(face, b[0], b[1], b[2], b[3], 0f)
                    emitter.spriteBake(sprite, QuadEmitter.BAKE_LOCK_UV)
                    emitter.color(-1, -1, -1, -1)
                    emitter.renderLayer(BlockRenderLayer.CUTOUT)
                    emitter.emit()
                }
            }
        }

        override fun createGeometryKey(
            blockView: BlockRenderView,
            pos: BlockPos,
            state: BlockState,
            random: Random
        ): Any? {
            if (!MiscConfigManager.config.fuckCoalEnabled) {
                return super.createGeometryKey(blockView, pos, state, random)
            }
            return null
        }
    }
}
