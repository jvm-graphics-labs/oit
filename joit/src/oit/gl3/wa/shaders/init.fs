//--------------------------------------------------------------------------------------
// Order Independent Transparency with Average Color
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#extension GL_ARB_draw_buffers : require

#include semantic.glsl

layout (location = SUM_COLORS) out vec4 accumulationTex0;
layout (location = COUNT) out float accumulationTex1;

vec4 shadeFragment();

void main(void)
{
    vec4 color = shadeFragment();
    accumulationTex0 = vec4(color.rgb * color.a, color.a);
    accumulationTex1 = 1;
}
