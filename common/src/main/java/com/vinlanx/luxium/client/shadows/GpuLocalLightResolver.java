package com.vinlanx.luxium.client.shadows;




import com.vinlanx.luxium.rtx.Coloredlight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
public final class GpuLocalLightResolver {
    static final int META_CENTER_OFFSET_X = 10;
    static final int META_CENTER_OFFSET_Y = 11;
    static final int META_CENTER_OFFSET_Z = 14;
    static final int META_BLOCKED_FACE_MASK = 15;
    static final int META_BOX_EMITTER = 18;
    static final int META_VISIBILITY_SLOT = 19;
    private static final double FIXTURE_HORIZONTAL_OFFSET = 0.28;
    private static final double TORCH_VERTICAL_OFFSET = 0.2;
    private static final double END_ROD_OFFSET = 0.32;
    private GpuLocalLightResolver() { }

    public static void resolve(ClientLevel level, long sourceKey, int emission, float[] output, int base) {
        float red = 1.0f;
        float green = 0.76f;
        float blue = 0.48f;

        if (level != null) {
            BlockPos pos = BlockPos.of((long)sourceKey);
            BlockState state = level.getBlockState(pos);
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String path = id == null ? "" : id.getPath();

            if (path.contains("soul") && path.contains("torch") || GpuLocalLightResolver.containsAny
                    (path, "soul_lantern", "soul_fire", "soul_campfire")) {
                red = 0.28f; green = 0.78f; blue = 1.0f;
            } else if (path.contains("redstone") && path.contains("torch") || GpuLocalLightResolver.containsAny
                    (path, "redstone_ore", "deepslate_redstone_ore")) {
                red = 1.0f; green = 0.2f; blue = 0.08f;
            } else if (GpuLocalLightResolver.containsAny(path, "sea_lantern", "conduit", "prismarine")) {
                red = 0.58f; green = 0.95f; blue = 1.0f;
            } else if (GpuLocalLightResolver.containsAny(path, "end_rod", "pearlescent_froglight")) {
                red = 1.0f; green = 0.72f; blue = 0.95f;
            } else if (GpuLocalLightResolver.containsAny(path, "verdant_froglight")) {
                red = 0.68f; green = 1.0f; blue = 0.66f;
            } else if (GpuLocalLightResolver.containsAny(path, "ochre_froglight", "shroomlight", "glowstone")) {
                red = 1.0f; green = 0.82f; blue = 0.48f;
            } else if (GpuLocalLightResolver.containsAny(path, "amethyst", "budding_amethyst")) {
                red = 0.78f; green = 0.48f; blue = 1.0f;
            } else if (GpuLocalLightResolver.containsAny(path, "sculk", "respawn_anchor")) {
                red = 0.2f; green = 0.78f; blue = 0.92f;
            } else if (GpuLocalLightResolver.containsAny(path, "lava", "fire", "campfire", "magma", "furnace", "blast_furnace", "smoker", "jack_o_lantern", "torch", "lantern", "candle")) {

                red = 1.0f; green = 0.58f; blue = 0.26f;
            } else if (GpuLocalLightResolver.containsAny(path, "glow_lichen", "glow_berries")) {
                red = 0.72f; green = 1.0f; blue = 0.58f;
            } else if (GpuLocalLightResolver.containsAny(path, "beacon", "light")) {
                red = 1.0f; green = 0.98f; blue = 0.9f;
            }
        }

        if (Coloredlight.isEnabled()) {
            red = Coloredlight.getRedMultiplier();
            green = Coloredlight.getGreenMultiplier();
            blue = Coloredlight.getBlueMultiplier();
        }

        float normalizedEmission = Mth.clamp((float)((float)emission / 15.0f), (float)0.0f, (float)1.0f);

        float intensity = 1.18f * (float)Math.pow(normalizedEmission, 0.78);

        output[base] = red * intensity;
        output[base + 1] = green * intensity;
        output[base + 2] = blue * intensity;
        output[base + 3] = 1.0f;
    }

    static SourceGeometry resolveGeometry(ClientLevel level, long sourceKey) {
        int blockX = BlockPos.getX((long)sourceKey);
        int blockY = BlockPos.getY((long)sourceKey);
        int blockZ = BlockPos.getZ((long)sourceKey);
        double centerX = (double)blockX + 0.5;
        double centerY = (double)blockY + 0.5;
        double centerZ = (double)blockZ + 0.5;
        double sourceX = centerX;
        double sourceY = centerY;
        double sourceZ = centerZ;
        boolean boxEmitter = false;
        int blockedFaceMask = 0;

        if (level != null) {
            BlockPos pos = BlockPos.of((long)sourceKey);
            BlockState state = level.getBlockState(pos);
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String path = id == null ? "" : id.getPath();
            boxEmitter = state.isCollisionShapeFullBlock((BlockGetter)level, pos);

            if (path.contains("torch")) {
                boxEmitter = false;
                Direction facing = GpuLocalLightResolver.fixtureFacing(state);
                if (facing == null) {
                    sourceY += 0.2;
                } else if (facing.getAxis().isHorizontal()) {
                    sourceX += (double)facing.getStepX() * 0.28;
                    sourceY += 0.2;
                    sourceZ += (double)facing.getStepZ() * 0.28;
                } else {
                    sourceX += (double)facing.getStepX() * 0.28;
                    sourceY += (double)facing.getStepY() * 0.28;
                    sourceZ += (double)facing.getStepZ() * 0.28;
                }
            } else if (path.contains("end_rod")) {
                boxEmitter = false;
                Direction facing = GpuLocalLightResolver.fixtureFacing(state);
                if (facing != null) {
                    sourceX += (double)facing.getStepX() * 0.32;
                    sourceY += (double)facing.getStepY() * 0.32;
                    sourceZ += (double)facing.getStepZ() * 0.32;
                }
            } else if (GpuLocalLightResolver.isFixtureLantern(path)) {
                boxEmitter = false;
                boolean hanging = state.hasProperty((Property)BlockStateProperties.HANGING) &&
                        (Boolean)state.getValue((Property)BlockStateProperties.HANGING) != false;
                sourceY += hanging ? -0.12 : 0.12;
            } else if (path.contains("candle")) {
                boxEmitter = false;
                sourceY += 0.18;
            }

            blockedFaceMask = GpuLocalLightResolver.resolveBlockedFaceMask(level, pos);
        }

        return new SourceGeometry(sourceX, sourceY, sourceZ, centerX, centerY, centerZ, boxEmitter, blockedFaceMask);
    }


    static void writeGeometryMetadata(SourceGeometry geometry, float[] output, int rectBase) {
        output[rectBase + 10] = (float)(geometry.centerX - geometry.sourceX);
        output[rectBase + 11] = (float)(geometry.centerY - geometry.sourceY);
        output[rectBase + 14] = (float)(geometry.centerZ - geometry.sourceZ);
        output[rectBase + 15] = geometry.blockedFaceMask;
        output[rectBase + 18] = geometry.boxEmitter ? 1.0f : 0.0f;
    }

    private static Direction fixtureFacing(BlockState state) {
        if (state.hasProperty((Property)BlockStateProperties.HORIZONTAL_FACING)) {
            return (Direction)state.getValue((Property)BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty((Property)BlockStateProperties.FACING)) {
            return (Direction)state.getValue((Property)BlockStateProperties.FACING);
        }
        return null;
    }

    private static int resolveBlockedFaceMask(ClientLevel level, BlockPos sourcePos) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = sourcePos.relative(direction);
            BlockState neighbour = level.getBlockState(neighbourPos);
            if (!neighbour.canOcclude() || !neighbour.isCollisionShapeFullBlock((BlockGetter)level, neighbourPos))
                    continue;

            mask |= (switch (direction) { default -> throw new IncompatibleClassChangeError();
                case EAST -> 1;
                case WEST -> 2;
                case UP -> 4;
                case DOWN -> 8;
                case SOUTH -> 16;
                case NORTH -> 32;
            });
        }
        return mask;
    }

    private static boolean isFixtureLantern(String path) {
        return path.contains("lantern") && !path.contains
                ("sea_lantern") && !path.contains
                ("jack_o_lantern");
    }

    private static boolean containsAny(String value, String ... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) { return true; }
        }
        return false;
    }




    static final class SourceGeometry {
        final double sourceX;
        final double sourceY;
        final double sourceZ;
        final double centerX;
        final double centerY;
        final double centerZ;
        final boolean boxEmitter;
        final int blockedFaceMask;
        SourceGeometry(double sourceX, double sourceY, double sourceZ, double centerX, double centerY, double centerZ, boolean boxEmitter, int blockedFaceMask) {
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.sourceZ = sourceZ;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.boxEmitter = boxEmitter;
            this.blockedFaceMask = blockedFaceMask;
        }
    }
}
