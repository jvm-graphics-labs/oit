//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) in vec2 position;

uniform mat4 modelToClip;

void main(void)
{
     gl_Position = modelToClip * vec4(position, 0.0, 1.0);
}