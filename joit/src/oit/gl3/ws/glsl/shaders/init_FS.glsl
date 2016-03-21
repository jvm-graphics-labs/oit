//--------------------------------------------------------------------------------------
// Order Independent Transparency with Weighted Sums
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

out vec4 outputColor;

vec4 ShadeFragment();

void main(void)
{
	vec4 color = ShadeFragment();
	outputColor = vec4(color.rgb * color.a, color.a);
}
