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

layout (location = FRAG_COLOR) out vec2 outputColor;

uniform sampler2DRect opaqueDepthTex;

void main(void)
{
    //outputColor = vec2(-gl_FragCoord.z, gl_FragCoord.z);

    float opaqueDepth = texture(opaqueDepthTex, gl_FragCoord.xy).r;

    if (gl_FragCoord.z > opaqueDepth) {
        discard;
    }
    outputColor = vec2(-gl_FragCoord.z, min(gl_FragCoord.z, opaqueDepth));
}