//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

vec3 shadeVertex();

out vec3 shading;

//in vec3 position;

void main(void)
{
    gl_Position = ftransform();
    //gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    //gl_Position = gl_ModelViewProjectionMatrix * vec4(position, 1);
    shading = shadeVertex();
}
