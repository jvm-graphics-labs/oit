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

uniform sampler2DRect sumColorTex;
uniform sampler2DRect countTex;
uniform sampler2DRect opaqueColorTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main(void)
{
    vec3 opaqueColor = texture(opaqueColorTex, gl_FragCoord.xy).rgb;
    vec4 sumColor = texture(sumColorTex, gl_FragCoord.xy);
    float n = texture(countTex, gl_FragCoord.xy).r;

    if (n == 0.0) {
        outputColor.rgb = opaqueColor;
        return;
    }

    vec3 avgColor = sumColor.rgb / sumColor.a;
    float avgAlpha = sumColor.a / n;

    float t = pow(1.0 - avgAlpha, n);
    outputColor = vec4(avgColor * (1 - t) + opaqueColor * t, 1.0);
}
