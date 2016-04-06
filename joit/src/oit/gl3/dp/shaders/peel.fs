//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#include semantic.glsl

vec4 shadeFragment();

uniform sampler2DRect depthTex;
uniform sampler2DRect opaqueDepthTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

vec4 shadeFragment();

void main(void)
{
    // Bit-exact comparison between FP32 z-buffer and fragment depth
    float frontDepth = texture(depthTex, gl_FragCoord.xy).r;
    float opaqueDepth = texture(opaqueDepthTex, gl_FragCoord.xy).r;
    //if (gl_FragCoord.z <= frontDepth) {
    if (gl_FragCoord.z <= frontDepth || gl_FragCoord.z > opaqueDepth) {
        discard;
    }
    // Shade all the fragments behind the z-buffer
    vec4 color = shadeFragment();
    outputColor = vec4(color.rgb * color.a, color.a);
}