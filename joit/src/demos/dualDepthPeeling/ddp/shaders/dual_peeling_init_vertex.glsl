//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

in vec3 vertex;

precision highp float;
precision highp int;

void main(void)
{
     //gl_Position = ftransform();
     //gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
     //gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
     gl_Position = gl_ModelViewProjectionMatrix * vec4(vertex, 1);
}
