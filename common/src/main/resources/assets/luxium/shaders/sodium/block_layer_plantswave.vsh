#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>
#import <luxium:embeddium/include/plantswave/plants_wave.glsl>

out vec4 v_Color;
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

uvec3 _get_relative_chunk_coord(uint pos) {
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

void main() {
    _vert_init();
    vec2 plantMotion = luxium_plant_vertex_data();
    vec3 position = _vert_position + u_RegionOffset + vec3(_get_relative_chunk_coord(_draw_id)) * 16.0;
    position = luxium_apply_plant_wave(position, plantMotion.x, plantMotion.y);
#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);
    v_Color = _vert_color * texture(u_LightTex, _vert_tex_light_coord);
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    v_MaterialMipBias = _material_mip_bias(_material_params);
#ifdef USE_FRAGMENT_DISCARD
    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
#endif
}