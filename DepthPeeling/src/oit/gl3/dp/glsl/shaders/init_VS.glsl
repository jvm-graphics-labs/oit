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
layout (location = 1) in vec3 normal;

smooth out vec3 interpolated;

uniform mat4 modelToWorld;

layout(std140) uniform vpMatrixes  {

    mat4 worldToCamera;
    mat4 cameraToClip;
};

vec3 shadeVertex();

/*vec3 shadeVertex() {

    float diffuse = abs(normalize(mat3(modelToWorld) * normal).z);
    return vec3(position.xy, diffuse);
}*/

void main(void) {

    gl_Position = cameraToClip * worldToCamera * modelToWorld * vec4(position, 1.0);
    interpolated = shadeVertex();
}