//--------------------------------------------------------------------------------------
// Order Independent Transparency with Average Color
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform sampler2DRect ColorTex0;
uniform sampler2DRect ColorTex1;
uniform vec3 BackgroundColor;

out vec4 outputColor;

void main(void)
{
    vec4 SumColor = texture(ColorTex0, gl_FragCoord.xy);
    float n = texture(ColorTex1, gl_FragCoord.xy).r;

    if (n == 0.0) {
        outputColor.rgb = BackgroundColor;
        return;
    }

    vec3 AvgColor = SumColor.rgb / SumColor.a;
    float AvgAlpha = SumColor.a / n;

    float T = pow(1.0 - AvgAlpha, n);
    outputColor.rgb = AvgColor * (1 - T) + BackgroundColor * T;
    outputColor.a = 1.0;
}
