package com.glorpaddons.misc

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.SlabBlock
import net.minecraft.block.enums.SlabType
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

    // Adjusts the band bounds when the edge neighbor is a slab so the frame aligns with
    // the slab's geometry. Returns null when the slab is positioned such that no flush
    // wall meets the coal block at this edge.
    private fun edgeBoundsForNeighbor(face: Direction, edge: Direction, neighbor: BlockState): FloatArray? {
        val base = edgeBounds(face, edge)
        if (neighbor.block !is SlabBlock) return base
        val slabType = neighbor.get(SlabBlock.TYPE)
        if (slabType == SlabType.DOUBLE) return base
        val isBottom = slabType == SlabType.BOTTOM
        val faceIsVertical = face.axis != Direction.Axis.Y

        if (faceIsVertical) {
            return when (edge) {
                Direction.UP -> if (isBottom) base else null
                Direction.DOWN -> if (!isBottom) base else null
                else -> if (isBottom) {
                    floatArrayOf(base[0], 0f, base[2], 0.5f)
                } else {
                    floatArrayOf(base[0], 0.5f, base[2], 1f)
                }
            }
        }
        return when (face) {
            Direction.UP -> if (!isBottom) base else null
            Direction.DOWN -> if (isBottom) base else null
            else -> base
        }
    }

    // When a slab is the face neighbor of a vertical face, the slab covers half of the
    // boundary plane. Returns a horizontal band on the visible portion of the coal face,
    // sitting flush against the slab's profile boundary (y=0.5 in face-local coords).
    private fun interiorSlabBand(face: Direction, faceNeighbor: BlockState): FloatArray? {
        if (faceNeighbor.block !is SlabBlock) return null
        val slabType = faceNeighbor.get(SlabBlock.TYPE)
        if (slabType == SlabType.DOUBLE) return null
        if (face.axis == Direction.Axis.Y) return null

        val isBottom = slabType == SlabType.BOTTOM
        return if (isBottom) {
            floatArrayOf(0f, 0.5f, 1f, 0.5f + W)
        } else {
            floatArrayOf(0f, 0.5f - W, 1f, 0.5f)
        }
    }

    private fun emitBand(emitter: QuadEmitter, sprite: Sprite, face: Direction, b: FloatArray) {
        emitter.square(face, b[0], b[1], b[2], b[3], 0f)
        emitter.spriteBake(sprite, QuadEmitter.BAKE_LOCK_UV)
        emitter.color(-1, -1, -1, -1)
        emitter.renderLayer(BlockRenderLayer.CUTOUT)
        emitter.emit()
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

            val slabAbove = blockView.getBlockState(pos.offset(Direction.UP)).let { ns ->
                ns.block is SlabBlock && ns.get(SlabBlock.TYPE) != SlabType.DOUBLE
            }

            for (face in Direction.entries) {
                if (cullTest.test(face)) continue
                val faceNeighbor = blockView.getBlockState(pos.offset(face))
                if (faceNeighbor.isOf(Blocks.COAL_BLOCK)) continue

                interiorSlabBand(face, faceNeighbor)?.let {
                    emitBand(emitter, sprite, face, it)
                }

                // For each perpendicular slab at the same y-level, draw a horizontal band
                // at the slab's profile boundary (y=0.5) — this is "where the slab connects".
                val sideSlabTypes = if (face.axis != Direction.Axis.Y) {
                    perpDirections(face)
                        .filter { it.axis != Direction.Axis.Y }
                        .mapNotNull {
                            val ns = blockView.getBlockState(pos.offset(it))
                            if (ns.block is SlabBlock) {
                                val t = ns.get(SlabBlock.TYPE)
                                if (t != SlabType.DOUBLE) t else null
                            } else null
                        }
                        .toSet()
                } else emptySet()

                for (slabType in sideSlabTypes) {
                    val band = if (slabType == SlabType.BOTTOM) {
                        floatArrayOf(0f, 0.5f, 1f, 0.5f + W)
                    } else {
                        floatArrayOf(0f, 0.5f - W, 1f, 0.5f)
                    }
                    emitBand(emitter, sprite, face, band)
                }

                // Skip the cube's regular bottom edge band whenever a slab is connected
                // somewhere other than directly below — the slab's own connection edges
                // (top band for an above slab, the y=0.5 band emitted above for a side
                // slab) take its place.
                val skipBottomEdge = perpDirections(face)
                    .filter { it != Direction.DOWN }
                    .any { dir ->
                        val ns = blockView.getBlockState(pos.offset(dir))
                        ns.block is SlabBlock && ns.get(SlabBlock.TYPE) != SlabType.DOUBLE
                    }

                for (edge in perpDirections(face)) {
                    if (skipBottomEdge && edge == Direction.DOWN) continue

                    val edgeNeighbor = blockView.getBlockState(pos.offset(edge))
                    if (edgeNeighbor.isOf(Blocks.COAL_BLOCK)) {
                        // Hide the seam when the diagonal corner is air (the adjacent coal's
                        // same-direction face renders, producing a duplicate) or when it's a
                        // slab (we treat the connected coal+slab structure as one mass).
                        val cornerState = blockView.getBlockState(pos.offset(edge).offset(face))
                        if (cornerState.isAir || cornerState.block is SlabBlock) continue
                        emitBand(emitter, sprite, face, edgeBounds(face, edge))
                        continue
                    }

                    val b = edgeBoundsForNeighbor(face, edge, edgeNeighbor) ?: continue
                    emitBand(emitter, sprite, face, b)
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
