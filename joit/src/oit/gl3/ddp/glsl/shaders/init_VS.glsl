//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) in vec3 position;

uniform mat4 modelToWorld;

layout(std140) uniform vpMatrixes  {

    mat4 worldToCamera;
    mat4 cameraToClip;
};

void main(void) 
{
    gl_Position = cameraToClip * worldToCamera * modelToWorld * vec4(position, 1.0);
}