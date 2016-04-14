//--------------------------------------------------------------------------------------
// Order Independent Transparency Vertex Shader
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

uniform Transform1
{
    mat4 model;
} t1;

vec3 shadeVertex()
{
    float diffuse = abs(normalize(mat3(t1.model) * normal).z);
    return vec3(position.xy, diffuse);
}
