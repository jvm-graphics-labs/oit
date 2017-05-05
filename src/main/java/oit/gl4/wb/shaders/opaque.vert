//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

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
    vec3 interpolated;
} outBlock;

vec3 shade();

void main(void) 
{
    gl_Position = t0.proj * (t0.view * (t1.model * vec4(position, 1.0)));
    outBlock.interpolated = shade();
}

vec3 shade()
{
    float diffuse = abs(normalize(mat3(t1.model) * normal).z);
    return vec3(position.xy, diffuse);
}