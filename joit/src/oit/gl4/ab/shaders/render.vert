/**
 * Fast Single-pass A-Buffer using OpenGL 4.0
 * Copyright Cyril Crassin, June 2010
**/

#version 450

#include semantic.glsl

layout (location = POSITION) in vec3 position;
layout (location = NORMAL) in vec3 normal;

layout (binding = TRANSFORM0) uniform Transform0
{
    mat4 view;
    mat4 proj;
} t0;

layout (binding = TRANSFORM1) uniform Transform1
{
    mat4 model;
} t1;

layout (location = BLOCK) out Block
{
    out vec4 pos;
    out vec3 texCoord;
    out vec3 normal;
} frag;

void main()
{
    vec4 pos = t0.proj * (t0.view * (t1.model * vec4(position, 1.0f)));

    vec3 normalEye = normalize((t0.view * (t1.model * vec4(normal, 1.0f))).xyz);

    frag.pos = pos;
    frag.texCoord = vec3(position.xy, abs(normalEye.z));
    frag.normal = normalEye;

    gl_Position = pos;
}