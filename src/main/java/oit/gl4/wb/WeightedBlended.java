/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.wb;

import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.IntBuffer;
import oit.framework.Resources;
import oit.gl4.FullscreenQuad;
import oit.gl4.OIT;
import oit.gl4.Scene;
import oit.gl4.Semantic;
import oit.gl4.Viewer;

/**
 *
 * @author gbarbieri
 */
public class WeightedBlended extends OIT {

    private FullscreenQuad fullscreenQuad;
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(1);

    private final String SHADERS_ROOT = "/oit/gl4/wb/shaders/";
    private final String[] SHADERS_NAME = new String[]{"init", "final"};

    private class Program {

        public static final int INIT = 0;
        public static final int FINAL = 1;
        public static final int MAX = 2;
    }

    private class Texture {

        public static final int SUM_COLOR = 0;
        public static final int SUM_WEIGHT = 1;
        public static final int MAX = 2;
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(1);

    @Override
    public void init(GL4 gl4) {

        initPrograms(gl4);

        fullscreenQuad = new FullscreenQuad(gl4);
    }

    private void initPrograms(GL4 gl4) {

        for (int program = 0; program < Program.MAX; program++) {

            ShaderCode vertShader = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_NAME[program], "vert", null, true);
            ShaderCode fragShader = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_NAME[program], "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl4, System.out);

            programName[program] = shaderProgram.program();
        }
    }

    @Override
    public void render(GL4 gl4, Scene scene) {

        gl4.glDisable(GL_DEPTH_TEST);
        /**
         * (1) Geometry Pass.
         */
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        gl4.glDrawBuffers(2, drawBuffers);
        /**
         * Render target 0 stores a sum (weighted RGBA colors), clear it to 0.f
         * Render target 1 stores a product (transmittances), clear it to 1.f.
         */
        gl4.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 0));
        gl4.glClearBufferfv(GL_COLOR, 1, Resources.clearColor.put(0, 1).put(1, 1).put(2, 1).put(3, 1));

        gl4.glEnable(GL_BLEND);
        gl4.glBlendEquation(GL_FUNC_ADD);
        gl4.glBlendFunci(0, GL_ONE, GL_ONE);
        gl4.glBlendFunci(1, GL_ZERO, GL_ONE_MINUS_SRC_COLOR);

        gl4.glUseProgram(programName[Program.INIT]);

        gl4.glBindTextureUnit(Semantic.Sampler.OPAQUE_DEPTH, Viewer.textureName.get(Viewer.Texture.DEPTH));
        gl4.glBindSampler(Semantic.Sampler.OPAQUE_DEPTH, samplerName.get(0));

        scene.renderTransparent(gl4);

        gl4.glDisable(GL_BLEND);
        /**
         * (2) Compositing Pass.
         */
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glDrawBuffer(GL_BACK);

        gl4.glUseProgram(programName[Program.FINAL]);

        gl4.glBindTextureUnit(Semantic.Sampler.SUM_COLOR, textureName.get(Texture.SUM_COLOR));
        gl4.glBindSampler(Semantic.Sampler.SUM_COLOR, samplerName.get(0));

        gl4.glBindTextureUnit(Semantic.Sampler.SUM_WEIGHT, textureName.get(Texture.SUM_WEIGHT));
        gl4.glBindSampler(Semantic.Sampler.SUM_WEIGHT, samplerName.get(0));

        gl4.glBindTextureUnit(Semantic.Sampler.OPAQUE_COLOR, Viewer.textureName.get(Viewer.Texture.COLOR));
        gl4.glBindSampler(Semantic.Sampler.OPAQUE_COLOR, samplerName.get(0));

        fullscreenQuad.render(gl4);
    }

    @Override
    public void reshape(GL4 gl4) {
        deleteRenderTargets(gl4);
        initRenderTargets(gl4);
    }

    private void initRenderTargets(GL4 gl4) {

        gl4.glCreateTextures(GL_TEXTURE_RECTANGLE, Texture.MAX, textureName);

        gl4.glTextureParameteri(textureName.get(Texture.SUM_COLOR), GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTextureParameteri(textureName.get(Texture.SUM_COLOR), GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTextureStorage2D(textureName.get(Texture.SUM_COLOR), 1, GL_RGBA16F, Resources.imageSize.x,
                Resources.imageSize.y);

        gl4.glTextureParameteri(textureName.get(Texture.SUM_WEIGHT), GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTextureParameteri(textureName.get(Texture.SUM_WEIGHT), GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTextureStorage2D(textureName.get(Texture.SUM_WEIGHT), 1, GL_R8, Resources.imageSize.x,
                Resources.imageSize.y);

        gl4.glCreateFramebuffers(1, framebufferName);

        gl4.glNamedFramebufferTexture(
                framebufferName.get(0),
                GL_COLOR_ATTACHMENT0 + Semantic.Frag.SUM_COLOR,
                textureName.get(Texture.SUM_COLOR),
                0);
        gl4.glNamedFramebufferTexture(
                framebufferName.get(0),
                GL_COLOR_ATTACHMENT0 + Semantic.Frag.SUM_WEIGHT,
                textureName.get(Texture.SUM_WEIGHT),
                0);
    }

    private void deleteRenderTargets(GL4 gl4) {
        gl4.glDeleteFramebuffers(1, framebufferName);
        gl4.glDeleteTextures(Texture.MAX, textureName);
    }

    @Override
    public void dispose(GL4 gl4) {
        deleteRenderTargets(gl4);
    }
}
