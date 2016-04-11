/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dp;

import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import jglm.Jglm;
import oit.BufferUtils;
import oit.Settings;
import oit.gl3.FullscreenQuad;
import oit.gl3.FullscreenQuad;
import oit.gl3.Scene;
import oit.gl3.Viewer;
import oit.gl3.OIT;
import oit.gl3.OIT;
import oit.gl3.Scene;
import oit.gl3.Viewer;
import oit.gl3.Semantic;

/**
 *
 * @author gbarbieri
 */
public class DepthPeeling extends OIT {

    private static final String SHADERS_ROOT = "/oit/gl3/dp/shaders/";
    private static final String[] SHADERS_SRC = new String[]{"init", "peel", "blend", "final"};
    private FullscreenQuad fullscreenQuad;
    private int numGeoPasses, numPasses;
    private boolean useOQ;

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
        public final static int COLOR0 = 2;
        public final static int COLOR1 = 3;
        public final static int COLOR_BLENDER = 4;
        public final static int MAX = 5;
    }

    private class Framebuffer {

        public final static int _0 = 0;
        public final static int _1 = 1;
        public final static int COLOR_BLENDER = 2;
        public final static int MAX = 3;
    }

    private int[] programName = new int[Program.MAX];
    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(Framebuffer.MAX),
            queryName = GLBuffers.newDirectIntBuffer(1), samplerName = GLBuffers.newDirectIntBuffer(1),
            samplesCount = GLBuffers.newDirectIntBuffer(1);

    @Override
    public void init(GL3 gl3) {

        numGeoPasses = 0;
        numPasses = 4;

        useOQ = true;

        clearColor = GLBuffers.newDirectFloatBuffer(4);
        clearDepth = GLBuffers.newDirectFloatBuffer(1);

        initBuffers(gl3);

        initPrograms(gl3);

        initSampler(gl3);

        initTargets(gl3);

        gl3.glGenQueries(1, queryName);

        fullscreenQuad = new FullscreenQuad(gl3);

        clearDepth.put(new float[]{1}).rewind();
    }

    private void initBuffers(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Float.BYTES, null, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(Buffer.PARAMETERS));

        ByteBuffer modelToClip = GLBuffers.newDirectByteBuffer(glm.mat._4.Mat4.SIZE);
        modelToClip.asFloatBuffer().put(Jglm.orthographic2D(0, 1, 0, 1).toFloatArray());

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM2));
        gl3.glBufferData(GL_UNIFORM_BUFFER, glm.mat._4.Mat4.SIZE, modelToClip, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM2, bufferName.get(Buffer.TRANSFORM2));

        BufferUtils.destroyDirectBuffer(modelToClip);
    }

    private void initPrograms(GL3 gl3) {

        // init & peel
        for (int program = Program.INIT; program <= Program.PEEL; program++) {

            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[program], "shade"}, "vs", null, null, null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[program], "shade"}, "fs", null, null, null, true);

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

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Parameters"),
                    Semantic.Uniform.PARAMETERS);

            gl3.glUseProgram(programName[program]);
            gl3.glUniform1i(gl3.glGetUniformLocation(programName[program], "opaqueDepthTex"),
                    Semantic.Sampler.OPAQUE_DEPTH);
        }

        gl3.glUseProgram(programName[Program.PEEL]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.PEEL], "depthTex"),
                Semantic.Sampler.DEPTH);

        // blend & final
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
                Semantic.Sampler.TEMP);

        gl3.glUseProgram(programName[Program.FINAL]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.FINAL], "colorTex"),
                Semantic.Sampler.COLOR);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.FINAL], "opaqueColorTex"),
                Semantic.Sampler.OPAQUE_COLOR);
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

        numGeoPasses = 0;
        /**
         * (1) Initialize Min Depth Buffer.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.COLOR_BLENDER));
        gl3.glDrawBuffer(drawBuffers.get(0));

        clearColor.put(new float[]{0, 0, 0, 1}).rewind();
        gl3.glClearBufferfv(GL_COLOR, 0, clearColor);
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth);

        gl3.glEnable(GL_DEPTH_TEST);

        gl3.glUseProgram(programName[Program.INIT]);
        bindTextureRect(gl3, Viewer.textureName.get(Viewer.Texture.DEPTH), Semantic.Sampler.OPAQUE_DEPTH, samplerName);

        scene.renderTransparent(gl3);
        numGeoPasses++;
        /**
         * (2) Depth Peeling + Blending
         *
         * numLayers is useful if occlusion queries are disabled In this case,
         * increasing/decreasing numPasses lets you see the intermediate results
         * and compare the intermediate results of front-to-back peeling vs dual
         * depth peeling for a given budget of geometry passes (numPasses).
         */
        int numLayers = (numPasses - 1) * 2;
        /**
         * careful, && means you wont go deeper of the numLayers, you might be
         * done earlier but for sure not further than that
         *
         * || means you will peel until you render something or you didnt reach
         * yet the max numLayers.
         */
        for (int layer = 1; useOQ || layer < numLayers; layer++) {

            int currId = layer % 2;
            int prevId = 1 - currId;

            gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer._0 + currId));
            gl3.glDrawBuffer(drawBuffers.get(0));

            clearColor.put(3, 0);
            gl3.glClearBufferfv(GL_COLOR, 0, clearColor);
            gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth);

            gl3.glDisable(GL_BLEND);
            gl3.glEnable(GL_DEPTH_TEST);

            if (useOQ) {
                gl3.glBeginQuery(GL_SAMPLES_PASSED, queryName.get(0));
            }

            gl3.glUseProgram(programName[Program.PEEL]);
            bindTextureRect(gl3, textureName.get(Texture.DEPTH0 + prevId), Semantic.Sampler.DEPTH, samplerName);
            bindTextureRect(gl3, Viewer.textureName.get(Viewer.Texture.DEPTH), Semantic.Sampler.OPAQUE_DEPTH, samplerName);

            scene.renderTransparent(gl3);
            numGeoPasses++;

            if (useOQ) {
                gl3.glEndQuery(GL_SAMPLES_PASSED);
            }

            gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.COLOR_BLENDER));
            gl3.glDrawBuffer(drawBuffers.get(0));

            gl3.glDisable(GL_DEPTH_TEST);
            gl3.glEnable(GL_BLEND);

            gl3.glBlendEquation(GL_FUNC_ADD);
            gl3.glBlendFuncSeparate(GL_DST_ALPHA, GL_ONE, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA);

            gl3.glUseProgram(programName[Program.BLEND]);
            bindTextureRect(gl3, textureName.get(Texture.COLOR0 + currId), Semantic.Sampler.TEMP, samplerName);            

            fullscreenQuad.render(gl3);

            gl3.glDisable(GL_BLEND);

            if (useOQ) {
                gl3.glGetQueryObjectuiv(queryName.get(0), GL_QUERY_RESULT, samplesCount);
                if (samplesCount.get(0) == 0) {
                    break;
                }
            }
        }
        /**
         * (3) Final Pass.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL_BACK);
        gl3.glDisable(GL_DEPTH_TEST);

        gl3.glUseProgram(programName[Program.FINAL]);
        bindTextureRect(gl3, textureName.get(Texture.COLOR_BLENDER), Semantic.Sampler.COLOR, samplerName);
        bindTextureRect(gl3, Viewer.textureName.get(Viewer.Texture.COLOR), Semantic.Sampler.OPAQUE_COLOR, samplerName);

        fullscreenQuad.render(gl3);
    }

    @Override
    public void reshape(GL3 gl3) {

        deleteTargets(gl3);
        initTargets(gl3);
    }

    @Override
    public void dispose(GL3 gl3) {

    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);
        gl3.glGenFramebuffers(Framebuffer.MAX, framebufferName);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_DEPTH_COMPONENT32F, Settings.imageSize.x, Settings.imageSize.y, 0,
                    GL_DEPTH_COMPONENT, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.COLOR0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Settings.imageSize.x, Settings.imageSize.y, 0, GL_RGBA,
                    GL_FLOAT, null);

            gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer._0 + i));

            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.DEPTH0 + i), 0);
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.COLOR0 + i), 0);

            if (gl3.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new Error("framebuffer (_0 + " + i + ") incomplete!");
            }
        }

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.COLOR_BLENDER));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Settings.imageSize.x, Settings.imageSize.y, 0, GL_RGBA, GL_FLOAT,
                null);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.COLOR_BLENDER));
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.DEPTH0), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.COLOR_BLENDER), 0);

        if (gl3.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new Error("framebuffer COLOR_BLENDER incomplete!");
        }
    }

    private void deleteTargets(GL3 gl3) {

        gl3.glDeleteFramebuffers(Framebuffer.MAX, framebufferName);
        gl3.glDeleteTextures(Texture.MAX, textureName);
    }

}
