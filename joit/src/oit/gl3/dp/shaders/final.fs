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

uniform sampler2DRect colorTex;
uniform sampler2DRect opaqueColorTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main(void)
{
    vec4 frontColor = texture(colorTex, gl_FragCoord.xy);
    vec3 opaqueColor = texture(opaqueColorTex, gl_FragCoord.xy).rgb;
    outputColor = vec4(frontColor.rgb + opaqueColor * frontColor.a, 1);
}
