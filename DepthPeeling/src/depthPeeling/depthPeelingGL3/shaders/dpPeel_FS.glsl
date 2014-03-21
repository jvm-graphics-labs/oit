//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform sampler2DRect depthTex;

out vec4 outputColor;

in vec2 oUV;

uniform sampler2D texture0;
uniform int enableTexture;

uniform float alpha;

vec4 ShadeFragment()
{
	vec4 color;
	color.rgb = vec3(.4,.85,.0);
	color.a = alpha;
	return color;
}

void main(void)
{
    // Bit-exact comparison between FP32 z-buffer and fragment depth
    float frontDepth = texture(depthTex, gl_FragCoord.xy).r;
    if (gl_FragCoord.z <= frontDepth) {
            discard;
    }
	
    // Shade all the fragments behind the z-buffer
    vec4 color = ShadeFragment();

    color = (1 - enableTexture) * color + enableTexture * texture(texture0, oUV);
    
    outputColor = vec4(color.rgb * color.a, color.a);
}