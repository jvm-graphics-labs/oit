//--------------------------------------------------------------------------------------
// Order Independent Transparency with Weighted Sums
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform sampler2DRect ColorTex;
uniform vec3 BackgroundColor;

out vec4 outputColor;

// Sum(A_i * C_i) + C_bg * (1 - Sum(A_i))
void main(void)
{
    vec4 S = texture(ColorTex, gl_FragCoord.xy);
    outputColor.rgb = S.rgb + BackgroundColor * (1.0 - S.a);
    outputColor.a = 1.0;
}
