//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#include semantic.glsl

layout (location = POSITION) in vec3 position;
layout (location = NORMAL) in vec3 normal;

out vec3 interpolated;

uniform Transform0
{
    mat4 view;
    mat4 proj;
} t0;

uniform Transform1
{
    mat4 model;
} t1;

vec3 shadeVertex();

void main(void) 
{
    gl_Position = t0.proj * t0.view * t1.model * vec4(position, 1.0);
    interpolated = shadeVertex();
}