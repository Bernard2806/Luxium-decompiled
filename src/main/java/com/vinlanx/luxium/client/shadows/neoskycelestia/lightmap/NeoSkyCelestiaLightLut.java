/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.renderer.LightTexture
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL15
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia.lightmap;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaLighting;
import com.vinlanx.luxium.client.shadows.neoskycelestia.lightmap.NeoSkyLightTextureExtension;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.LightTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public final class NeoSkyCelestiaLightLut
implements AutoCloseable {
    private static final int NORMAL_COUNT = 7;
    private static final int TILE_SIZE = 16;
    private static final int WIDTH = 32;
    private static final int HEIGHT = 112;
    private final FloatBuffer pixels = BufferUtils.createFloatBuffer((int)14336);
    private final int[] mainLightPixels = new int[256];
    private final int[] blockOnlyLightPixels = new int[256];
    private boolean cacheValid;
    private long lastLightmapRevision = Long.MIN_VALUE;
    private long snapshottedLightmapRevision = Long.MIN_VALUE;
    private boolean lastLightingEnabled;
    private boolean lastShadowsEnabled;
    private int lastDirectionX;
    private int lastDirectionY;
    private int lastDirectionZ;
    private int lastDirectColorX;
    private int lastDirectColorY;
    private int lastDirectColorZ;
    private int lastSkyAmbientX;
    private int lastSkyAmbientY;
    private int lastSkyAmbientZ;
    private int lastGroundAmbientX;
    private int lastGroundAmbientY;
    private int lastGroundAmbientZ;
    private int lastDirectStrength;
    private int lastAmbientStrength;
    private int lastSunShadowBlockVisibility;
    private int lastConfigHash;
    private static final int BUFFER_COUNT = 3;
    private final int[] textureIds = new int[3];
    private int currentTextureIndex = -1;

    public int update(LightTexture lightTexture, NeoSkyCelestiaLighting.State lighting, boolean shadowsEnabled, float sunShadowBlockVisibility) {
        RenderSystem.assertOnRenderThread();
        if (!(lightTexture instanceof NeoSkyLightTextureExtension)) {
            return -1;
        }
        NeoSkyLightTextureExtension extension = (NeoSkyLightTextureExtension)lightTexture;
        this.ensureTextures();
        long lightmapRevision = extension.neosky$getLightmapRevision();
        int configHash = NeoSkyCelestiaLightLut.lightingConfigHash();
        if (this.matchesKey(lightmapRevision, lighting, shadowsEnabled, sunShadowBlockVisibility)) {
            return this.textureId();
        }
        this.snapshotLightmaps(extension, lightmapRevision);
        this.pixels.clear();
        for (int normalCode = 0; normalCode < 7; ++normalCode) {
            float nx = NeoSkyCelestiaLightLut.normalX(normalCode);
            float ny = NeoSkyCelestiaLightLut.normalY(normalCode);
            float nz = NeoSkyCelestiaLightLut.normalZ(normalCode);
            float rawNdotL = nx * lighting.direction().x + ny * lighting.direction().y + nz * lighting.direction().z;
            for (int skyLevel = 0; skyLevel < 16; ++skyLevel) {
                int index;
                int blockLevel;
                int row = skyLevel << 4;
                for (blockLevel = 0; blockLevel < 16; ++blockLevel) {
                    index = row | blockLevel;
                    this.putEndpoint(this.mainLightPixels[index], this.blockOnlyLightPixels[index], this.blockOnlyLightPixels[row], this.mainLightPixels[row], ny, rawNdotL, lighting, shadowsEnabled, sunShadowBlockVisibility, 0.0f);
                }
                for (blockLevel = 0; blockLevel < 16; ++blockLevel) {
                    index = row | blockLevel;
                    this.putEndpoint(this.mainLightPixels[index], this.blockOnlyLightPixels[index], this.blockOnlyLightPixels[row], this.mainLightPixels[row], ny, rawNdotL, lighting, shadowsEnabled, sunShadowBlockVisibility, 1.0f);
                }
            }
        }
        this.pixels.flip();
        this.currentTextureIndex = (this.currentTextureIndex + 1) % 3;
        int uploadTexture = this.textureIds[this.currentTextureIndex];
        int oldActive = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture((int)33990);
        GL15.glBindBuffer((int)35052, (int)0);
        GL11.glPixelStorei((int)3317, (int)1);
        GL11.glPixelStorei((int)3314, (int)0);
        GL11.glPixelStorei((int)3315, (int)0);
        GL11.glPixelStorei((int)3316, (int)0);
        GlStateManager._bindTexture((int)uploadTexture);
        GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)32, (int)112, (int)6408, (int)5126, (FloatBuffer)this.pixels);
        GlStateManager._bindTexture((int)0);
        GL11.glPixelStorei((int)3317, (int)4);
        GL11.glPixelStorei((int)3314, (int)0);
        GL11.glPixelStorei((int)3315, (int)0);
        GL11.glPixelStorei((int)3316, (int)0);
        GL15.glBindBuffer((int)35052, (int)0);
        GlStateManager._activeTexture((int)oldActive);
        this.rememberKey(lightmapRevision, lighting, shadowsEnabled, sunShadowBlockVisibility, configHash);
        return this.textureId();
    }

    public int textureId() {
        return this.currentTextureIndex >= 0 ? this.textureIds[this.currentTextureIndex] : 0;
    }

    private void ensureTextures() {
        if (this.textureIds[0] != 0) {
            return;
        }
        int oldActive = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture((int)33990);
        for (int i = 0; i < 3; ++i) {
            this.textureIds[i] = GlStateManager._genTexture();
            GlStateManager._bindTexture((int)this.textureIds[i]);
            GL11.glTexImage2D((int)3553, (int)0, (int)34842, (int)32, (int)112, (int)0, (int)6408, (int)5126, (long)0L);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
            GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        }
        GlStateManager._bindTexture((int)0);
        GlStateManager._activeTexture((int)oldActive);
    }

    private void snapshotLightmaps(NeoSkyLightTextureExtension extension, long revision) {
        if (this.snapshottedLightmapRevision == revision) {
            return;
        }
        for (int skyLevel = 0; skyLevel < 16; ++skyLevel) {
            int row = skyLevel << 4;
            for (int blockLevel = 0; blockLevel < 16; ++blockLevel) {
                int index = row | blockLevel;
                this.mainLightPixels[index] = extension.neosky$getMainLightPixel(blockLevel, skyLevel);
                this.blockOnlyLightPixels[index] = extension.neosky$getBlockOnlyLightPixel(blockLevel, skyLevel);
            }
        }
        this.snapshottedLightmapRevision = revision;
    }

    private boolean matchesKey(long lightmapRevision, NeoSkyCelestiaLighting.State lighting, boolean shadowsEnabled, float sunShadowBlockVisibility) {
        return this.cacheValid && this.lastLightmapRevision == lightmapRevision && this.lastLightingEnabled == lighting.enabled() && this.lastShadowsEnabled == shadowsEnabled && this.lastDirectionX == NeoSkyCelestiaLightLut.bits(lighting.direction().x) && this.lastDirectionY == NeoSkyCelestiaLightLut.bits(lighting.direction().y) && this.lastDirectionZ == NeoSkyCelestiaLightLut.bits(lighting.direction().z) && this.lastDirectColorX == NeoSkyCelestiaLightLut.bits(lighting.directColor().x) && this.lastDirectColorY == NeoSkyCelestiaLightLut.bits(lighting.directColor().y) && this.lastDirectColorZ == NeoSkyCelestiaLightLut.bits(lighting.directColor().z) && this.lastSkyAmbientX == NeoSkyCelestiaLightLut.bits(lighting.shadowSkyAmbientColor().x) && this.lastSkyAmbientY == NeoSkyCelestiaLightLut.bits(lighting.shadowSkyAmbientColor().y) && this.lastSkyAmbientZ == NeoSkyCelestiaLightLut.bits(lighting.shadowSkyAmbientColor().z) && this.lastGroundAmbientX == NeoSkyCelestiaLightLut.bits(lighting.shadowGroundAmbientColor().x) && this.lastGroundAmbientY == NeoSkyCelestiaLightLut.bits(lighting.shadowGroundAmbientColor().y) && this.lastGroundAmbientZ == NeoSkyCelestiaLightLut.bits(lighting.shadowGroundAmbientColor().z) && this.lastDirectStrength == NeoSkyCelestiaLightLut.bits(lighting.directStrength()) && this.lastAmbientStrength == NeoSkyCelestiaLightLut.bits(lighting.ambientStrength()) && this.lastSunShadowBlockVisibility == NeoSkyCelestiaLightLut.bits(sunShadowBlockVisibility);
    }

    private void rememberKey(long lightmapRevision, NeoSkyCelestiaLighting.State lighting, boolean shadowsEnabled, float sunShadowBlockVisibility, int configHash) {
        this.lastLightmapRevision = lightmapRevision;
        this.lastLightingEnabled = lighting.enabled();
        this.lastShadowsEnabled = shadowsEnabled;
        this.lastDirectionX = NeoSkyCelestiaLightLut.bits(lighting.direction().x);
        this.lastDirectionY = NeoSkyCelestiaLightLut.bits(lighting.direction().y);
        this.lastDirectionZ = NeoSkyCelestiaLightLut.bits(lighting.direction().z);
        this.lastDirectColorX = NeoSkyCelestiaLightLut.bits(lighting.directColor().x);
        this.lastDirectColorY = NeoSkyCelestiaLightLut.bits(lighting.directColor().y);
        this.lastDirectColorZ = NeoSkyCelestiaLightLut.bits(lighting.directColor().z);
        this.lastSkyAmbientX = NeoSkyCelestiaLightLut.bits(lighting.shadowSkyAmbientColor().x);
        this.lastSkyAmbientY = NeoSkyCelestiaLightLut.bits(lighting.shadowSkyAmbientColor().y);
        this.lastSkyAmbientZ = NeoSkyCelestiaLightLut.bits(lighting.shadowSkyAmbientColor().z);
        this.lastGroundAmbientX = NeoSkyCelestiaLightLut.bits(lighting.shadowGroundAmbientColor().x);
        this.lastGroundAmbientY = NeoSkyCelestiaLightLut.bits(lighting.shadowGroundAmbientColor().y);
        this.lastGroundAmbientZ = NeoSkyCelestiaLightLut.bits(lighting.shadowGroundAmbientColor().z);
        this.lastDirectStrength = NeoSkyCelestiaLightLut.bits(lighting.directStrength());
        this.lastAmbientStrength = NeoSkyCelestiaLightLut.bits(lighting.ambientStrength());
        this.lastSunShadowBlockVisibility = NeoSkyCelestiaLightLut.bits(sunShadowBlockVisibility);
        this.lastConfigHash = configHash;
        this.cacheValid = true;
    }

    private static int bits(float value) {
        return Float.floatToIntBits(value);
    }

    private static int lightingConfigHash() {
        int hash = 1;
        hash = NeoSkyCelestiaLightLut.mixHash(hash, Config.isFeatureEnabled(Config.CLIENT.skyLightEnabled) ? 1 : 0);
        hash = NeoSkyCelestiaLightLut.mixHash(hash, Config.isFeatureEnabled(Config.CLIENT.skyLightColorsEnabled) ? 1 : 0);
        hash = NeoSkyCelestiaLightLut.mixHash(hash, (Integer)Config.CLIENT.skyLightSunZenithColor.get());
        hash = NeoSkyCelestiaLightLut.mixHash(hash, (Integer)Config.CLIENT.skyLightSunsetColor.get());
        hash = NeoSkyCelestiaLightLut.mixHash(hash, (Integer)Config.CLIENT.skyLightMoonColor.get());
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.skyLightSunStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.skyLightMoonStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.skyLightSunZenithStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.skyLightSunsetStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.skyLightMoonBaseStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.skyLightAmbientStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, Config.isFeatureEnabled(Config.CLIENT.realisticShadowTemperature) ? 1 : 0);
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.shadowTemperatureStrength.get()).floatValue()));
        hash = NeoSkyCelestiaLightLut.mixHash(hash, (Integer)Config.CLIENT.shadowTemperatureBias.get());
        hash = NeoSkyCelestiaLightLut.mixHash(hash, NeoSkyCelestiaLightLut.bits(((Double)Config.CLIENT.vanillaBlockLightInSunShadows.get()).floatValue()));
        return hash;
    }

    private static int mixHash(int hash, int value) {
        return hash * 31 + value;
    }

    private void putEndpoint(int full, int block, int zero, int sky, float normalY, float rawNdotL, NeoSkyCelestiaLighting.State lighting, boolean shadowsEnabled, float sunShadowBlockVisibility, float visibility) {
        float fullR = NeoSkyCelestiaLightLut.red(full);
        float fullG = NeoSkyCelestiaLightLut.green(full);
        float fullB = NeoSkyCelestiaLightLut.blue(full);
        if (!lighting.enabled()) {
            this.put(fullR, fullG, fullB, rawNdotL);
            return;
        }
        float blockR = NeoSkyCelestiaLightLut.red(block);
        float blockG = NeoSkyCelestiaLightLut.green(block);
        float blockB = NeoSkyCelestiaLightLut.blue(block);
        float zeroR = NeoSkyCelestiaLightLut.red(zero);
        float zeroG = NeoSkyCelestiaLightLut.green(zero);
        float zeroB = NeoSkyCelestiaLightLut.blue(zero);
        float skyR = NeoSkyCelestiaLightLut.red(sky);
        float skyG = NeoSkyCelestiaLightLut.green(sky);
        float skyB = NeoSkyCelestiaLightLut.blue(sky);
        float vanillaSkyR = Math.max(skyR - zeroR, 0.0f);
        float vanillaSkyG = Math.max(skyG - zeroG, 0.0f);
        float vanillaSkyB = Math.max(skyB - zeroB, 0.0f);
        float skyAccess = NeoSkyCelestiaLightLut.smoothstep(0.015f, 0.3f, Math.max(vanillaSkyR, Math.max(vanillaSkyG, vanillaSkyB)));
        float hemisphere = NeoSkyCelestiaLightLut.clamp(normalY * 0.5f + 0.5f);
        float ambientR = NeoSkyCelestiaLightLut.lerp(lighting.shadowGroundAmbientColor().x, lighting.shadowSkyAmbientColor().x, hemisphere) * lighting.ambientStrength() * skyAccess;
        float ambientG = NeoSkyCelestiaLightLut.lerp(lighting.shadowGroundAmbientColor().y, lighting.shadowSkyAmbientColor().y, hemisphere) * lighting.ambientStrength() * skyAccess;
        float ambientB = NeoSkyCelestiaLightLut.lerp(lighting.shadowGroundAmbientColor().z, lighting.shadowSkyAmbientColor().z, hemisphere) * lighting.ambientStrength() * skyAccess;
        float directAccess = shadowsEnabled ? NeoSkyCelestiaLightLut.clamp(visibility) * skyAccess : skyAccess;
        float geometricSunFacing = rawNdotL >= 0.005f ? 1.0f : 0.0f;
        float wrappedNdotL = NeoSkyCelestiaLightLut.clamp((rawNdotL + 0.16f) / 1.16f) * geometricSunFacing;
        float directScale = wrappedNdotL * lighting.directStrength() * directAccess;
        float celestialR = ambientR + lighting.directColor().x * directScale;
        float celestialG = ambientG + lighting.directColor().y * directScale;
        float celestialB = ambientB + lighting.directColor().z * directScale;
        float sunShadowAmount = shadowsEnabled ? 1.0f - NeoSkyCelestiaLightLut.clamp(visibility) * geometricSunFacing : 0.0f;
        float blockContributionR = Math.max(blockR - zeroR, 0.0f);
        float blockContributionG = Math.max(blockG - zeroG, 0.0f);
        float blockContributionB = Math.max(blockB - zeroB, 0.0f);
        float skyReferenceR = Math.max(skyR - zeroR, 0.0f);
        float skyReferenceG = Math.max(skyG - zeroG, 0.0f);
        float skyReferenceB = Math.max(skyB - zeroB, 0.0f);
        float blockIrradiance = NeoSkyCelestiaLightLut.luminance(blockContributionR, blockContributionG, blockContributionB);
        float adaptationIrradiance = Math.max(NeoSkyCelestiaLightLut.luminance(skyReferenceR, skyReferenceG, skyReferenceB), NeoSkyCelestiaLightLut.luminance(celestialR, celestialG, celestialB));
        float dominantVisibleBlockIrradiance = Math.max(blockIrradiance - adaptationIrradiance, 0.0f);
        float dominantVisibility = blockIrradiance > 1.0E-5f ? dominantVisibleBlockIrradiance / blockIrradiance : 0.0f;
        float daylightPresence = NeoSkyCelestiaLightLut.smoothstep(0.1f, 0.32f, NeoSkyCelestiaLightLut.luminance(skyReferenceR, skyReferenceG, skyReferenceB));
        float setting = NeoSkyCelestiaLightLut.clamp(sunShadowBlockVisibility);
        float requestedShadowVisibility = setting * daylightPresence * NeoSkyCelestiaLightLut.clamp(sunShadowAmount);
        float sourceCoreCompression = 1.0f / (1.0f + blockIrradiance * (1.0f - setting));
        float shadowVisibility = requestedShadowVisibility * sourceCoreCompression;
        float blockVisibility = Math.max(dominantVisibility, shadowVisibility);
        this.put(zeroR + celestialR + blockContributionR * blockVisibility, zeroG + celestialG + blockContributionG * blockVisibility, zeroB + celestialB + blockContributionB * blockVisibility, rawNdotL);
    }

    private void put(float r, float g, float b, float a) {
        this.pixels.put(r).put(g).put(b).put(a);
    }

    private static float red(int rgba) {
        return (float)(rgba & 0xFF) / 255.0f;
    }

    private static float green(int rgba) {
        return (float)(rgba >>> 8 & 0xFF) / 255.0f;
    }

    private static float blue(int rgba) {
        return (float)(rgba >>> 16 & 0xFF) / 255.0f;
    }

    private static float luminance(float r, float g, float b) {
        return Math.max(r, 0.0f) * 0.2126f + Math.max(g, 0.0f) * 0.7152f + Math.max(b, 0.0f) * 0.0722f;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = NeoSkyCelestiaLightLut.clamp((value - edge0) / Math.max(edge1 - edge0, 1.0E-5f));
        return t * t * (3.0f - 2.0f * t);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float normalX(int code) {
        if (code == 5) {
            return -1.0f;
        }
        if (code == 6) {
            return 1.0f;
        }
        return 0.0f;
    }

    private static float normalY(int code) {
        if (code == 1) {
            return -1.0f;
        }
        if (code == 2 || code == 0) {
            return 1.0f;
        }
        return 0.0f;
    }

    private static float normalZ(int code) {
        if (code == 3) {
            return -1.0f;
        }
        if (code == 4) {
            return 1.0f;
        }
        return 0.0f;
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        for (int i = 0; i < 3; ++i) {
            if (this.textureIds[i] == 0) continue;
            GlStateManager._deleteTexture((int)this.textureIds[i]);
            this.textureIds[i] = 0;
        }
        this.currentTextureIndex = -1;
        this.cacheValid = false;
        this.lastLightmapRevision = Long.MIN_VALUE;
        this.snapshottedLightmapRevision = Long.MIN_VALUE;
        this.lastConfigHash = 0;
    }
}

