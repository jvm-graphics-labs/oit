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

#version 330

#include semantic.glsl

layout (location = FRAG_COLOR) out vec4 outColor;

uniform sampler2DRect weightedSum;
uniform sampler2DRect transmProduct;
uniform sampler2DRect opaqueColorTex;

void main(void)
{
    vec3 backgroundColor = texture(opaqueColorTex, gl_FragCoord.xy).rgb;

    vec4 sumColor = vec4(texture(weightedSum, gl_FragCoord.xy).rgb, texture(transmProduct, gl_FragCoord.xy).r);
    float transmittance = texture(weightedSum, gl_FragCoord.xy).a;
    vec3 averageColor = sumColor.rgb / max(sumColor.a, 0.00001);

    outColor = vec4(averageColor * (1 - transmittance) + backgroundColor * transmittance, 1.0);
}
