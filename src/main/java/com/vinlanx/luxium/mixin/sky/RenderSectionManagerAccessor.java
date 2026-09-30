/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ReferenceMap
 *  me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSection
 *  me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.vinlanx.luxium.mixin.sky;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={RenderSectionManager.class}, remap=false)
public interface RenderSectionManagerAccessor {
    @Accessor(value="sectionByPosition")
    public Long2ReferenceMap<RenderSection> luxium$getSectionByPosition();

    @Accessor(value="chunkRenderer")
    public ChunkRenderer luxium$getChunkRenderer();
}

