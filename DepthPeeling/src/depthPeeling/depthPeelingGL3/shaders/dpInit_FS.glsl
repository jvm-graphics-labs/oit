//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

out vec4 outputColor;

in vec2 oUV;

uniform sampler2D texture0;
uniform int enableTexture;

uniform float alpha;

vec4 ShadeFragment() {
    vec4 color;
    color.rgb = vec3(.4,.85,.0);
    color.a = alpha;
    return color;
}

void main(void)
{
    vec4 color = ShadeFragment();

    color = (1 - enableTexture) * color + enableTexture * texture(texture0, oUV);
    //color = (1 - enableTexture) * color + enableTexture * vec4(1);

    outputColor = vec4(color.rgb * color.a, 1.0 - color.a);
    //outputColor = vec4(1, 0, 0, 1);
}