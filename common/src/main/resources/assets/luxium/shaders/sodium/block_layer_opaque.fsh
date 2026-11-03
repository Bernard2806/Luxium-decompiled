#version 330 core
#define LUXIUM_ENTITY_SHADOWS_ENABLED 0
#ifdef GL_ARB_texture_gather
#extension GL_ARB_texture_gather : enable
#define LUXIUM_TEXTURE_GATHER
#endif
#import <sodium:include/fog.glsl>
#import <luxium:embeddium/include/sky_shadow.glsl>
#import <luxium:embeddium/include/neogpuvanilla.glsl>

in vec4 v_VertexColor;
in vec3 v_FullLight;
in vec3 v_SkyOnlyLight;
in vec3 v_BlockOnlyLight;
in vec3 v_ZeroBlockOnlyLight;
in vec2 v_TexCoord;
in vec2 v_LuxiumLightCoord;
in float v_MaterialMipBias;
in vec3 v_LuxiumShadowCoord0;
in vec3 v_LuxiumShadowCoord1;
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
in vec3 v_LuxiumEntityShadowCoord;
uniform sampler2D u_LuxiumEntityShadowMap;
uniform vec4 u_LuxiumEntityShadowData;
uniform int u_LuxiumEntityShadowEnabled;
#endif
in float v_LuxiumViewDistance;
in vec3 v_LuxiumWorldPosition;
flat in uint v_LuxiumNormalCode;
#ifdef USE_FRAGMENT_DISCARD
in float v_MaterialAlphaCutoff;
#endif
#ifdef USE_FOG
in float v_FragDistance;
#endif

uniform sampler2D u_BlockTex;
uniform sampler2D u_LightTex;
uniform sampler2D u_LuxiumBlockOnlyLightTex;
uniform sampler2DShadow u_LuxiumShadowMap0;
uniform sampler2DShadow u_LuxiumShadowMap1;
uniform sampler2D u_LuxiumLocalShadowAtlas;
uniform sampler2D u_LuxiumLocalLightGrid;
uniform vec4 u_LuxiumLocalLights[32];
uniform vec4 u_LuxiumLocalAtlasRect[192];
uniform int u_LuxiumLocalShadowEnabled;
uniform int u_LuxiumLocalReplaceBlockLight;
uniform int u_LuxiumLocalSoftShadows;
uniform int u_LuxiumLocalShadowMode;
uniform float u_LuxiumLocalSpreadOcclusion;
uniform int u_LuxiumLocalLightCount;
uniform sampler3D u_LuxiumLocalBakedHardPX;
uniform sampler3D u_LuxiumLocalBakedHardNX;
uniform sampler3D u_LuxiumLocalBakedHardPY;
uniform sampler3D u_LuxiumLocalBakedHardNY;
uniform sampler3D u_LuxiumLocalBakedHardPZ;
uniform sampler3D u_LuxiumLocalBakedHardNZ;
uniform int u_LuxiumLocalBakedHardEnabled;
uniform vec3 u_LuxiumLocalBakedHardMin;
uniform vec3 u_LuxiumLocalBakedHardSize;
uniform vec4 u_LuxiumCascadeData;
uniform vec4 u_LuxiumBiasData;
uniform int u_LuxiumSkyShadowEnabled;
uniform int u_LuxiumSkyLightEnabled;
uniform int u_LuxiumFilterSamples;
uniform int u_LuxiumShadowPass;
uniform vec3 u_LuxiumLightDirection;
uniform vec3 u_LuxiumDirectColor;
uniform vec3 u_LuxiumSkyAmbientColor;
uniform vec3 u_LuxiumGroundAmbientColor;
uniform float u_LuxiumDirectStrength;
uniform float u_LuxiumAmbientStrength;
uniform float u_LuxiumVanillaBlockLightInSunShadows;
uniform sampler2D u_LuxiumNeoGpuVanillaVolume;
uniform int u_LuxiumNeoGpuVanillaEnabled;
uniform int u_LuxiumNeoGpuVanillaFineScale;
uniform vec3 u_LuxiumNeoGpuVanillaMin;
uniform vec3 u_LuxiumNeoGpuVanillaSize;
uniform vec4 u_FogColor;
uniform float u_FogStart;
uniform float u_FogEnd;
#import <luxium:embeddium/include/local_shadow.glsl>
out vec4 fragColor;

void main() {
    if (u_LuxiumShadowPass != 0) {
#ifdef USE_FRAGMENT_DISCARD
        if (texture(u_BlockTex, v_TexCoord, v_MaterialMipBias).a < v_MaterialAlphaCutoff) discard;
#endif
        return;
    }
    vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, v_MaterialMipBias);
#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < v_MaterialAlphaCutoff) discard;
#endif
    vec3 normal = luxium_resolve_terrain_normal(v_LuxiumNormalCode, v_LuxiumWorldPosition);
    bool skyActive = u_LuxiumSkyShadowEnabled != 0 || u_LuxiumSkyLightEnabled != 0;
    vec3 clippedFullLight = v_FullLight;
    vec3 clippedBlockOnlyLight = v_BlockOnlyLight;
    float originalBlockLevel = clamp(v_LuxiumLightCoord.x * 16.0, 0.0, 15.0);
    float clippedBlockLevel = luxium_ngv_clipped_level(
            u_LuxiumNeoGpuVanillaVolume, u_LuxiumNeoGpuVanillaEnabled,
            u_LuxiumNeoGpuVanillaMin, u_LuxiumNeoGpuVanillaSize,
            v_LuxiumWorldPosition, normal, u_LuxiumNeoGpuVanillaFineScale,
            originalBlockLevel);
    if (clippedBlockLevel + 0.001 < originalBlockLevel) {
            vec2 clippedLightCoord = vec2(
                clamp(clippedBlockLevel / 16.0, 0.5 / 16.0, 15.5 / 16.0),
                v_LuxiumLightCoord.y);
            clippedFullLight = texture(u_LightTex, clippedLightCoord).rgb;
        clippedBlockOnlyLight = skyActive
                ? texture(u_LuxiumBlockOnlyLightTex, clippedLightCoord).rgb
                : clippedFullLight;
    }
    vec3 fullLight = clippedFullLight;
    vec3 noBlockLight = v_SkyOnlyLight;
    if (skyActive) {
        float rawNdotL = dot(normal, u_LuxiumLightDirection);
        float nDotL = max(rawNdotL, 0.0);
        float visibility = 1.0;
        if (u_LuxiumSkyShadowEnabled != 0 && rawNdotL > -0.16) {
            visibility = luxium_resolve_sky_visibility(
                    u_LuxiumShadowMap0, u_LuxiumShadowMap1,
                    v_LuxiumShadowCoord0, v_LuxiumShadowCoord1, v_LuxiumViewDistance,
                    u_LuxiumCascadeData, u_LuxiumBiasData, u_LuxiumFilterSamples, nDotL);
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
            if (u_LuxiumEntityShadowEnabled != 0) {
                visibility = min(visibility, luxium_resolve_entity_visibility(
                        u_LuxiumEntityShadowMap, v_LuxiumEntityShadowCoord,
                        u_LuxiumEntityShadowData, u_LuxiumFilterSamples, nDotL));
            }
#endif
        }
        if (u_LuxiumSkyLightEnabled != 0) {
            LuxiumCelestialTerms celestial = luxium_prepare_celestial_lighting(
                    v_ZeroBlockOnlyLight, v_SkyOnlyLight, normal, rawNdotL,
                    u_LuxiumDirectColor, u_LuxiumSkyAmbientColor, u_LuxiumGroundAmbientColor,
                    u_LuxiumDirectStrength, u_LuxiumAmbientStrength, visibility,
                    u_LuxiumSkyShadowEnabled != 0);
            fullLight = luxium_apply_prepared_celestial_lighting(
                    clippedBlockOnlyLight, v_ZeroBlockOnlyLight, v_SkyOnlyLight, celestial,
                    u_LuxiumVanillaBlockLightInSunShadows);
            noBlockLight = v_ZeroBlockOnlyLight + celestial.light;
        }
    }
    vec3 blockGuideRgb = max(clippedBlockOnlyLight - v_ZeroBlockOnlyLight, vec3(0.0));
    float blockLightGuide = max(blockGuideRgb.r, max(blockGuideRgb.g, blockGuideRgb.b));
    vec4 localLighting = luxium_resolve_local_lighting(v_LuxiumWorldPosition, normal, blockLightGuide);
    float replacement = u_LuxiumLocalReplaceBlockLight != 0 ? localLighting.a : 0.0;
    vec3 light = min(mix(fullLight, noBlockLight, replacement) + localLighting.rgb, vec3(1.6));
    diffuseColor.rgb *= v_VertexColor.rgb * v_VertexColor.a * light;
#ifdef USE_FOG
    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
#else
    fragColor = diffuseColor;
#endif
}