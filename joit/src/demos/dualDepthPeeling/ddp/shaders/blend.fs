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

uniform sampler2DRect tempTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main(void)
{
    outputColor = texture(tempTex, gl_FragCoord.xy);
    // for occlusion query
    if (outputColor.a == 0) 
        discard;
}
