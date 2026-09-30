/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.vinlanx.luxium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets={"org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask"})
public interface EmbeddiumChunkBuilderMeshingTaskAccessor {
    @Accessor(value="render")
    public Object luxium$getRender();
}

