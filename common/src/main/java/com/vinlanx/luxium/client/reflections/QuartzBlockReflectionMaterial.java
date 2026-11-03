package com.vinlanx.luxium.client.reflections;
import com.vinlanx.luxium.client.reflections.ReflectionMaterial;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
public final class QuartzBlockReflectionMaterial
implements ReflectionMaterial {
    private static final Set<Block> QUARTZ_BLOCKS = Set.of(Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BRICKS);







    @Override
    public boolean supports(BlockState state) {
        return QUARTZ_BLOCKS.contains(state.getBlock());
    }

    @Override
    public boolean shouldReflectFace(Minecraft mc, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean useRaycastVisibility() {
        return false;
    }

    @Override
    public boolean useCornerVisibilitySamples() {
        return false;
    }

    @Override
    public float getBaseAlpha() {
        return 0.2f;
    }









    @Override
    public float adjustVertexAlpha(float alpha, float projectedU, float projectedV, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        float edgeDistance = Math.min(Math.min
                (projectedU, 1.0f - projectedU), Math.min
                (projectedV, 1.0f - projectedV));

        float edgeFade = QuartzBlockReflectionMaterial.smoothstep(-0.1f, 0.08f, edgeDistance);
        return alpha * (0.35f + 0.65f * edgeFade);
    }











    @Override
    public float warpU(float u, float v, float worldX, float worldY, float worldZ, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        float scatter = QuartzBlockReflectionMaterial.quartzScatterScale(pos, face, cameraPos);
        float grainA = ReflectionMaterial.hashToUnit(pos.getX(), pos.getY(), pos.getZ(), face.get3DDataValue() * 17 + vertexIndex * 31) - 0.5f;
        float grainB = ReflectionMaterial.hashToUnit(pos.getZ(), pos.getX(), pos.getY(), face.get3DDataValue() * 13 + vertexIndex * 29) - 0.5f;
        float lowFreq = (float)Math.sin(worldX * 0.9f + worldZ * 1.3f + worldY * 0.4f) * (0.0035f * scatter);
        return ReflectionMaterial.clampUv(u + grainA * (0.015f * scatter) + grainB * (0.006f * scatter) + lowFreq);
    }











    @Override
    public float warpV(float u, float v, float worldX, float worldY, float worldZ, BlockPos pos, Direction face, int vertexIndex, Vec3 cameraPos) {
        float scatter = QuartzBlockReflectionMaterial.quartzScatterScale(pos, face, cameraPos);
        float grainA = ReflectionMaterial.hashToUnit(pos.getY(), pos.getZ(), pos.getX(), face.get3DDataValue() * 19 + vertexIndex * 37) - 0.5f;
        float grainB = ReflectionMaterial.hashToUnit(pos.getX(), pos.getY(), pos.getZ(), face.get3DDataValue() * 23 + vertexIndex * 41) - 0.5f;
        float lowFreq = (float)Math.cos(worldZ * 1.1f + worldX * 0.6f + worldY * 0.7f) * (0.0035f * scatter);
        return ReflectionMaterial.clampUv(v + grainA * (0.015f * scatter) + grainB * (0.006f * scatter) + lowFreq);
    }

    private static float quartzScatterScale(BlockPos pos, Direction face, Vec3 cameraPos) {
        Vec3 faceCenter = Vec3.atCenterOf((Vec3i)pos).add((double)face.getStepX
                () * 0.5, (double)face.getStepY
                () * 0.5, (double)face.getStepZ
                () * 0.5);

        float distance = (float)cameraPos.distanceTo(faceCenter);
        return 0.35f + 0.65f * QuartzBlockReflectionMaterial.smoothstep(1.0f, 4.0f, distance);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0f : 0.0f;
        }
        float t = (value - edge0) / (edge1 - edge0);
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }
}
