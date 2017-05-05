/**
 * Fast Single-pass A-Buffer using OpenGL 4.0
 * Copyright Cyril Crassin, June 2010
**/

#version 450

#include semantic.glsl

layout (location = POSITION) in vec4 position;

void main()
{
    gl_Position = position;
}