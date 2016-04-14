//--------------------------------------------------------------------------------------
// Order Independent Transparency Fragment Shader
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

//#version 330

#define COLOR_FREQ 30.0
#define ALPHA_FREQ 30.0

in vec3 interpolated;

uniform Parameters
{
    float alpha;
    float depthScale;
} params;

#if 1
vec4 shadeFragment()
{
    float xWorldPos = interpolated.x;
    float yWorldPos = interpolated.y;
    float diffuse = interpolated.z;

    vec4 color;
    float i = floor(xWorldPos * COLOR_FREQ);
    float j = floor(yWorldPos * ALPHA_FREQ);
    color.rgb = (mod(i, 2.0) == 0) ? vec3(.4,.85,.0) : vec3(1.0);
    //color.a = (mod(j, 2.0) == 0) ? alpha : 0.2;
    color.a = params.alpha;
    //color.a = 0.6f;

    color.rgb *= diffuse;
    return color;
}
#else
vec4 shadeFragment()
{
    vec4 color;
    color.rgb = vec3(.4,.85,.0);
    color.a = params.alpha;
    //color.a = 0.6f;
    return color;
}
#endif