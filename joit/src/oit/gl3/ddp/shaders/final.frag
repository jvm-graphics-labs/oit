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

//uniform sampler2DRect depthBlenderTex;
uniform sampler2DRect frontBlenderTex;
uniform sampler2DRect backBlenderTex;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main(void)
{
    vec4 frontColor = texture(frontBlenderTex, gl_FragCoord.xy);
    vec3 backColor = texture(backBlenderTex, gl_FragCoord.xy).rgb;
    outputColor = vec4(frontColor.rgb + vec3(1) * frontColor.a, 1);

    float alphaMultiplier = 1.0 - frontColor.w;

    // front + back
    outputColor = vec4(frontColor.rgb + backColor * alphaMultiplier, 1);

    // front blender
    //gl_FragColor.rgb = frontColor + vec3(alphaMultiplier);

    // back blender
    //gl_FragColor.rgb = backColor;
}
