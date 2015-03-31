//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform sampler2DRect tempTex;

out vec4 outputColor;

void main(void)
{
    outputColor = texture(tempTex, gl_FragCoord.xy);
}
