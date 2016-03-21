//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) out vec4 fragColor;

void main(void)
{
    fragColor = vec4(-gl_FragCoord.z, gl_FragCoord.z, 0, 1);
}
