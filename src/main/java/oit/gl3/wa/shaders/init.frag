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

uniform sampler2DRect opaqueDepthTex;

layout (location = SUM_COLORS) out vec4 sumColors;
layout (location = COUNT) out float count;

vec4 shadeFragment();

void main(void)
{
    float opaqueDepth = texture(opaqueDepthTex, gl_FragCoord.xy).r;

    if (gl_FragCoord.z > opaqueDepth) {
        discard;
    }

    vec4 color = shadeFragment();
    sumColors = vec4(color.rgb * color.a, color.a);
    count = 1;
}
