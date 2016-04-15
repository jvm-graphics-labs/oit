
// Extensions
#extension GL_NV_gpu_shader5 : enable
#extension GL_EXT_shader_image_load_store : enable
#extension GL_EXT_bindable_uniform : enable
#extension GL_NV_shader_buffer_load : enable
//#extension GL_NV_shader_buffer_store : enable (implied by GL_NV_gpu_program5 or GL_NV_gpu_shader5)

//Macros changed from the C++ side
#define SCREEN_WIDTH            1024
#define SCREEN_HEIGHT           768
#define USE_ABUFFER             0
#define ABUFFER_SIZE            16
#define ABUFFER_USE_TEXTURES    1
#define ABUFFER_RESOLVE_GELLY   0

#define BACKGROUND_COLOR_R  1.0f
#define BACKGROUND_COLOR_G  1.0f
#define BACKGROUND_COLOR_B  1.0f

#define ABUFFER_RESOLVE_USE_SORTING 0

#define ABUFFER_RESOLVE_ALPHA_CORRECTION    0

// Attributes
#define POSITION    0
#define NORMAL      1

// Uniforms
#define TRANSFORM0  0
#define TRANSFORM1  1
#define TRANSFORM2  2
#define PARAMETERS  3

// Samplers
#define ABUFFER         0
#define ABUFFER_COUNTER 1

// Interfaces
#define BLOCK       0

// Outputs
#define FRAG_COLOR  0
#define SUM_COLOR   0
#define SUM_WEIGHT  1

precision highp float;
precision highp int;
layout (std140, column_major) uniform;
layout (std430, column_major) buffer;
/*
layout (binding = PARAMETERS) uniform Parameters
{
    int screenWidth;
    int screenHeight;
    int useABuffer;
    int abufferSize;
    int useTextures;
    int abResolveGelly;
    int resolveUseSorting;
    int resolveAlphaCorrection;
    vec4 backgroundColor;
};*/