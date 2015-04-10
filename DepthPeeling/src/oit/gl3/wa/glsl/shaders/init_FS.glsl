//--------------------------------------------------------------------------------------
// Order Independent Transparency with Average Color
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330
#extension ARB_draw_buffers : require

layout (location = 0) out vec4 accumulationTexId0;
layout (location = 1) out vec4 accumulationTexId1;

vec4 ShadeFragment();

void main(void)
{
    vec4 color = ShadeFragment();
    accumulationTexId0 = vec4(color.rgb * color.a, color.a);
    accumulationTexId1 = vec4(1.0);
}
