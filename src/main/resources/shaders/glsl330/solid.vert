// $shader_type: vertex

#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

out vec3 normalView;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform mat4 projectionMatrix;

void main() {
    normalView = (normalMatrix * vec4(normal, 0)).xyz;

    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1);
}
