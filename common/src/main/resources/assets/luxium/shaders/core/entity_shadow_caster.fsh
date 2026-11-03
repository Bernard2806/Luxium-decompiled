#version 150

uniform sampler2D Sampler0;
uniform float LuxiumAlphaCutoff;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    if (LuxiumAlphaCutoff >= 0.0 && texture(Sampler0, texCoord0).a < LuxiumAlphaCutoff) discard;
    fragColor = vec4(1.0);
}