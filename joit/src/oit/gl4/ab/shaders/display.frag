/**
 * Fast Single-pass A-Buffer using OpenGL 4.0
 * Copyright Cyril Crassin, June 2010
**/

#version 450

#include semantic.glsl

const vec4 backgroundColor = vec4(BACKGROUND_COLOR_R, BACKGROUND_COLOR_G, BACKGROUND_COLOR_B, 0.0f);
const float fragmentAlpha = 0.5f;

//Whole number pixel offsets (not necessary just to test the layout keyword !)
layout(pixel_center_integer) in vec4 gl_FragCoord;

//Output fragment color
layout (location = FRAG_COLOR) out vec4 outColor;

#if ABUFFER_USE_TEXTURES    
    layout (binding = ABUFFER, rgba32f) uniform image2DArray abufferImg;
    layout (binding = ABUFFER_COUNTER, r32ui) uniform uimage2D abufferCounterImg;
#else
    uniform vec4 *d_abuffer;
    uniform uint *d_abufferIdx;
#endif

//Local memory array (probably in L1)
vec4 fragmentList[ABUFFER_SIZE];

//Keeps only closest fragment
vec4 resolveClosest(ivec2 coords, int abNumFrag);
//Blend fragments front-to-back
vec4 resolveAlphaBlend(ivec2 coords, int abNumFrag);
//Compute gelly shader
vec4 resolveGelly(ivec2 coords, int abNumFrag);

//Resolve A-Buffer and blend sorted fragments
void main(void) 
{
    ivec2 coords = ivec2(gl_FragCoord.xy);
	
    if (coords.x >= 0 && coords.y >= 0 && coords.x < SCREEN_WIDTH && coords.y < SCREEN_HEIGHT )
    {
        //Load the number of fragments in the current pixel.
        /*#if ABUFFER_USE_TEXTURES
            int abNumFrag = (int) imageLoad(abufferCounterImg, coords).r;
        #else
            int abNumFrag = (int) d_abufferIdx[coords.x + coords.y * SCREEN_WIDTH];

            //Crash without this (WTF ??)
            if(abNumFrag < 0)
                abNumFrag = 0;
            if(abNumFrag > ABUFFER_SIZE) 
                abNumFrag = ABUFFER_SIZE;
        #endif
        //outColor = vec4(0,1,1,1);
        //outColor = imageLoad(abufferImg, ivec3(gl_FragCoord.xy, 0));
        //outColor = outColor = resolveClosest(coords, abNumFrag);
        if (abNumFrag > 0)
        {
            //Copute ans output final color for the frame buffer
            #if ABUFFER_RESOLVE_USE_SORTING == 0	
                //If we only want the closest fragment
                outColor = resolveClosest(coords, abNumFrag);
            #elif ABUFFER_RESOLVE_GELLY
                //We want to sort and apply gelly shader
                outColor = resolveGelly(coords, abNumFrag);
            #else
                //We want to sort and blend fragments
                outColor = resolveAlphaBlend(coords, abNumFrag);
            #endif

        } 
        else 
            //If no fragment, write nothing
            discard;*/
    }	
    outColor = imageLoad(abufferImg, ivec3(gl_FragCoord.xy, 0));
}

vec4 resolveClosest(ivec2 coords, int abNumFrag)
{
    //Search smallest z
    vec4 minFrag = vec4(0.0f, 0.0f, 0.0f, 1000000.0f);
    for (int i = 0; i < abNumFrag; i++)
    {
        #if ABUFFER_USE_TEXTURES
            vec4 val = imageLoad(abufferImg, ivec3(coords, i));
        #else
            vec4 val = d_abuffer[coords.x + coords.y * SCREEN_WIDTH + (i * SCREEN_WIDTH * SCREEN_HEIGHT)];
        #endif
        if(val.w < minFrag.w)
            minFrag = val;
    }
    //Output final color for the frame buffer
    return minFrag;
}

void fillFragmentArray(ivec2 coords, int abNumFrag)
{
    //Load fragments into a local memory array for sorting
    for (int i = 0; i < abNumFrag; i++)
    {
        #if ABUFFER_USE_TEXTURES
            fragmentList[i] = imageLoad(abufferImg, ivec3(coords, i));
        #else
            fragmentList[i] = d_abuffer[coords.x + coords.y * SCREEN_WIDTH + (i * SCREEN_WIDTH * SCREEN_HEIGHT)];
        #endif
    }
}

//Bubble sort used to sort fragments
void bubbleSort(int array_size);
//Bitonic sort test
void bitonicSort( int n );

//Blend fragments front-to-back
vec4 resolveAlphaBlend(ivec2 coords, int abNumFrag)
{	
    //Copy fragments in local array
    fillFragmentArray(coords, abNumFrag);

    //Sort fragments in local memory array
    bubbleSort(abNumFrag);
		
    vec4 finalColor = vec4(0.0f);


    const float sigma = 30.0f;
    float thickness = fragmentList[0].w / 2.0f;

    finalColor = vec4(0.0f);
    for (int i = 0; i < abNumFrag; i++)
    {
        vec4 frag = fragmentList[i];

        vec4 col;
        col.rgb = frag.rgb;
        col.w = fragmentAlpha;	//uses constant alpha

        #if ABUFFER_RESOLVE_ALPHA_CORRECTION
            if (i % 2 == abNumFrag % 2)
                thickness = (fragmentList[i + 1].w - frag.w) * 0.5f;
            col.w = 1.0f - pow(1.0f - col.w, thickness * sigma);
        #endif
        col.rgb = col.rgb * col.w;

        finalColor = finalColor + col * (1.0f - finalColor.a);
    }

    finalColor = finalColor + backgroundColor * (1.0f - finalColor.a);

    return finalColor;
}

//Blend fragments front-to-back
vec4 resolveGelly(ivec2 coords, int abNumFrag)
{	
    //Copy fragments in local array
    fillFragmentArray(coords, abNumFrag);

    //Sort fragments in local memory array
    bubbleSort(abNumFrag);

    float thickness = 0.0f;
    vec4 accumColor = vec4(0.0f);

    vec4 prevFrag;
    for (int i = 0; i < abNumFrag; i++)
    {
        vec4 frag = fragmentList[i];

        if (i % 2 == 1)
            thickness += frag.w - prevFrag.w;

        vec4 col;
        col.rgb = frag.rgb;
        col.w = 0.5f;	//uses constant alpha

        col.rgb = col.rgb * col.w;
        accumColor = accumColor + col * (1.0f - accumColor.a);

        prevFrag = frag;
    }
    accumColor = accumColor + backgroundColor * (1.0f - accumColor.a);

    //float thickness=fragmentList[abNumFrag-1].w-fragmentList[0].w;
    float sigma = 20.0f;
    float ia = exp(-sigma * thickness);
    float ka = 0.8;

    vec4 finalColor = vec4(0.0f);
    finalColor = ka * ia + (1.0 - ka) * fragmentList[0]; //Same as Bavoil 2005
    //finalColor = ka * ia + (1.0-ka) * accumColor;   //Uses accumulated Color

    const vec4 jade = vec4(.4, .14, .11, 1.0) * 8.0f;
    const vec4 green = vec4(0.3f, 0.7f, 0.1f, 1.0f) * 4.0f;
    finalColor *= jade;

    return finalColor;
}

//Bubble sort used to sort fragments
void bubbleSort(int array_size) 
{
    for (int i = (array_size - 2); i >= 0; --i) 
    {
        for (int j = 0; j <= i; ++j) 
        {
            if (fragmentList[j].w > fragmentList[j + 1].w) 
            {
                vec4 temp = fragmentList[j + 1];
                fragmentList[j + 1] = fragmentList[j];
                fragmentList[j] = temp;
            }
        }
    }
}

//Swap function
void swapFragArray(int n0, int n1)
{
    vec4 temp = fragmentList[n1];
    fragmentList[n1] = fragmentList[n0];
    fragmentList[n0] = temp;
}

//->Same artefact than in L.Bavoil
//Bitonic sort: http://www.tools-of-computing.com/tc/CS/Sorts/bitonic_sort.htm
void bitonicSort( int n ) 
{
    int i, j, k;
    for (k = 2; k <= n; k = 2 * k) 
    {
        for (j = k >> 1; j > 0; j = j >> 1) 
        {
            for (i = 0; i < n; i++) 
            {
                int ixj = i ^ j;
                if (ixj > i) 
                {
                    if ((i & k) == 0 && fragmentList[i].w > fragmentList[ixj].w)
                        swapFragArray(i, ixj);

                    if ((i & k) != 0 && fragmentList[i].w < fragmentList[ixj].w) 
                        swapFragArray(i, ixj);
                }
            }
        }
    }
}