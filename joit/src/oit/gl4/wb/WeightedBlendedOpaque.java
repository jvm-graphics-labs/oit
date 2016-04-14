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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import oit.framework.BufferUtils;
import oit.framework.Resources;
import oit.gl4.FullscreenQuad;
import oit.gl4.Scene;
import oit.gl4.Semantic;

/**
 *
 * @author gbarbieri
 */
public class WeightedBlendedOpaque {

    private FullscreenQuad fullscreenQuad;
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(1);

    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    private final String SHADERS_ROOT = "/oit/gl4/wb/shaders/";
    private final String[] SHADERS_NAME = new String[]{"opaque", "init", "final"};

    private class Program {

        public static final int OPAQUE = 0;
        public static final int INIT = 1;
        public static final int FINAL = 2;
        public static final int MAX = 3;
    }

    private class Texture {

        public static final int SUM_COLOR = 0;
        public static final int SUM_WEIGHT = 1;
        public static final int OPAQUE_COLOR = 2;
        public static final int OPAQUE_DEPTH = 3;
        public static final int MAX = 4;
    }

    private class Framebuffer {

        public static final int ACCUMULATION = 0;
        public static final int OPAQUE = 1;
        public static final int MAX = 2;
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(Framebuffer.MAX),
            drawBuffers = GLBuffers.newDirectIntBuffer(2);
    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4), 
            clearDepth = GLBuffers.newDirectFloatBuffer(1);

    public WeightedBlendedOpaque(GL4 gl4) {

        initSampler(gl4);

        initPrograms(gl4);

        fullscreenQuad = new FullscreenQuad(gl4);
    }

    private void initSampler(GL4 gl4) {

        FloatBuffer borderColorBuffer = GLBuffers.newDirectFloatBuffer(new float[]{0.0f, 0.0f, 0.0f, 0.0f});

        gl4.glCreateSamplers(1, samplerName);
        // TODO check GL_NEAREST_MIPMAP_NEAREST
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameterfv(samplerName.get(0), GL_TEXTURE_BORDER_COLOR, borderColorBuffer);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MIN_LOD, -1000.f);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MAX_LOD, 1000.f);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_LOD_BIAS, 0.0f);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_COMPARE_MODE, GL_NONE);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

        BufferUtils.destroyDirectBuffer(borderColorBuffer);
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

    public void render(GL4 gl4, Scene scene) {

        /**
         * (1) Initialize Opaque Depth Fbo.
         */
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.OPAQUE));
        gl4.glDrawBuffer(GL_COLOR_ATTACHMENT0);

        clearColor.put(1).put(1).put(1).put(1).rewind();
        clearDepth.put(1).rewind();
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor);
        gl4.glClearBufferfv(GL_DEPTH, 0, clearDepth);

        gl4.glEnable(GL_DEPTH_TEST);

        gl4.glUseProgram(programName[Program.OPAQUE]);
        scene.renderOpaque(gl4);

        gl4.glDisable(GL_DEPTH_TEST);
        /**
         * (2) Geometry Pass.
         */
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.ACCUMULATION));
        drawBuffers.put(GL_COLOR_ATTACHMENT0 + Semantic.Frag.SUM_COLOR)
                .put(GL_COLOR_ATTACHMENT0 + Semantic.Frag.SUM_WEIGHT).rewind();
        gl4.glDrawBuffers(2, drawBuffers);
        /**
         * Render target 0 stores a sum (weighted RGBA colors), clear it to 0.f
         * Render target 1 stores a product (transmittances), clear it to 1.f.
         */
        clearColor.put(0).put(0).put(0).put(0).rewind();
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor);
        clearColor.put(1).put(1).put(1).put(1).rewind();
        gl4.glClearBufferfv(GL_COLOR, 1, clearColor);

        gl4.glEnable(GL_BLEND);
        gl4.glBlendEquation(GL_FUNC_ADD);
        gl4.glBlendFunci(0, GL_ONE, GL_ONE);
        gl4.glBlendFunci(1, GL_ZERO, GL_ONE_MINUS_SRC_COLOR);

        gl4.glUseProgram(programName[Program.INIT]);

        gl4.glBindTextureUnit(Semantic.Sampler.OPAQUE_DEPTH, textureName.get(Texture.OPAQUE_DEPTH));
        gl4.glBindSampler(Semantic.Sampler.OPAQUE_DEPTH, samplerName.get(0));

        scene.renderTransparent(gl4);

        gl4.glDisable(GL_BLEND);
        /**
         * (3) Compositing Pass.
         */
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glDrawBuffer(GL_BACK);

        gl4.glUseProgram(programName[Program.FINAL]);

        gl4.glBindTextureUnit(Semantic.Sampler.SUM_COLOR, textureName.get(Texture.SUM_COLOR));
        gl4.glBindSampler(Semantic.Sampler.SUM_COLOR, samplerName.get(0));

        gl4.glBindTextureUnit(Semantic.Sampler.SUM_WEIGHT, textureName.get(Texture.SUM_WEIGHT));
        gl4.glBindSampler(Semantic.Sampler.SUM_WEIGHT, samplerName.get(0));

        gl4.glBindTextureUnit(Semantic.Sampler.OPAQUE_COLOR, textureName.get(Texture.OPAQUE_COLOR));
        gl4.glBindSampler(Semantic.Sampler.OPAQUE_COLOR, samplerName.get(0));

        fullscreenQuad.render(gl4);
    }

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

        gl4.glCreateFramebuffers(Framebuffer.MAX, framebufferName);

        gl4.glNamedFramebufferTexture(
                framebufferName.get(Framebuffer.ACCUMULATION),
                GL_COLOR_ATTACHMENT0 + Semantic.Frag.SUM_COLOR,
                textureName.get(Texture.SUM_COLOR),
                0);
        gl4.glNamedFramebufferTexture(
                framebufferName.get(Framebuffer.ACCUMULATION),
                GL_COLOR_ATTACHMENT0 + Semantic.Frag.SUM_WEIGHT,
                textureName.get(Texture.SUM_WEIGHT),
                0);

        /**
         * Opaque targets.
         */
        gl4.glTextureParameteri(textureName.get(Texture.OPAQUE_DEPTH), GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTextureParameteri(textureName.get(Texture.OPAQUE_DEPTH), GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTextureStorage2D(textureName.get(Texture.OPAQUE_DEPTH), 1, GL_DEPTH_COMPONENT32F, 
                Resources.imageSize.x, Resources.imageSize.y);

        gl4.glTextureParameteri(textureName.get(Texture.OPAQUE_COLOR), GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTextureParameteri(textureName.get(Texture.OPAQUE_COLOR), GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTextureStorage2D(textureName.get(Texture.OPAQUE_COLOR), 1, GL_RGBA8, Resources.imageSize.x, 
                Resources.imageSize.y);

        gl4.glNamedFramebufferTexture(
                framebufferName.get(Framebuffer.OPAQUE),
                GL_DEPTH_ATTACHMENT,
                textureName.get(Texture.OPAQUE_DEPTH),
                0);
        gl4.glNamedFramebufferTexture(
                framebufferName.get(Framebuffer.OPAQUE),
                GL_COLOR_ATTACHMENT0,
                textureName.get(Texture.OPAQUE_COLOR),
                0);
    }

    private void deleteRenderTargets(GL4 gl4) {

        for (int framebuffer = 0; framebuffer < Framebuffer.MAX; framebuffer++) {
            if (gl4.glIsFramebuffer(framebufferName.get(framebuffer))) {
                framebufferName.position(framebuffer);
                gl4.glDeleteFramebuffers(1, framebufferName);
                framebufferName.rewind();
            }
        }
        for (int texture = 0; texture < Texture.MAX; texture++) {
            if (gl4.glIsTexture(textureName.get(texture))) {
                textureName.position(texture);
                gl4.glDeleteTextures(1, textureName);
                textureName.rewind();
            }
        }
    }

    public void dispose(GL4 gl4) {

        deleteRenderTargets(gl4);

        BufferUtils.destroyDirectBuffer(framebufferName);
        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(clearColor);
        BufferUtils.destroyDirectBuffer(clearDepth);
        BufferUtils.destroyDirectBuffer(drawBuffers);
    }
}
