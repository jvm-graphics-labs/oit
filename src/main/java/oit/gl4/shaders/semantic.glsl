// Attributes
#define POSITION    0
#define NORMAL      1

// Uniforms
#define TRANSFORM0  0
#define TRANSFORM1  1
#define TRANSFORM2  2
#define PARAMETERS  3

// Samplers
#define OPAQUE_DEPTH    0
#define OPAQUE_COLOR    3
#define SUM_COLOR_      1
#define SUM_WEIGHT_     2

// Interfaces
#define BLOCK       0

// Outputs
#define FRAG_COLOR  0
#define SUM_COLOR   0
#define SUM_WEIGHT  1

precision highp float;
precision highp int;
layout(std140, column_major) uniform;
layout(std430, column_major) buffer;