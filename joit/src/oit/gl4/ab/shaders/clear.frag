/**
 * Fast Single-pass A-Buffer using OpenGL 4.0
 * Copyright Cyril Crassin, June 2010
**/
#version 450

#include semantic.glsl

//Whole number pixel offsets (not necessary just to test the layout keyword !)
layout(pixel_center_integer) in vec4 gl_FragCoord;

#if ABUFFER_USE_TEXTURES

    layout (binding = ABUFFER_COUNTER, r32ui) uniform coherent uimage2D abufferCounterImg;
    layout (binding = ABUFFER, rgba32f) uniform coherent image2DArray abufferImg;

    void main(void) 
    {
        ivec2 coords = ivec2(gl_FragCoord.xy);

        if(coords.x >= 0 && coords.y >= 0 && coords.x < SCREEN_WIDTH && coords.y < SCREEN_HEIGHT )
        {
            //Reset counter
            imageStore(abufferCounterImg, coords, ivec4(0));

            //Put black in first layer
            imageStore(abufferImg, ivec3(coords, 0), vec4(0.0f));
        }
        //Discard fragment so nothing is writen to the framebuffer
        discard;
    }
#else	//#if ABUFFER_USE_TEXTURES

    uniform vec4 *d_abuffer;
    uniform uint *d_abufferIdx;

    void main(void) 
    {
        ivec2 coords = ivec2(gl_FragCoord.xy);

        //Be sure we are into the framebuffer
        if(coords.x >= 0 && coords.y >= 0 && coords.x < SCREEN_WIDTH && coords.y < SCREEN_HEIGHT )
        {
            d_abufferIdx[coords.x + coords.y * SCREEN_WIDTH] = 0;
            d_abuffer[coords.x + coords.y * SCREEN_WIDTH] = vec4(0.0f);
        }
        //Discard fragment so nothing is writen to the framebuffer
        discard;
    }
#endif	//#if ABUFFER_USE_TEXTURES


