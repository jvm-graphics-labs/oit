//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

#extension GL_ARB_draw_buffers : require

#define MAX_DEPTH 1.0

#include semantic.glsl

uniform sampler2DRect depthBlenderTex;
uniform sampler2DRect frontBlenderTex;

layout (location = DEPTH) out vec2 depth;
layout (location = FRONT_BLENDER) out vec4 frontBlender;
layout (location = BACK_BLENDER) out vec4 backBlender;

vec4 shadeFragment();

void main(void)
{
    // window-space depth interpolated linearly in screen space
    float fragDepth = gl_FragCoord.z;

    vec2 depthBlender = texture(depthBlenderTex, gl_FragCoord.xy).xy;
    vec4 forwardTemp = texture(frontBlenderTex, gl_FragCoord.xy);

    // Depths and 1.0-alphaMult always increase
    // so we can use pass-through by default with MAX blending
    depth = depthBlender;

    // Front colors always increase (DST += SRC*ALPHA_MULT)
    // so we can use pass-through by default with MAX blending
    frontBlender = forwardTemp;

    // Because over blending makes color increase or decrease,
    // we cannot pass-through by default.
    // Each pass, only one fragment writes a color greater than 0
    backBlender = vec4(0.0);

    float nearestDepth = -depthBlender.x;
    float furthestDepth = depthBlender.y;
    float alphaMultiplier = 1.0 - forwardTemp.w;

    if (fragDepth < nearestDepth || fragDepth > furthestDepth) {
        // Skip this depth in the peeling algorithm
        depth = vec2(-MAX_DEPTH);
        return;
    }

    if (fragDepth > nearestDepth && fragDepth < furthestDepth) {
        // This fragment needs to be peeled again
        depth = vec2(-fragDepth, fragDepth);
        return;
    }

    // If we made it here, this fragment is on the peeled layer from last pass
    // therefore, we need to shade it, and make sure it is not peeled any farther
    vec4 color = shadeFragment();
    depth = vec2(-MAX_DEPTH);

    if (fragDepth == nearestDepth) {
        frontBlender.xyz += color.rgb * color.a * alphaMultiplier;
        frontBlender.w = 1.0 - alphaMultiplier * (1.0 - color.a);
    } else {
        backBlender += color;
    }
}