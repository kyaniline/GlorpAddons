package com.glorpaddons.mobesp

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.passive.BatEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.joml.Matrix4f
import kotlin.math.cos
import kotlin.math.sin

object BatEsp {

    // Pipeline: position+color shader, DEBUG_LINES draw mode, no depth test (through walls)
    private val ESP_PIPELINE: RenderPipeline by lazy {
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("glorpaddons", "bat_esp"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .build()
    }

    private val ESP_LAYER: RenderLayer by lazy {
        RenderLayer.of(
            "bat_esp",
            1536,
            ESP_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder()
                .build(RenderLayer.OutlineMode.NONE)
        )
    }

    fun register() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            render(context)
        }
    }

    private fun render(context: WorldRenderContext) {
        if (!MobEspConfigManager.config.batEspEnabled) return

        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val player = client.player ?: return

        val bats = world.getEntities().filterIsInstance<BatEntity>()
        if (bats.isEmpty()) return

        val consumers = context.consumers() ?: return
        val buf = consumers.getBuffer(ESP_LAYER)
        val matrix = context.matrices().peek().positionMatrix

        val camPos = context.worldState().cameraRenderState.pos
        val camX = camPos.x
        val camY = camPos.y
        val camZ = camPos.z

        // Per-frame tick delta for smooth sub-tick interpolation
        val delta = client.getRenderTickCounter().getTickProgress(true).toDouble()

        // Green highlight box around every bat (through walls)
        for (bat in bats) {
            val lx = MathHelper.lerp(delta, bat.lastX, bat.x)
            val ly = MathHelper.lerp(delta, bat.lastY, bat.y)
            val lz = MathHelper.lerp(delta, bat.lastZ, bat.z)
            val dx = lx - bat.x; val dy = ly - bat.y; val dz = lz - bat.z
            val bb = bat.boundingBox
            val x1 = (bb.minX + dx - camX).toFloat()
            val y1 = (bb.minY + dy - camY).toFloat()
            val z1 = (bb.minZ + dz - camZ).toFloat()
            val x2 = (bb.maxX + dx - camX).toFloat()
            val y2 = (bb.maxY + dy - camY).toFloat()
            val z2 = (bb.maxZ + dz - camZ).toFloat()
            drawBoxEdges(buf, matrix, x1, y1, z1, x2, y2, z2)
        }

        // Single tracer to the nearest bat
        val nearest = bats.minByOrNull { it.squaredDistanceTo(player) } ?: return
        val nlx = MathHelper.lerp(delta, nearest.lastX, nearest.x)
        val nly = MathHelper.lerp(delta, nearest.lastY, nearest.y)
        val nlz = MathHelper.lerp(delta, nearest.lastZ, nearest.z)
        val ndx = nlx - nearest.x; val ndy = nly - nearest.y; val ndz = nlz - nearest.z
        val nbb = nearest.boundingBox
        val tx = ((nbb.minX + nbb.maxX) / 2.0 + ndx - camX).toFloat()
        val ty = ((nbb.minY + nbb.maxY) / 2.0 + ndy - camY).toFloat()
        val tz = ((nbb.minZ + nbb.maxZ) / 2.0 + ndz - camZ).toFloat()

        // Tracer starts from just in front of the camera (avoids near-clip at eye origin).
        // Compute the camera's 3D forward direction from yaw/pitch, then offset by 0.1 units.
        val camera = client.gameRenderer.camera
        val yawRad = Math.toRadians(camera.yaw.toDouble())
        val pitchRad = Math.toRadians(camera.pitch.toDouble())
        val fwdX = (-sin(yawRad) * cos(pitchRad)).toFloat()
        val fwdY = (-sin(pitchRad)).toFloat()
        val fwdZ = (cos(yawRad) * cos(pitchRad)).toFloat()
        val near = 0.1f
        buf.vertex(matrix, fwdX * near, fwdY * near, fwdZ * near).color(0, 255, 0, 255)
        buf.vertex(matrix, tx, ty, tz).color(0, 255, 0, 255)
    }

    private fun drawBoxEdges(
        buf: VertexConsumer, matrix: Matrix4f,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float
    ) {
        // Bottom face
        line(buf, matrix, x1, y1, z1, x2, y1, z1)
        line(buf, matrix, x2, y1, z1, x2, y1, z2)
        line(buf, matrix, x2, y1, z2, x1, y1, z2)
        line(buf, matrix, x1, y1, z2, x1, y1, z1)
        // Top face
        line(buf, matrix, x1, y2, z1, x2, y2, z1)
        line(buf, matrix, x2, y2, z1, x2, y2, z2)
        line(buf, matrix, x2, y2, z2, x1, y2, z2)
        line(buf, matrix, x1, y2, z2, x1, y2, z1)
        // Vertical edges
        line(buf, matrix, x1, y1, z1, x1, y2, z1)
        line(buf, matrix, x2, y1, z1, x2, y2, z1)
        line(buf, matrix, x2, y1, z2, x2, y2, z2)
        line(buf, matrix, x1, y1, z2, x1, y2, z2)
    }

    private fun line(
        buf: VertexConsumer, matrix: Matrix4f,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float
    ) {
        buf.vertex(matrix, x1, y1, z1).color(0, 255, 0, 255)
        buf.vertex(matrix, x2, y2, z2).color(0, 255, 0, 255)
    }
}
