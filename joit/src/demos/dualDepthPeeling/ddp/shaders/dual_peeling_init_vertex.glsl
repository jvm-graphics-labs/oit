//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

in vec3 vertex;

//in vec3 position;

void main(void)
{
     gl_Position = ftransform();
     //gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
     //gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    //gl_Position = gl_ModelViewProjectionMatrix * vec4(vertex, 1);
}
