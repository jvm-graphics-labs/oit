//--------------------------------------------------------------------------------------
// Order Independent Transparency with Average Color
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#include semantic.glsl

layout (location = POSITION) in vec3 position;

out vec3 interpolated;

uniform Transform0
{
    mat4 projView;
} t0;

uniform Transform1
{
    mat4 model;
} t1;

vec3 shadeVertex();

void main(void) {

    gl_Position = t0.projView * t1.model * vec4(position, 1.0);
    interpolated = shadeVertex();
}
