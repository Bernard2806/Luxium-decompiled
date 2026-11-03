#version 330 core
#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>
#import <luxium:embeddium/include/plantswave/plants_wave.glsl>

out vec4 btColor;
out vec2 btUv;
out vec3 btSky;
out float btLevel;
out vec3 btPosition;
flat out uint btFace;
out float btBias;
#ifdef USE_FRAGMENT_DISCARD
out float btCutoff;
#endif
#ifdef USE_FOG
out float btFog;
#endif

uniform int u_FogShape;
uniform vec3 u_RegionOffset;
uniform vec2 u_TexCoordShrink;
uniform sampler2D u_LightTex;

void main() {
    _vert_init();
    vec2 plantMotion = luxium_plant_vertex_data();
    uvec3 chunk = uvec3(_draw_id) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
    vec3 position = _vert_position + u_RegionOffset + vec3(chunk) * 16.0;
    position = luxium_apply_plant_wave(position, plantMotion.x, plantMotion.y);
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);
#ifdef USE_FOG
    btFog = getFragDistance(u_FogShape, position);
#endif
    ivec2 lightTexel = ivec2(clamp(floor(_vert_tex_light_coord * 16.0), vec2(0.0), vec2(15.0)));
    btColor = _vert_color;
    btUv = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    btSky = texelFetch(u_LightTex, ivec2(0, lightTexel.y), 0).rgb;
    btLevel = clamp(_vert_tex_light_coord.x * (256.0 / 240.0), 0.0, 1.0);
    btPosition = position;
    btFace = (_material_params >> 3u) & 7u;
    btBias = _material_mip_bias(_material_params);
#ifdef USE_FRAGMENT_DISCARD
    btCutoff = _material_alpha_cutoff(_material_params);
#endif
}