//--------------------------------------------------------------------------------------
// Order Independent Transparency Vertex Shader
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 modelToWorld;

vec3 ShadeVertex()
{
	float diffuse = abs(normalize(mat3(modelToWorld) * normal).z);
	return vec3(position.xy, diffuse);
}
