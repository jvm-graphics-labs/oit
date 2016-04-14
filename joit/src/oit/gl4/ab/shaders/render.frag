/**
 * Fast Single-pass A-Buffer using OpenGL 4.0
 * Copyright Cyril Crassin, June 2010
**/

#version 450

#include semantic.glsl

//Whole number pixel offsets (not necessary just to test the layout keyword !)
layout (pixel_center_integer) in vec4 gl_FragCoord;

#if ABUFFER_USE_TEXTURES
//A-Buffer fragments storage
coherent uniform layout (location = ABUFFER, size4x32) image2DArray abufferImg;
//A-Buffer fragment counter
coherent uniform layout (location = ABUFFER_COUNTER, size1x32) uimage2D abufferCounterImg;
#else
uniform vec4 *d_abuffer;
uniform coherent uint *d_abufferIdx;
#endif

layout (location = BLOCK) in Block
{
    vec4 pos;
    vec3 texCoord;
    vec3 normal;
} frag;

//Shade using green-white strips
vec3 shadeStrips(vec3 texcoord);

#if USE_ABUFFER

    void main(void) 
    {
        ivec2 coords = ivec2(gl_FragCoord.xy);

        //Check we are in the framebuffer
        if(coords.x >= 0 && coords.y >= 0 && coords.x < SCREEN_WIDTH && coords.y < SCREEN_HEIGHT )
        {
            //Atomic increment of the counter
            #if ABUFFER_USE_TEXTURES == 0
                int abidx = (int) atomicIncWrap(d_abufferIdx + coords.x + coords.y * SCREEN_WIDTH, ABUFFER_SIZE);
            #else
                int abidx = (int) imageAtomicIncWrap(abufferCounterImg, coords, ABUFFER_SIZE);
                //int abidx=imageAtomicAdd(abufferCounterImg, coords, 1);
            #endif

            //Create fragment to be stored
            vec4 abuffval;
            vec3 col;

            //Choose what we store per fragment
            #if ABUFFER_RESOLVE_GELLY == 0
                //Store color strips
                col = shadeStrips(frag.texCoord);
            #else
                //Store pseudo-illumination info
                vec3 N = normalize(frag.normal);
                vec3 L = normalize(vec3(0.0f,1.0f,1.0f));
                col = vec3(dot(N,L));
            #endif

            abuffval.rgb = col;
            abuffval.w = frag.pos.z;	//Will be used for sorting

            //Put fragment into A-Buffer
            #if ABUFFER_USE_TEXTURES == 0
                d_abuffer[coords.x + coords.y * SCREEN_WIDTH + (abidx * SCREEN_WIDTH * SCREEN_HEIGHT)] = abuffval;
            #else
                imageStore(abufferImg, ivec3(coords, abidx), abuffval);
            #endif
        }
        //Discard fragment so nothing is writen to the framebuffer
        discard;
    }
#else	//#if USE_ABUFFER

    layout (location = FRAG_COLOR) out vec4 outColor;

    void main(void) 
    {
        vec3 col = shadeStrips(frag.texCoord);
        outColor = vec4(col, 1.0f);
    }

#endif	//#if USE_ABUFFER

vec3 shadeStrips(vec3 texcoord)
{
    vec3 col;
    float i = floor(texcoord.x * 6.0f);

    col.rgb = fract(i * 0.5f) == 0.0f ? vec3(0.4f, 0.85f, 0.0f) : vec3(1.0f);
    col.rgb *= texcoord.z;

    return col;
}