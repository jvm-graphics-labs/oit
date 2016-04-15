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
import oit.framework.Resources;
import oit.gl3.OIT;
import oit.framework.Scene;
import oit.gl3.Semantic;
import oit.gl3.Viewer;

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
            framebufferName = GLBuffers.newDirectIntBuffer(1);
    private int[] programName = new int[Program.MAX];

    @Override
    public void init(GL3 gl3) {

        initPrograms(gl3);

        initTargets(gl3);
    }

    private void initPrograms(GL3 gl3) {
        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.INIT], "shade"}, "vert", null, null, null, true);;
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.INIT], "shade"}, "frag", null, null, null, true);

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
        gl3.glUseProgram(programName[Program.INIT]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.INIT], "opaqueDepthTex"), 
                Semantic.Sampler.OPAQUE_DEPTH);
        
        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[Program.FINAL], "vert", null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[Program.FINAL], "frag", null, true);

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
            gl3.glUniform1i(
                    gl3.glGetUniformLocation(programName[Program.FINAL], "opaqueColorTex"),
                    Semantic.Sampler.OPAQUE_COLOR);
        }

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
        gl3.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 0));
        gl3.glClearBufferfv(GL_COLOR, 1, Resources.clearColor);

        gl3.glBlendEquation(GL_FUNC_ADD);
        gl3.glBlendFunc(GL_ONE, GL_ONE);
        gl3.glEnable(GL_BLEND);

        gl3.glUseProgram(programName[Program.INIT]);
        bindRectTex(gl3, Viewer.textureName.get(Viewer.Texture.DEPTH), Semantic.Sampler.OPAQUE_DEPTH);
        scene.renderTransparent(gl3);

        gl3.glDisable(GL_BLEND);
        /**
         * (2) Approximate Blending.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL_BACK);

        gl3.glUseProgram(programName[Program.FINAL]);
        bindRectTex(gl3, textureName.get(Texture.SUM_COLOR), Semantic.Sampler.SUM_COLOR);
        bindRectTex(gl3, textureName.get(Texture.COUNT), Semantic.Sampler.COUNT);
        bindRectTex(gl3, Viewer.textureName.get(Viewer.Texture.COLOR), Semantic.Sampler.OPAQUE_COLOR);
        Viewer.fullscreenQuad.render(gl3);
    }

    @Override
    public void reshape(GL3 gl3) {
        deleteTargets(gl3);
        initTargets(gl3);
    }

    @Override
    public void dispose(GL3 gl3) {
        deleteTargets(gl3);
    }

    private void deleteTargets(GL3 gl3) {
        gl3.glDeleteFramebuffers(1, framebufferName);
        gl3.glDeleteFramebuffers(Texture.MAX, textureName);
    }
}
