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

layout (location = POSITION) in vec2 position;

uniform Transform2
{
    mat4 modelToClip;
} t2;

void main(void)
{
     gl_Position = t2.modelToClip * vec4(position, 0.0, 1.0);
}