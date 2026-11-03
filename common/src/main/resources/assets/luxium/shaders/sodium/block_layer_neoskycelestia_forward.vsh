#version 330 core
#define LUXIUM_ENTITY_SHADOWS_ENABLED 0

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>
#import <luxium:embeddium/include/plantswave/plants_wave.glsl>
#ifdef LUXIUM_BLOCKLIGHT_TEST
#import <luxium:embeddium/include/blocklighttest.glsl>
#endif

out vec4 v_LuxiumShadowColorAlpha;
out vec3 v_LuxiumLightDelta;
out vec2 v_TexCoord;
out float v_MaterialMipBias;
out vec3 v_LuxiumShadowCoord0;
out vec3 v_LuxiumShadowCoord1;
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
out vec3 v_LuxiumEntityShadowCoord;
uniform mat4 u_LuxiumEntityLightFromView;
#endif
out float v_LuxiumViewDistance;
flat out float v_LuxiumRawNdotL;
flat out uint v_LuxiumNormalCode;
#ifdef USE_FRAGMENT_DISCARD
out float v_MaterialAlphaCutoff;
#endif
#ifdef USE_FOG
out float v_FragDistance;
#endif

uniform int u_FogShape;
uniform vec3 u_RegionOffset;
uniform vec2 u_TexCoordShrink;
uniform sampler2D u_LightTex;
uniform sampler2D u_LuxiumCelestialLut;
uniform mat4 u_LuxiumLightFromView0;
uniform mat4 u_LuxiumLightFromView1;
uniform int u_LuxiumShadowPass;
uniform int u_LuxiumSkyShadowEnabled;
uniform int u_LuxiumSkyLightEnabled;

vec4 luxium_sample_celestial_lut(vec2 lightCoord, uint normalCode, float litSide) {
    vec2 lightUv = clamp(lightCoord, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
    float code = float(min(normalCode, 6u));
    vec2 uv = vec2(lightUv.x * 0.5 + litSide * 0.5, (code + lightUv.y) / 7.0);
    return texture(u_LuxiumCelestialLut, uv);
}

uvec3 luxium_get_relative_chunk_coord(uint pos) {
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

void main() {
    _vert_init();
    vec2 plantMotion = luxium_plant_vertex_data();
    vec3 position = _vert_position + u_RegionOffset + vec3(luxium_get_relative_chunk_coord(_draw_id)) * 16.0;
    position = luxium_apply_plant_wave(position, plantMotion.x, plantMotion.y);
    vec4 viewPosition = u_ModelViewMatrix * vec4(position, 1.0);
    gl_Position = u_ProjectionMatrix * viewPosition;
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    v_MaterialMipBias = _material_mip_bias(_material_params);
#ifdef USE_FRAGMENT_DISCARD
    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
#endif
    if (u_LuxiumShadowPass != 0) {
        return;
    }
#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif

    uint normalCode = (_material_params >> 3u) & 7u;
    vec2 skyCoord = _vert_tex_light_coord;
#ifdef LUXIUM_BLOCKLIGHT_TEST
    skyCoord.x = 0.5 / 16.0;
#endif
    bool skyActive = u_LuxiumSkyShadowEnabled != 0 || u_LuxiumSkyLightEnabled != 0;
    vec3 shadowLight;
    vec3 litLight;
    float rawNdotL;
    if (skyActive) {
        vec4 shadowSample = luxium_sample_celestial_lut(skyCoord, normalCode, 0.0);
        vec4 litSample = luxium_sample_celestial_lut(skyCoord, normalCode, 1.0);
        shadowLight = shadowSample.rgb;
        litLight = litSample.rgb;
        rawNdotL = litSample.a;
    } else {
        shadowLight = texture(u_LightTex, skyCoord).rgb;
        litLight = shadowLight;
        rawNdotL = -1.0;
    }
#ifdef LUXIUM_BLOCKLIGHT_TEST
    float blockLevel = clamp(_vert_tex_light_coord.x * (256.0 / 240.0), 0.0, 1.0);
    vec3 localGlow = bt_illumination(blockLevel, position, bt_face_normal(normalCode));
    shadowLight += localGlow;
    litLight += localGlow;
#endif

    vec3 vertexFactor = _vert_color.rgb * _vert_color.a;
    v_LuxiumShadowColorAlpha = vec4(vertexFactor * shadowLight, 1.0);
    v_LuxiumLightDelta = vertexFactor * (litLight - shadowLight);
    v_LuxiumNormalCode = normalCode;
    v_LuxiumRawNdotL = rawNdotL;
    if (skyActive) {
        v_LuxiumShadowCoord0 = (u_LuxiumLightFromView0 * viewPosition).xyz;
        v_LuxiumShadowCoord1 = (u_LuxiumLightFromView1 * viewPosition).xyz;
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
        v_LuxiumEntityShadowCoord = (u_LuxiumEntityLightFromView * viewPosition).xyz;
#endif
        v_LuxiumViewDistance = length(viewPosition.xyz);
    } else {
        v_LuxiumShadowCoord0 = vec3(0.0);
        v_LuxiumShadowCoord1 = vec3(0.0);
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
        v_LuxiumEntityShadowCoord = vec3(0.0);
#endif
        v_LuxiumViewDistance = 0.0;
    }
}