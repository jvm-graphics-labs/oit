//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 450

// Vertex attributes
#define POSITION    0
#define NORMAL      1

// Uniform
#define TRANSFORM0  0
#define TRANSFORM1  1

// Interfaces
#define BLOCK       0

precision highp float;
precision highp int;
layout(std140, column_major) uniform;
layout(std430, column_major) buffer;

layout (location = POSITION) in vec3 position;
layout (location = NORMAL) in vec3 normal;

layout (binding = TRANSFORM0) uniform Transform0 
{
    mat4 viewProj;
} t0;

layout (binding = TRANSFORM1) uniform Transform1 
{
    mat4 modelToWorld;
} t1;

layout (location = BLOCK) out Block 
{
    vec3 interpolated;
} outBlock;

vec3 shade();

void main(void) 
{
    gl_Position = t0.viewProj * (t1.modelToWorld * vec4(position, 1.0));
    outBlock.interpolated = shade();
}

vec3 shade()
{
    float diffuse = abs(normalize(mat3(t1.modelToWorld) * normal).z);
    return vec3(position.xy, diffuse);
}