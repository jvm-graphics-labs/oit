//----------------------------------------------------------------------------------
// File:        gl4-kepler\WeightedBlendedOIT\assets\shaders/weighted_blend_fragment.glsl
// SDK Version: v2.11 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2015, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------

#version 400
#extension ARB_draw_buffers : require

#define COLOR_FREQ 30.0
#define ALPHA_FREQ 30.0

smooth in vec3 interpolated;

uniform float alpha;

uniform sampler2DRect opaqueDepthTex;

uniform float depthScale;

layout (location = 0) out vec4 sumColor;
layout (location = 1) out vec4 sumWeight;

vec4 shade();

void main(void)
{
    float opaqueDepth = texture(opaqueDepthTex, gl_FragCoord.xy).r;

    if (gl_FragCoord.z > opaqueDepth) {
        discard;
    }

    vec4 color = shade();

    // Assuming that the projection matrix is a perspective projection
    // gl_FragCoord.w returns the inverse of the oPos.w register from the vertex shader
    float viewDepth = abs(1.0 / gl_FragCoord.w);

    // Tuned to work well with FP16 accumulation buffers and 0.001 < linearDepth < 2.5
    // See Equation (9) from http://jcgt.org/published/0002/02/09/
    float linearDepth = viewDepth * depthScale;
    float weight = clamp(0.03 / (1e-5 + pow(linearDepth, 4.0)), 1e-2, 3e3);

    sumColor = vec4(color.rgb * color.a, color.a) * weight;
    sumWeight = vec4(color.a);
}

#if 1
vec4 shade()
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
vec4 shade()
{
    vec4 color;
    color.rgb = vec3(.4,.85,.0);
    color.a = alpha;
    return color;
}
#endif