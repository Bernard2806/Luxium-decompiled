package com.vinlanx.luxium.client.reflections; import com.vinlanx.luxium.client.reflections.IronBlockReflectionMaterial;
import com.vinlanx.luxium.client.reflections.PolishedAndesiteReflectionMaterial;
import com.vinlanx.luxium.client.reflections.QuartzBlockReflectionMaterial;
import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
public final class ReflectionMaterialRegistry {
    private static final List<ReflectionMaterial> MATERIALS = new ArrayList<ReflectionMaterial>();

    static {
        MATERIALS.add(new IronBlockReflectionMaterial());
        MATERIALS.add(new QuartzBlockReflectionMaterial());
        MATERIALS.add(new PolishedAndesiteReflectionMaterial());
    }

    private ReflectionMaterialRegistry() {
    }

    @Nullable
    public static ReflectionMaterial find(BlockState state) {
        for (ReflectionMaterial material : MATERIALS) {
            if (material.supports(state)) {
                return material;
            }
        }
        return null;
    }

    public static List<ReflectionMaterial> viewMaterials() {
        return Collections.unmodifiableList(MATERIALS);
    }
}
