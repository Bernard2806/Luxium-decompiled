#version 330 core
#define LUXIUM_ENTITY_SHADOWS_ENABLED 0
#define LUXIUM_NGV_CUTOUT_ENABLED 0

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>
#import <luxium:embeddium/include/plantswave/plants_wave.glsl>

out vec2 v_TexCoord;
out float v_MaterialMipBias;
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

uvec3 luxium_get_relative_chunk_coord(uint pos) {
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

#if defined(USE_FRAGMENT_DISCARD) && LUXIUM_NGV_CUTOUT_ENABLED == 0

out vec4 v_Color;

void main() {
    _vert_init();
    vec2 plantMotion = luxium_plant_vertex_data();
    vec3 position = _vert_position + u_RegionOffset + vec3(luxium_get_relative_chunk_coord(_draw_id)) * 16.0;
    position = luxium_apply_plant_wave(position, plantMotion.x, plantMotion.y);
#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);
    v_Color = _vert_color * texture(u_LightTex, _vert_tex_light_coord);
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    v_MaterialMipBias = _material_mip_bias(_material_params);
    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
}

#else

out vec4 v_VertexColor;
out vec3 v_FullLight;
out vec3 v_SkyOnlyLight;
out vec3 v_BlockOnlyLight;
out vec3 v_ZeroBlockOnlyLight;
out vec2 v_LuxiumLightCoord;
out vec3 v_LuxiumShadowCoord0;
out vec3 v_LuxiumShadowCoord1;
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
out vec3 v_LuxiumEntityShadowCoord;
uniform mat4 u_LuxiumEntityLightFromView;
#endif
out float v_LuxiumViewDistance;
out vec3 v_LuxiumWorldPosition;
flat out uint v_LuxiumNormalCode;

uniform sampler2D u_LuxiumBlockOnlyLightTex;
uniform mat4 u_LuxiumLightFromView0;
uniform mat4 u_LuxiumLightFromView1;
uniform int u_LuxiumShadowPass;
uniform int u_LuxiumNeoGpuDepthPrepass;
uniform int u_LuxiumSkyShadowEnabled;
uniform int u_LuxiumSkyLightEnabled;

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
    if (u_LuxiumShadowPass != 0 || u_LuxiumNeoGpuDepthPrepass != 0) {
        return;
    }
#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif
    v_VertexColor = _vert_color;
    v_LuxiumLightCoord = _vert_tex_light_coord;
    vec2 skyCoord = vec2(0.5 / 16.0, _vert_tex_light_coord.y);
    v_FullLight = texture(u_LightTex, _vert_tex_light_coord).rgb;
    v_SkyOnlyLight = texture(u_LightTex, skyCoord).rgb;
    bool skyActive = u_LuxiumSkyShadowEnabled != 0 || u_LuxiumSkyLightEnabled != 0;
    if (skyActive) {
        v_BlockOnlyLight = texture(u_LuxiumBlockOnlyLightTex, _vert_tex_light_coord).rgb;
        v_ZeroBlockOnlyLight = texture(u_LuxiumBlockOnlyLightTex, skyCoord).rgb;
        v_LuxiumShadowCoord0 = (u_LuxiumLightFromView0 * viewPosition).xyz;
        v_LuxiumShadowCoord1 = (u_LuxiumLightFromView1 * viewPosition).xyz;
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
        v_LuxiumEntityShadowCoord = (u_LuxiumEntityLightFromView * viewPosition).xyz;
#endif
        v_LuxiumViewDistance = length(viewPosition.xyz);
    } else {
        v_BlockOnlyLight = v_FullLight;
        v_ZeroBlockOnlyLight = v_SkyOnlyLight;
        v_LuxiumShadowCoord0 = vec3(0.0);
        v_LuxiumShadowCoord1 = vec3(0.0);
#if LUXIUM_ENTITY_SHADOWS_ENABLED != 0
        v_LuxiumEntityShadowCoord = vec3(0.0);
#endif
        v_LuxiumViewDistance = 0.0;
    }
    v_LuxiumWorldPosition = position;
    v_LuxiumNormalCode = (_material_params >> 3u) & 7u;
}

#endif