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

uniform sampler2DRect colorTex;
uniform sampler2DRect opaqueColorTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

// Sum(A_i * C_i) + C_bg * (1 - Sum(A_i))
void main(void)
{
    vec3 opaqueColor = texture(opaqueColorTex, gl_FragCoord.xy).rgb;;
    vec4 s = texture(colorTex, gl_FragCoord.xy);
    outputColor = vec4(s.rgb + opaqueColor * (1.0 - s.a), 1.0);
}
