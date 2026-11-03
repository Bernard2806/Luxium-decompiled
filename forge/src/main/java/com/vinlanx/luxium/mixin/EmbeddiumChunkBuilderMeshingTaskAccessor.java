package com.vinlanx.luxium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
    value = {ChunkBuilderMeshingTask.class},
    remap = false
)
public interface EmbeddiumChunkBuilderMeshingTaskAccessor {
    @Accessor("render")
    RenderSection luxium$getRender();
}
