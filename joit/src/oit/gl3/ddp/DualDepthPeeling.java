/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ddp;

import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import oit.BufferUtils;
import oit.Resources;
import oit.gl3.FullscreenQuad;
import oit.gl3.OIT;
import oit.gl3.Scene;
import oit.gl3.Semantic;

/**
 *
 * @author gbarbieri
 */
public class DualDepthPeeling extends OIT {

    private static final String SHADERS_ROOT = "/oit/gl3/ddp/shaders/";
    private static final String[] SHADERS_SRC = new String[]{"init", "peel", "blend", "final"};
    private static final float MAX_DEPTH = 1f;

    private class Program {

        public final static int INIT = 0;
        public final static int PEEL = 1;
        public final static int BLEND = 2;
        public final static int FINAL = 3;
        public final static int MAX = 4;
    }

    private class Buffer {

        public final static int PARAMETERS = 0;
        public final static int TRANSFORM2 = 1;
        public final static int MAX = 2;
    }

    private class Texture {

        public final static int DEPTH0 = 0;
        public final static int DEPTH1 = 1;
        public final static int FRONT_BLENDER0 = 2;
        public final static int FRONT_BLENDER1 = 3;
        public final static int BACK_TEMP0 = 4;
        public final static int BACK_TEMP1 = 5;
        public final static int BACK_BLENDER = 6;
        public final static int MAX = 7;
    }

    private class Framebuffer {

        public final static int SINGLE = 0;
        public final static int BACK_BLENDER = 1;
        public final static int MAX = 2;
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(Framebuffer.MAX),
            samplerName = GLBuffers.newDirectIntBuffer(1), queryName = GLBuffers.newDirectIntBuffer(1),
            samplesCount = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);

    @Override
    public void init(GL3 gl3) {

        clearColor = GLBuffers.newDirectFloatBuffer(4);
        clearDepth = GLBuffers.newDirectFloatBuffer(1);

        initBuffers(gl3);

        initPrograms(gl3);

        initSampler(gl3);

        initTargets(gl3);

        gl3.glGenQueries(1, queryName);

        Resources.fullscreenQuad = new FullscreenQuad(gl3);

        clearDepth.put(new float[]{1}).rewind();
    }

    private void initBuffers(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Float.BYTES, Resources.opacity, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(Buffer.PARAMETERS));

        Resources.matBuffer.asFloatBuffer().put(glm.ortho_(0, 1, 0, 1).toFa_());

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM2));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, Resources.matBuffer, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM2, bufferName.get(Buffer.TRANSFORM2));
    }

    private void initPrograms(GL3 gl3) {

        for (int program = Program.INIT; program <= Program.PEEL; program++) {

            ShaderCode vertShader = (program == Program.INIT) ? ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(),
                    SHADERS_ROOT, null, SHADERS_SRC[program], "vs", null, true)
                    : ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                            new String[]{SHADERS_SRC[Program.PEEL], "shade"}, "vs", null, null, null, true);;
            ShaderCode fragShader = (program == Program.INIT) ? ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(),
                    SHADERS_ROOT, null, SHADERS_SRC[program], "fs", null, true)
                    : ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                            new String[]{SHADERS_SRC[Program.PEEL], "shade"}, "fs", null, null, null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[program] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Transform0"),
                    Semantic.Uniform.TRANSFORM0);

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Transform1"),
                    Semantic.Uniform.TRANSFORM1);
        }
        gl3.glUniformBlockBinding(
                programName[Program.PEEL],
                gl3.glGetUniformBlockIndex(programName[Program.PEEL], "Parameters"),
                Semantic.Uniform.PARAMETERS);

        gl3.glUseProgram(programName[Program.PEEL]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.PEEL], "depthBlenderTex"),
                Semantic.Sampler.DEPTH);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.PEEL], "frontBlenderTex"),
                Semantic.Sampler.FRONT_BLENDER);

        for (int program = Program.BLEND; program <= Program.FINAL; program++) {

            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[program], "vs", null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[program], "fs", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[program] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Transform2"),
                    Semantic.Uniform.TRANSFORM2);
        }
        gl3.glUseProgram(programName[Program.BLEND]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.BLEND], "tempTex"),
                Semantic.Sampler.BACK_TEMP);

        gl3.glUseProgram(programName[Program.FINAL]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.FINAL], "frontBlenderTex"),
                Semantic.Sampler.FRONT_BLENDER);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.FINAL], "backBlenderTex"),
                Semantic.Sampler.BACK_BLENDER);
    }

    private void initSampler(GL3 gl3) {

        gl3.glGenSamplers(1, samplerName);

        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    @Override
    public void render(GL3 gl3, Scene scene) {

        gl3.glDisable(GL_DEPTH_TEST);
        gl3.glEnable(GL_BLEND);

        /**
         * (1) Initialize Min-Max Depth Buffer.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.SINGLE));

        /**
         * Render targets 1 and 2 store the front and back colors
         * Clear to 0.0 and use MAX blending to filter written color
         * At most one front color and one back color can be written every pass.
         */
//        clearColor.put(new float[]{0, 0.1f, 0.5f, 0.9f}).rewind();
//        gl3.glClearBufferfv(GL_COLOR, 1, new float[]{0, 0.1f, 0.5f, 0.9f}, 0);
//        gl3.glClearBufferfv(GL_COLOR, 2, clearColor);
        drawBuffers.position(1);
        gl3.glDrawBuffers(2, drawBuffers);
        gl3.glClearColor(0, 0, 0, 0);
        gl3.glClear(GL_COLOR_BUFFER_BIT);

//        gl3.glReadBuffer(GL_COLOR_ATTACHMENT1);
//        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(4 * Float.BYTES);
//        gl3.glReadPixels(500, 500, 1, 1, GL_RGBA, GL_FLOAT, buffer);
//        System.out.println("pixel (" + buffer.getFloat() + ", " + buffer.getFloat() + ", " + buffer.getFloat()
//                + ", " + buffer.getFloat() + ")");

        /**
         * Render target 0 stores (-minDepth, maxDepth, alphaMultiplier).
         */
        gl3.glDrawBuffer(drawBuffers.get(0));
        gl3.glClearColor(-MAX_DEPTH, -MAX_DEPTH, 0, 0);
        gl3.glClear(GL_COLOR_BUFFER_BIT);
        gl3.glBlendEquation(GL_MAX);

        gl3.glUseProgram(programName[Program.INIT]);
        scene.renderTransparent(gl3);

        /**
         * (2) Dual Depth Peeling + Blending
         *
         * Since we cannot blend the back colors in the geometry passes,
         * we use another render target to do the alpha blending.
         */
        gl3.glDrawBuffer(drawBuffers.get(6));
        gl3.glClearColor(1, 1, 1, 0);
        gl3.glClear(GL_COLOR_BUFFER_BIT);

        int currId = 0;

        for (int pass = 1; Resources.useOQ || pass < Resources.numPasses; pass++) {

            currId = pass % 2;
            int prevId = 1 - currId;
            int buffId = currId * 3;

            drawBuffers.position(buffId + 1);
            gl3.glDrawBuffers(2, drawBuffers);
            gl3.glClearColor(0, 0, 0, 0);
            gl3.glClear(GL_COLOR_BUFFER_BIT);

            gl3.glDrawBuffer(drawBuffers.get(buffId + 0));
            gl3.glClearColor(-MAX_DEPTH, -MAX_DEPTH, 0, 0);
            gl3.glClear(GL_COLOR_BUFFER_BIT);

            /**
             * Render target 0: RG32F MAX blending
             * Render target 1: RGBA MAX blending
             * Render target 2: RGBA MAX blending.
             */
            drawBuffers.position(buffId + 0);
            gl3.glDrawBuffers(3, drawBuffers);
            gl3.glBlendEquation(GL_MAX);

            gl3.glUseProgram(programName[Program.PEEL]);
            bindTextureRect(gl3, textureName.get(Texture.DEPTH0 + prevId), Semantic.Sampler.DEPTH, samplerName);
            bindTextureRect(gl3, textureName.get(Texture.FRONT_BLENDER0 + prevId), Semantic.Sampler.FRONT_BLENDER, samplerName);

            scene.renderTransparent(gl3);

            /**
             * Full screen pass to alpha-blend the back color.
             */
            gl3.glDrawBuffer(drawBuffers.get(6));

            gl3.glBlendEquation(GL_FUNC_ADD);
            gl3.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            if (Resources.useOQ) {
                gl3.glBeginQuery(GL_SAMPLES_PASSED, queryName.get(0));
            }

            gl3.glUseProgram(programName[Program.BLEND]);
            bindTextureRect(gl3, textureName.get(Texture.BACK_TEMP0 + currId), Semantic.Sampler.BACK_TEMP, samplerName);
            Resources.fullscreenQuad.render(gl3);

            if (Resources.useOQ) {
                gl3.glEndQuery(GL_SAMPLES_PASSED);
                gl3.glGetQueryObjectuiv(queryName.get(0), GL_QUERY_RESULT, samplesCount);
                if (samplesCount.get(0) == 0) {
                    break;
                }
            }
        }

        gl3.glDisable(GL_BLEND);

        /**
         * (3) Final Pass.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL_BACK);

        gl3.glUseProgram(programName[Program.FINAL]);
        bindTextureRect(gl3, textureName.get(Texture.FRONT_BLENDER0 + currId), Semantic.Sampler.FRONT_BLENDER, samplerName);
        bindTextureRect(gl3, textureName.get(Texture.BACK_BLENDER), Semantic.Sampler.BACK_BLENDER, samplerName);

        Resources.fullscreenQuad.render(gl3);
    }

    @Override
    public void reshape(GL3 gl3) {

        deleteTargets(gl3);
        initTargets(gl3);
    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);
        gl3.glGenFramebuffers(Framebuffer.MAX, framebufferName);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RG32F, Resources.imageSize.x, Resources.imageSize.y, 0,
                    GL_RG, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.FRONT_BLENDER0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Resources.imageSize.x, Resources.imageSize.y, 0,
                    GL_RGBA, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_TEMP0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Resources.imageSize.x, Resources.imageSize.y, 0,
                    GL_RGBA, GL_FLOAT, null);
        }

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_BLENDER));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGB8, Resources.imageSize.x, Resources.imageSize.y, 0,
                GL_RGB, GL_FLOAT, null);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.BACK_BLENDER));
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.BACK_BLENDER), 0);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.SINGLE));
        for (int i = 0; i < 2; i++) {
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + 3 * i, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.DEPTH0 + i), 0);
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1 + 3 * i, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.FRONT_BLENDER0 + i), 0);
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2 + 3 * i, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.BACK_TEMP0 + i), 0);
        }
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT6, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.BACK_BLENDER), 0);
    }

    private void deleteTargets(GL3 gl3) {

        gl3.glDeleteTextures(Texture.MAX, textureName);
        gl3.glDeleteFramebuffers(Framebuffer.MAX, framebufferName);
    }

    @Override
    public void dispose(GL3 gl3) {    
        deleteTargets(gl3);
    }
}
