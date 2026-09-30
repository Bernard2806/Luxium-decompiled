/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.shaders.Uniform
 *  net.minecraft.client.renderer.ShaderInstance
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL20
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.vinlanx.luxium.mixin.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.client.BlockLightTest.BlockLightTest;
import com.vinlanx.luxium.client.NeoShadowsEngine;
import com.vinlanx.luxium.client.shaders.LuxiumGpuShaderFeatures;
import com.vinlanx.luxium.client.shadows.GpuNeoShadows;
import com.vinlanx.luxium.client.shadows.GpuShadowFrameState;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestiaFrameState;
import com.vinlanx.luxium.client.tfrpluslsr.TemporalFrameSystem;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanillaFrameState;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ShaderInstance.class})
public abstract class ShaderInstanceSkyShadowMixin {
    @Unique
    private static final Set<String> LUXIUM$RECEIVER_SHADERS = Set.of("rendertype_entity_solid", "rendertype_entity_cutout", "rendertype_entity_cutout_no_cull", "rendertype_entity_translucent", "rendertype_entity_translucent_cull", "rendertype_armor_cutout_no_cull");
    @Shadow
    @Final
    private String f_173300_;
    @Unique
    private boolean luxium$receiverShaderResolved;
    @Unique
    private boolean luxium$receiverShader;
    @Unique
    private boolean luxium$uniformsResolved;
    @Unique
    private Uniform luxium$nearMatrix;
    @Unique
    private Uniform luxium$farMatrix;
    @Unique
    private Uniform luxium$entityMatrix;
    @Unique
    private Uniform luxium$cascadeData;
    @Unique
    private Uniform luxium$biasData;
    @Unique
    private Uniform luxium$entityShadowData;
    @Unique
    private Uniform luxium$shadowEnabled;
    @Unique
    private Uniform luxium$entityShadowEnabled;
    @Unique
    private Uniform luxium$entityShadowCasterPass;
    @Unique
    private Uniform luxium$skyLightEnabled;
    @Unique
    private Uniform luxium$filterSamples;
    @Unique
    private Uniform luxium$lightDirection;
    @Unique
    private Uniform luxium$directColor;
    @Unique
    private Uniform luxium$skyAmbientColor;
    @Unique
    private Uniform luxium$groundAmbientColor;
    @Unique
    private Uniform luxium$directStrength;
    @Unique
    private Uniform luxium$ambientStrength;
    @Unique
    private Uniform luxium$vanillaBlockLightInSunShadows;
    @Unique
    private NeoSkyCelestiaFrameState luxium$lastUploadedState;
    @Unique
    private boolean luxium$lastUploadedActive;
    @Unique
    private boolean luxium$lastUploadedCasterPass;
    @Unique
    private float luxium$lastUploadedVanillaBlockLightInSunShadows = Float.NaN;
    @Unique
    private boolean luxium$localLocationsResolved;
    @Unique
    private boolean luxium$neoGpuLocationsResolved;
    @Unique
    private int luxium$localEnabledLoc;
    @Unique
    private int luxium$localReplaceLoc;
    @Unique
    private int luxium$localSoftLoc;
    @Unique
    private int luxium$localModeLoc;
    @Unique
    private int luxium$localSpreadLoc;
    @Unique
    private int luxium$localCountLoc;
    @Unique
    private int luxium$localAtlasLoc;
    @Unique
    private int luxium$localGridLoc;
    @Unique
    private int luxium$localLightsLoc;
    @Unique
    private int luxium$localRectsLoc;
    @Unique
    private int luxium$localBakedHardEnabledLoc;
    @Unique
    private int luxium$localBakedHardMinLoc;
    @Unique
    private int luxium$localBakedHardSizeLoc;
    @Unique
    private int[] luxium$localBakedHardSamplerLocs;
    @Unique
    private int luxium$neoGpuEnabledLoc;
    @Unique
    private int luxium$neoGpuSamplerLoc;
    @Unique
    private int luxium$neoGpuFineScaleLoc;
    @Unique
    private int luxium$neoGpuMinLoc;
    @Unique
    private int luxium$neoGpuSizeLoc;
    @Unique
    private int luxium$neoGpuFastEnabledLoc;
    @Unique
    private int[] luxium$neoGpuFastSamplerLocs;
    @Unique
    private long luxium$lastLocalVersion = Long.MIN_VALUE;

    @Inject(method={"apply"}, at={@At(value="TAIL")}, require=0)
    private void luxium$uploadBlockLightTest(CallbackInfo ci) {
        if (!this.luxium$isReceiverShader()) {
            return;
        }
        if (LuxiumGpuShaderFeatures.neoSkyCelestiaEnabled() || BlockLightTest.enabled()) {
            BlockLightTest.bind(((ShaderInstance)this).m_108943_(), true);
        }
    }

    @Unique
    private boolean luxium$isReceiverShader() {
        if (!this.luxium$receiverShaderResolved) {
            this.luxium$receiverShader = LUXIUM$RECEIVER_SHADERS.contains(this.f_173300_);
            this.luxium$receiverShaderResolved = true;
        }
        return this.luxium$receiverShader;
    }

    @Inject(method={"apply"}, at={@At(value="HEAD")})
    private void luxium$bindSkyShadowState(CallbackInfo ci) {
        float vanillaBlockLightInSunShadows;
        boolean casterPass;
        if (!this.luxium$isReceiverShader() || !LuxiumGpuShaderFeatures.receiverBindingEnabled()) {
            return;
        }
        ShaderInstance shader = (ShaderInstance)this;
        if (!this.luxium$uniformsResolved) {
            this.luxium$resolveUniforms(shader);
        }
        if (TemporalFrameSystem.isFirstPersonOverlayPass()) {
            if (this.luxium$shadowEnabled != null) {
                this.luxium$shadowEnabled.m_142617_(0);
            }
            if (this.luxium$entityShadowEnabled != null) {
                this.luxium$entityShadowEnabled.m_142617_(0);
            }
            if (this.luxium$entityShadowCasterPass != null) {
                this.luxium$entityShadowCasterPass.m_142617_(0);
            }
            if (this.luxium$skyLightEnabled != null) {
                this.luxium$skyLightEnabled.m_142617_(0);
            }
            this.luxium$lastUploadedState = null;
            this.luxium$lastUploadedActive = false;
            this.luxium$lastUploadedCasterPass = false;
            this.luxium$lastUploadedVanillaBlockLightInSunShadows = Float.NaN;
            return;
        }
        NeoSkyCelestia system = NeoSkyCelestia.get();
        NeoSkyCelestiaFrameState state = system.frameState();
        boolean active = system.isMainPassActive();
        boolean skyEntityCasterPass = system.isRenderingEntityShadowPass();
        boolean bl = casterPass = skyEntityCasterPass || NeoShadowsEngine.isGpuShadowAtlasCapturePass();
        if (skyEntityCasterPass) {
            system.bindEntityShadowTarget();
        }
        float f = vanillaBlockLightInSunShadows = state.moonLight() ? 0.0f : ((Double)Config.CLIENT.vanillaBlockLightInSunShadows.get()).floatValue() * 0.01f;
        if (this.luxium$lastUploadedState == state && this.luxium$lastUploadedActive == active && this.luxium$lastUploadedCasterPass == casterPass && Float.compare(this.luxium$lastUploadedVanillaBlockLightInSunShadows, vanillaBlockLightInSunShadows) == 0) {
            return;
        }
        this.luxium$lastUploadedState = state;
        this.luxium$lastUploadedActive = active;
        this.luxium$lastUploadedCasterPass = casterPass;
        this.luxium$lastUploadedVanillaBlockLightInSunShadows = vanillaBlockLightInSunShadows;
        boolean enabled = active && state.enabled();
        boolean skyLightEnabled = active && state.skyLightEnabled();
        shader.m_173350_("LuxiumBlockOnlyLightSampler", (Object)state.blockOnlyLightTexture());
        shader.m_173350_("LuxiumShadowMap0", (Object)state.nearDepthTexture());
        shader.m_173350_("LuxiumShadowMap1", (Object)state.farDepthTexture());
        shader.m_173350_("LuxiumEntityShadowMap", (Object)state.entityDepthTexture());
        if (this.luxium$nearMatrix != null) {
            this.luxium$nearMatrix.m_5679_(state.nearLightFromView());
        }
        if (this.luxium$farMatrix != null) {
            this.luxium$farMatrix.m_5679_(state.farLightFromView());
        }
        if (this.luxium$entityMatrix != null) {
            this.luxium$entityMatrix.m_5679_(state.entityLightFromView());
        }
        if (this.luxium$cascadeData != null) {
            this.luxium$cascadeData.m_5805_(state.nearRadius(), state.farRadius(), state.nearTexelSize(), state.farTexelSize());
        }
        if (this.luxium$biasData != null) {
            this.luxium$biasData.m_5805_(state.baseBiasNear(), state.baseBiasFar(), state.slopeBias(), state.cascadeBlendStart());
        }
        if (this.luxium$entityShadowData != null) {
            this.luxium$entityShadowData.m_5805_(state.entityTexelSize(), state.entityBaseBias(), state.slopeBias(), state.entityRadius());
        }
        if (this.luxium$shadowEnabled != null) {
            this.luxium$shadowEnabled.m_142617_(enabled ? 1 : 0);
        }
        if (this.luxium$entityShadowEnabled != null) {
            this.luxium$entityShadowEnabled.m_142617_(active && state.entityShadowEnabled() ? 1 : 0);
        }
        if (this.luxium$entityShadowCasterPass != null) {
            this.luxium$entityShadowCasterPass.m_142617_(casterPass ? 1 : 0);
        }
        if (this.luxium$skyLightEnabled != null) {
            this.luxium$skyLightEnabled.m_142617_(skyLightEnabled ? 1 : 0);
        }
        if (this.luxium$filterSamples != null) {
            this.luxium$filterSamples.m_142617_(state.filterSamples());
        }
        if (this.luxium$lightDirection != null) {
            this.luxium$lightDirection.m_5889_(state.lightDirection().x, state.lightDirection().y, state.lightDirection().z);
        }
        if (this.luxium$directColor != null) {
            this.luxium$directColor.m_5889_(state.directColor().x, state.directColor().y, state.directColor().z);
        }
        if (this.luxium$skyAmbientColor != null) {
            this.luxium$skyAmbientColor.m_5889_(state.skyAmbientColor().x, state.skyAmbientColor().y, state.skyAmbientColor().z);
        }
        if (this.luxium$groundAmbientColor != null) {
            this.luxium$groundAmbientColor.m_5889_(state.groundAmbientColor().x, state.groundAmbientColor().y, state.groundAmbientColor().z);
        }
        if (this.luxium$directStrength != null) {
            this.luxium$directStrength.m_5985_(state.directStrength() * 0.7f);
        }
        if (this.luxium$ambientStrength != null) {
            this.luxium$ambientStrength.m_5985_(state.ambientStrength());
        }
        if (this.luxium$vanillaBlockLightInSunShadows != null) {
            this.luxium$vanillaBlockLightInSunShadows.m_5985_(vanillaBlockLightInSunShadows);
        }
    }

    @Inject(method={"apply"}, at={@At(value="TAIL")})
    private void luxium$bindLocalShadowState(CallbackInfo ci) {
        boolean bakedHardActive;
        if (!LUXIUM$RECEIVER_SHADERS.contains(this.f_173300_)) {
            return;
        }
        ShaderInstance shader = (ShaderInstance)this;
        int program = shader.m_108943_();
        if (program <= 0 || GL20.glGetProgrami((int)program, (int)35714) == 0) {
            return;
        }
        if (LuxiumGpuShaderFeatures.neoGpuVanillaReceiverEnabled()) {
            if (!this.luxium$neoGpuLocationsResolved) {
                if (this.luxium$neoGpuFastSamplerLocs == null) {
                    this.luxium$neoGpuFastSamplerLocs = new int[6];
                    Arrays.fill(this.luxium$neoGpuFastSamplerLocs, -1);
                }
                this.luxium$neoGpuEnabledLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuVanillaEnabled");
                this.luxium$neoGpuSamplerLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuVanillaVolume");
                this.luxium$neoGpuFastEnabledLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastShadowsEnabled");
                this.luxium$neoGpuFastSamplerLocs[0] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastLightPX");
                this.luxium$neoGpuFastSamplerLocs[1] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastLightNX");
                this.luxium$neoGpuFastSamplerLocs[2] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastLightPY");
                this.luxium$neoGpuFastSamplerLocs[3] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastLightNY");
                this.luxium$neoGpuFastSamplerLocs[4] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastLightPZ");
                this.luxium$neoGpuFastSamplerLocs[5] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuFastLightNZ");
                this.luxium$neoGpuFineScaleLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuVanillaFineScale");
                this.luxium$neoGpuMinLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuVanillaMin");
                this.luxium$neoGpuSizeLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumNeoGpuVanillaSize");
                this.luxium$neoGpuLocationsResolved = true;
            }
            if (TemporalFrameSystem.isFirstPersonOverlayPass()) {
                if (this.luxium$neoGpuEnabledLoc >= 0) {
                    GL20.glUniform1i((int)this.luxium$neoGpuEnabledLoc, (int)0);
                }
                if (this.luxium$neoGpuFastEnabledLoc >= 0) {
                    GL20.glUniform1i((int)this.luxium$neoGpuFastEnabledLoc, (int)0);
                }
                if (this.luxium$neoGpuFineScaleLoc >= 0) {
                    GL20.glUniform1i((int)this.luxium$neoGpuFineScaleLoc, (int)1);
                }
            } else {
                boolean neoGpuFastActive;
                NeoGpuVanillaFrameState neoGpu = NeoGpuVanilla.frameState();
                boolean neoGpuActive = NeoGpuVanilla.isMainPassActive() && neoGpu.enabled() && !NeoShadowsEngine.isAnyShadowCapturePass();
                boolean bl = neoGpuFastActive = neoGpuActive && neoGpu.fastShadowsEnabled();
                if (this.luxium$neoGpuEnabledLoc >= 0) {
                    GL20.glUniform1i((int)this.luxium$neoGpuEnabledLoc, (int)(neoGpuActive ? 1 : 0));
                }
                if (this.luxium$neoGpuFastEnabledLoc >= 0) {
                    GL20.glUniform1i((int)this.luxium$neoGpuFastEnabledLoc, (int)(neoGpuFastActive ? 1 : 0));
                }
                if (this.luxium$neoGpuFineScaleLoc >= 0) {
                    GL20.glUniform1i((int)this.luxium$neoGpuFineScaleLoc, (int)(neoGpuActive ? neoGpu.fineScale() : 1));
                }
                if (neoGpuActive) {
                    ShaderInstanceSkyShadowMixin.luxium$bindTexture(9, neoGpu.volumeTexture());
                    if (this.luxium$neoGpuSamplerLoc >= 0) {
                        GL20.glUniform1i((int)this.luxium$neoGpuSamplerLoc, (int)9);
                    }
                    if (this.luxium$neoGpuMinLoc >= 0) {
                        GL20.glUniform3f((int)this.luxium$neoGpuMinLoc, (float)neoGpu.minX(), (float)neoGpu.minY(), (float)neoGpu.minZ());
                    }
                    if (this.luxium$neoGpuSizeLoc >= 0) {
                        GL20.glUniform3f((int)this.luxium$neoGpuSizeLoc, (float)neoGpu.sizeX(), (float)neoGpu.sizeY(), (float)neoGpu.sizeZ());
                    }
                    if (neoGpuFastActive) {
                        int[] textures = new int[]{neoGpu.fastLightPX(), neoGpu.fastLightNX(), neoGpu.fastLightPY(), neoGpu.fastLightNY(), neoGpu.fastLightPZ(), neoGpu.fastLightNZ()};
                        for (int i = 0; i < 6; ++i) {
                            ShaderInstanceSkyShadowMixin.luxium$bindTexture3D(10 + i, textures[i]);
                            if (this.luxium$neoGpuFastSamplerLocs[i] < 0) continue;
                            GL20.glUniform1i((int)this.luxium$neoGpuFastSamplerLocs[i], (int)(10 + i));
                        }
                    }
                }
            }
            GlStateManager._activeTexture((int)33984);
        }
        if (!LuxiumGpuShaderFeatures.localReceiverEnabled()) {
            return;
        }
        if (!this.luxium$localLocationsResolved) {
            if (this.luxium$localBakedHardSamplerLocs == null) {
                this.luxium$localBakedHardSamplerLocs = new int[6];
                Arrays.fill(this.luxium$localBakedHardSamplerLocs, -1);
            }
            this.luxium$localEnabledLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalShadowEnabled");
            this.luxium$localReplaceLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalReplaceBlockLight");
            this.luxium$localSoftLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalSoftShadows");
            this.luxium$localModeLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalShadowMode");
            this.luxium$localSpreadLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalSpreadOcclusion");
            this.luxium$localCountLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalLightCount");
            this.luxium$localAtlasLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalShadowAtlas");
            this.luxium$localGridLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalLightGrid");
            this.luxium$localLightsLoc = ShaderInstanceSkyShadowMixin.luxium$uniformLocation(program, "LuxiumLocalLights");
            this.luxium$localRectsLoc = ShaderInstanceSkyShadowMixin.luxium$uniformLocation(program, "LuxiumLocalAtlasRect");
            this.luxium$localBakedHardEnabledLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardEnabled");
            this.luxium$localBakedHardSamplerLocs[0] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardPX");
            this.luxium$localBakedHardSamplerLocs[1] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardNX");
            this.luxium$localBakedHardSamplerLocs[2] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardPY");
            this.luxium$localBakedHardSamplerLocs[3] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardNY");
            this.luxium$localBakedHardSamplerLocs[4] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardPZ");
            this.luxium$localBakedHardSamplerLocs[5] = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardNZ");
            this.luxium$localBakedHardMinLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardMin");
            this.luxium$localBakedHardSizeLoc = GL20.glGetUniformLocation((int)program, (CharSequence)"LuxiumLocalBakedHardSize");
            this.luxium$localLocationsResolved = true;
        }
        if (TemporalFrameSystem.isFirstPersonOverlayPass()) {
            if (this.luxium$localEnabledLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localEnabledLoc, (int)0);
            }
            if (this.luxium$localReplaceLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localReplaceLoc, (int)0);
            }
            if (this.luxium$localCountLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localCountLoc, (int)0);
            }
            if (this.luxium$localSoftLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localSoftLoc, (int)0);
            }
            if (this.luxium$localModeLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localModeLoc, (int)0);
            }
            if (this.luxium$localSpreadLoc >= 0) {
                GL20.glUniform1f((int)this.luxium$localSpreadLoc, (float)0.0f);
            }
            if (this.luxium$localBakedHardEnabledLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localBakedHardEnabledLoc, (int)0);
            }
            GlStateManager._activeTexture((int)33984);
            return;
        }
        GpuShadowFrameState state = GpuNeoShadows.frameState();
        boolean active = GpuNeoShadows.isMainPassActive() && state.enabled() && !NeoShadowsEngine.isAnyShadowCapturePass();
        boolean bl = bakedHardActive = active && GpuNeoShadows.isBakedHardReceiverReady();
        if (this.luxium$localEnabledLoc >= 0) {
            GL20.glUniform1i((int)this.luxium$localEnabledLoc, (int)(active ? 1 : 0));
        }
        if (this.luxium$localReplaceLoc >= 0) {
            GL20.glUniform1i((int)this.luxium$localReplaceLoc, (int)(active && state.replaceBlockLight() ? 1 : 0));
        }
        if (this.luxium$localCountLoc >= 0) {
            GL20.glUniform1i((int)this.luxium$localCountLoc, (int)(active && !bakedHardActive ? state.lightCount() : 0));
        }
        if (this.luxium$localSoftLoc >= 0) {
            GL20.glUniform1i((int)this.luxium$localSoftLoc, (int)(!bakedHardActive && state.softShadows() ? 1 : 0));
        }
        if (this.luxium$localModeLoc >= 0) {
            GL20.glUniform1i((int)this.luxium$localModeLoc, (int)state.shadowMode());
        }
        if (this.luxium$localSpreadLoc >= 0) {
            GL20.glUniform1f((int)this.luxium$localSpreadLoc, (float)state.spreadOcclusionStrength());
        }
        if (this.luxium$localBakedHardEnabledLoc >= 0) {
            GL20.glUniform1i((int)this.luxium$localBakedHardEnabledLoc, (int)(bakedHardActive ? 1 : 0));
        }
        if (!active) {
            return;
        }
        if (!bakedHardActive) {
            ShaderInstanceSkyShadowMixin.luxium$bindTexture(7, state.atlasTexture());
            ShaderInstanceSkyShadowMixin.luxium$bindTexture(8, state.lightGridTexture());
            if (this.luxium$localAtlasLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localAtlasLoc, (int)7);
            }
            if (this.luxium$localGridLoc >= 0) {
                GL20.glUniform1i((int)this.luxium$localGridLoc, (int)8);
            }
        }
        if (bakedHardActive) {
            for (int i = 0; i < 6; ++i) {
                int textureUnit = 9 + i;
                ShaderInstanceSkyShadowMixin.luxium$bindTexture3D(textureUnit, GpuNeoShadows.bakedHardTexture(i));
                if (this.luxium$localBakedHardSamplerLocs[i] < 0) continue;
                GL20.glUniform1i((int)this.luxium$localBakedHardSamplerLocs[i], (int)textureUnit);
            }
            if (this.luxium$localBakedHardMinLoc >= 0) {
                GL20.glUniform3f((int)this.luxium$localBakedHardMinLoc, (float)GpuNeoShadows.bakedHardMinX(), (float)GpuNeoShadows.bakedHardMinY(), (float)GpuNeoShadows.bakedHardMinZ());
            }
            if (this.luxium$localBakedHardSizeLoc >= 0) {
                GL20.glUniform3f((int)this.luxium$localBakedHardSizeLoc, (float)GpuNeoShadows.bakedHardSizeX(), (float)GpuNeoShadows.bakedHardSizeY(), (float)GpuNeoShadows.bakedHardSizeZ());
            }
        }
        if (!bakedHardActive && this.luxium$lastLocalVersion != state.version()) {
            if (this.luxium$localLightsLoc >= 0) {
                GL20.glUniform4fv((int)this.luxium$localLightsLoc, (float[])state.lights());
            }
            if (this.luxium$localRectsLoc >= 0) {
                GL20.glUniform4fv((int)this.luxium$localRectsLoc, (float[])state.atlasRects());
            }
            this.luxium$lastLocalVersion = state.version();
        }
        GlStateManager._activeTexture((int)33984);
    }

    @Unique
    private static int luxium$uniformLocation(int program, String name) {
        int location = GL20.glGetUniformLocation((int)program, (CharSequence)(name + "[0]"));
        return location >= 0 ? location : GL20.glGetUniformLocation((int)program, (CharSequence)name);
    }

    @Unique
    private static void luxium$bindTexture(int unit, int textureId) {
        GlStateManager._activeTexture((int)(33984 + unit));
        GlStateManager._bindTexture((int)Math.max(textureId, 0));
    }

    @Unique
    private static void luxium$bindTexture3D(int unit, int textureId) {
        GL13.glActiveTexture((int)(33984 + unit));
        GL11.glBindTexture((int)32879, (int)Math.max(textureId, 0));
        GL13.glActiveTexture((int)33984);
    }

    @Unique
    private void luxium$resolveUniforms(ShaderInstance shader) {
        this.luxium$nearMatrix = shader.m_173348_("LuxiumLightFromView0");
        this.luxium$farMatrix = shader.m_173348_("LuxiumLightFromView1");
        this.luxium$entityMatrix = shader.m_173348_("LuxiumEntityLightFromView");
        this.luxium$cascadeData = shader.m_173348_("LuxiumCascadeData");
        this.luxium$biasData = shader.m_173348_("LuxiumBiasData");
        this.luxium$entityShadowData = shader.m_173348_("LuxiumEntityShadowData");
        this.luxium$shadowEnabled = shader.m_173348_("LuxiumSkyShadowEnabled");
        this.luxium$entityShadowEnabled = shader.m_173348_("LuxiumEntityShadowEnabled");
        this.luxium$entityShadowCasterPass = shader.m_173348_("LuxiumEntityShadowCasterPass");
        this.luxium$skyLightEnabled = shader.m_173348_("LuxiumSkyLightEnabled");
        this.luxium$filterSamples = shader.m_173348_("LuxiumFilterSamples");
        this.luxium$lightDirection = shader.m_173348_("LuxiumLightDirection");
        this.luxium$directColor = shader.m_173348_("LuxiumDirectColor");
        this.luxium$skyAmbientColor = shader.m_173348_("LuxiumSkyAmbientColor");
        this.luxium$groundAmbientColor = shader.m_173348_("LuxiumGroundAmbientColor");
        this.luxium$directStrength = shader.m_173348_("LuxiumDirectStrength");
        this.luxium$ambientStrength = shader.m_173348_("LuxiumAmbientStrength");
        this.luxium$vanillaBlockLightInSunShadows = shader.m_173348_("LuxiumVanillaBlockLightInSunShadows");
        this.luxium$uniformsResolved = true;
    }
}

