//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#define COLOR_FREQ 30.0
#define ALPHA_FREQ 30.0

smooth in vec3 interpolated;

uniform float alpha;
uniform sampler2DRect opaqueDepthTex;

out vec4 outputColor;

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
    color.a = alpha;

    color.rgb *= diffuse;
    return color;
}
#else
vec4 shadeFragment()
{
    vec4 color;
    color.rgb = vec3(.4,.85,.0);
    color.a = alpha;
    return color;
}
#endif

void main(void)
{
    float opaqueDepth = texture(opaqueDepthTex, gl_FragCoord.xy).r;
    /*if(opaqueDepth == 1)
        outputColor = vec4(0, 1, 0, 1);
    else
        outputColor = vec4(1, 0, 0, 1);*/
    if (gl_FragCoord.z > opaqueDepth) {
        discard;
    }

    vec4 color = shadeFragment();

    outputColor = vec4(color.rgb * color.a, 1.0 - color.a);
}