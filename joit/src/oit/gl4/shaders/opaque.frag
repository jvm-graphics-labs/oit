//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 450

#define COLOR_FREQ 30.0
#define ALPHA_FREQ 30.0

#include semantic.glsl

layout (location = FRAG_COLOR) out vec4 outputColor;

layout (location = BLOCK) in Block 
{
    vec3 interpolated;
} inBlock;

layout (binding = PARAMETERS) uniform Parameters
{
    float alpha;
    float depthScale;
} params;

vec4 shade();

void main(void)
{
    outputColor = shade();
}

#if 1
vec4 shade()
{
    float xWorldPos = inBlock.interpolated.x;
    float yWorldPos = inBlock.interpolated.y;
    float diffuse = inBlock.interpolated.z;

    vec4 color;
    float i = floor(xWorldPos * COLOR_FREQ);
    float j = floor(yWorldPos * ALPHA_FREQ);
    color.rgb = (mod(i, 2.0) == 0) ? vec3(.4,.85,.0) : vec3(1.0);
    //color.a = (mod(j, 2.0) == 0) ? alpha : 0.2;
    color.a = params.alpha;

    color.rgb *= diffuse;
    return color;
}
#else
vec4 shade()
{
    vec4 color;
    color.rgb = vec3(.4,.85,.0);
    color.a = params.alpha;
    return color;
}
#endif