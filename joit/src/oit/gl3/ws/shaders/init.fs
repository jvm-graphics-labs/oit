//--------------------------------------------------------------------------------------
// Order Independent Transparency with Weighted Sums
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#include semantic.glsl

layout (location = FRAG_COLOR) out vec4 outputColor;

vec4 shadeFragment();

void main(void)
{
    vec4 color = shadeFragment();
    outputColor = vec4(color.rgb * color.a, color.a);
}
