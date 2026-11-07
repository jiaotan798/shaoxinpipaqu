#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    // 连续保留小alpha，避免图片在淡出末尾突然切断。
    fragColor = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    if (fragColor.a < 0.001) discard;
}
