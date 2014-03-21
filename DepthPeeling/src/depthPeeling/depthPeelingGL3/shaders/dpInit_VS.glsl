//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 uv;

out vec2 oUV;

layout(std140) uniform mvpMatrixes  {

    mat4 projectionMatrix;
    mat4 cameraMatrix;
};

void main(void) {

    gl_Position = projectionMatrix * cameraMatrix * vec4(position, 1);

    oUV = uv;
}