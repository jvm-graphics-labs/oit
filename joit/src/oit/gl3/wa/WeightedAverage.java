/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.wa;

import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.IntBuffer;
import oit.Resources;
import oit.gl3.OIT;
import oit.gl3.Scene;
import oit.gl3.Semantic;

/**
 *
 * @author gbarbieri
 */
public class WeightedAverage extends OIT {

    private static final String SHADERS_ROOT = "/oit/gl3/wa/shaders/";
    private static final String[] SHADERS_SRC = new String[]{"init", "final"};

    private class Texture {

        public static final int SUM_COLOR = 0;
        public static final int COUNT = 1;
        public static final int MAX = 2;
    }

    private class Program {

        public static final int INIT = 0;
        public static final int FINAL = 1;
        public static final int MAX = 2;
    }

    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(1), samplerName = GLBuffers.newDirectIntBuffer(1);
    private int[] programName = new int[Program.MAX];

    @Override
    public void init(GL3 gl3) {

        initPrograms(gl3);

        initSampler(gl3);

        initTargets(gl3);
    }

    private void initPrograms(GL3 gl3) {
        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.INIT], "shade"}, "vs", null, null, null, true);;
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.INIT], "shade"}, "fs", null, null, null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[Program.INIT] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[Program.INIT],
                    gl3.glGetUniformBlockIndex(programName[Program.INIT], "Transform0"),
                    Semantic.Uniform.TRANSFORM0);

            gl3.glUniformBlockBinding(
                    programName[Program.INIT],
                    gl3.glGetUniformBlockIndex(programName[Program.INIT], "Transform1"),
                    Semantic.Uniform.TRANSFORM1);

            gl3.glUniformBlockBinding(
                    programName[Program.INIT],
                    gl3.glGetUniformBlockIndex(programName[Program.INIT], "Parameters"),
                    Semantic.Uniform.PARAMETERS);
        }
        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[Program.FINAL], "vs", null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[Program.FINAL], "fs", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[Program.FINAL] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[Program.FINAL],
                    gl3.glGetUniformBlockIndex(programName[Program.FINAL], "Transform2"),
                    Semantic.Uniform.TRANSFORM2);

            gl3.glUseProgram(programName[Program.FINAL]);
            gl3.glUniform1i(
                    gl3.glGetUniformLocation(programName[Program.FINAL], "sumColorTex"),
                    Semantic.Sampler.SUM_COLOR);
            gl3.glUniform1i(
                    gl3.glGetUniformLocation(programName[Program.FINAL], "countTex"),
                    Semantic.Sampler.COUNT);
        }

    }

    private void initSampler(GL3 gl3) {

        gl3.glGenSamplers(1, samplerName);

        gl3.glSamplerParameteri(samplerName.get(0), GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(samplerName.get(0), GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.SUM_COLOR));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA16F, Resources.imageSize.x, Resources.imageSize.y, 0, GL_RGBA,
                GL_FLOAT, null);

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.COUNT));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_R32F, Resources.imageSize.x, Resources.imageSize.y, 0, GL_RGBA,
                GL_FLOAT, null);

        gl3.glGenFramebuffers(1, framebufferName);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));

        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.SUM_COLOR), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.COUNT), 0);
    }

    @Override
    public void render(GL3 gl3, Scene scene) {

        gl3.glDisable(GL_DEPTH_TEST);
        /**
         * (1) Accumulate Colors and Depth Complexity.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        drawBuffers.position(0);
        gl3.glDrawBuffers(2, drawBuffers);

        gl3.glClearColor(0, 0, 0, 0);
        gl3.glClear(GL_COLOR_BUFFER_BIT);

        gl3.glBlendEquation(GL_FUNC_ADD);
        gl3.glBlendFunc(GL_ONE, GL_ONE);
        gl3.glEnable(GL_BLEND);

        gl3.glUseProgram(programName[Program.INIT]);
        scene.renderTransparent(gl3);

        gl3.glDisable(GL_BLEND);
        /**
         * (2) Approximate Blending.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL_BACK);

        gl3.glUseProgram(programName[Program.FINAL]);
//            gl3.glUniform3f(finale.getBackgroundColorUL(), 1, 1, 1);
        bindTextureRect(gl3, textureName.get(Texture.SUM_COLOR), Semantic.Sampler.SUM_COLOR, samplerName);
        bindTextureRect(gl3, textureName.get(Texture.COUNT), Semantic.Sampler.COUNT, samplerName);
        Resources.fullscreenQuad.render(gl3);
    }

    @Override
    public void reshape(GL3 gl3) {

        deleteTargets(gl3);
        initTargets(gl3);
    }

    @Override
    public void dispose(GL3 gl3) {

    }

    private void deleteTargets(GL3 gl3) {

        gl3.glDeleteFramebuffers(1, framebufferName);
        gl3.glDeleteFramebuffers(Texture.MAX, textureName);
    }
}
