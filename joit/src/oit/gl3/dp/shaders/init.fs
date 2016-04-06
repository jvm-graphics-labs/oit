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

vec4 shadeFragment();

uniform sampler2DRect opaqueDepthTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main(void)
{
    float opaqueDepth = texture(opaqueDepthTex, gl_FragCoord.xy).r;

    if (gl_FragCoord.z > opaqueDepth) {
        discard;
    }

    vec4 color = shadeFragment();

    outputColor = vec4(color.rgb * color.a, 1.0 - color.a);
}