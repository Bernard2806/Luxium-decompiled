/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Camera
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.culling.Frustum
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package com.vinlanx.luxium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={LevelRenderer.class})
public interface LevelRendererAccessor {
    @Invoker(value="setupRender")
    public void luxium$setupRender(Camera var1, Frustum var2, boolean var3, boolean var4);

    @Invoker(value="compileChunks")
    public void luxium$compileChunks(Camera var1);

    @Invoker(value="renderChunkLayer")
    public void luxium$renderChunkLayer(RenderType var1, PoseStack var2, double var3, double var5, double var7, Matrix4f var9);

    @Accessor(value="cullingFrustum")
    public Frustum luxium$getCullingFrustum();
}

