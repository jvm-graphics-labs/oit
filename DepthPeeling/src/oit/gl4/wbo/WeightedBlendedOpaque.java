/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.wbo;

import javax.media.opengl.GL4;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec2i;
import oit.gl4.FullscreenQuad;
import oit.gl4.Scene;
import oit.gl4.wbo.glsl.Final;
import oit.gl4.wbo.glsl.Init;
import oit.gl4.wbo.glsl.Opaque;

/**
 *
 * @author gbarbieri
 */
public class WeightedBlendedOpaque {

    private int[] accumulationFboId;
    private int[] accumulationTexId;
    private int[] sampler;
    private Vec2i imageSize;
    private Init init;
    private Final finale;
    private Opaque opaque;
    private FullscreenQuad fullscreenQuad;
    private float weightParameter;
    private int[] opaqueColorTexId;
    private int[] opaqueDepthTexId;
    private int[] opaqueFboId;

    public WeightedBlendedOpaque(GL4 gl4, int blockBinding) {

        initSampler(gl4);

        buildShaders(gl4, blockBinding);

        fullscreenQuad = new FullscreenQuad(gl4);

        weightParameter = .5f;
    }

    public void render(GL4 gl4, Scene scene) {

        /**
         * (1) Initialize Opaque Depth Fbo.
         */
        gl4.glBindFramebuffer(GL4.GL_FRAMEBUFFER, opaqueFboId[0]);
        gl4.glDrawBuffer(GL4.GL_COLOR_ATTACHMENT0);

        gl4.glClearColor(1, 1, 1, 1);
        gl4.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        gl4.glEnable(GL4.GL_DEPTH_TEST);

        opaque.bind(gl4);
        {
            scene.renderWaOpaque(gl4, opaque.getModelToWorldUL());
        }
        opaque.unbind(gl4);
        gl4.glDisable(GL4.GL_DEPTH_TEST);
        /**
         * (2) Geometry Pass.
         */
        gl4.glBindFramebuffer(GL4.GL_FRAMEBUFFER, accumulationFboId[0]);
        gl4.glDrawBuffers(2, new int[]{GL4.GL_COLOR_ATTACHMENT0, GL4.GL_COLOR_ATTACHMENT1}, 0);
        /**
         * Render target 0 stores a sum (weighted RGBA colors), clear it to 0.f
         * Render target 1 stores a product (transmittances), clear it to 1.f.
         */
        float[] clearColorZero = new float[]{0f, 0f, 0f, 0f};
        float[] clearColorOne = new float[]{1f, 1f, 1f, 1f};
        gl4.glClearBufferfv(GL4.GL_COLOR, 0, clearColorZero, 0);
        gl4.glClearBufferfv(GL4.GL_COLOR, 1, clearColorOne, 0);

        gl4.glEnable(GL4.GL_BLEND);
        gl4.glBlendEquation(GL4.GL_FUNC_ADD);
        gl4.glBlendFunci(0, GL4.GL_ONE, GL4.GL_ONE);
        gl4.glBlendFunci(1, GL4.GL_ZERO, GL4.GL_ONE_MINUS_SRC_COLOR);

        init.bind(gl4);
        {
            gl4.glActiveTexture(GL4.GL_TEXTURE0);
            gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0]);
            gl4.glBindSampler(0, sampler[0]);
            {
                gl4.glUniform1f(init.getDepthScaleUL(), weightParameter);
                scene.renderWaTransparent(gl4, init.getModelToWorldUL(), init.getAlphaUL());
            }
            gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, 0);
            gl4.glBindSampler(0, 0);
        }
        init.unbind(gl4);

        gl4.glDisable(GL4.GL_BLEND);
        /**
         * (3) Compositing Pass.
         */
        gl4.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);
        gl4.glDrawBuffer(GL4.GL_BACK);

        finale.bind(gl4);
        {
            gl4.glActiveTexture(GL4.GL_TEXTURE0);
            gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, accumulationTexId[0]);
            gl4.glBindSampler(0, sampler[0]);
            {
                gl4.glActiveTexture(GL4.GL_TEXTURE1);
                gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, accumulationTexId[1]);
                gl4.glBindSampler(1, sampler[0]);
                {
                    gl4.glActiveTexture(GL4.GL_TEXTURE2);
                    gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, opaqueColorTexId[0]);
                    gl4.glBindSampler(2, sampler[0]);
                    {
                        fullscreenQuad.render(gl4);
                    }
                    gl4.glBindSampler(2, 0);
                }
                gl4.glBindSampler(1, 0);
            }
            gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, 0);
            gl4.glBindSampler(0, 0);
        }
        finale.unbind(gl4);
    }

    private void buildShaders(GL4 gl4, int blockBinding) {

        String shadersFilepath = "/oit/gl4/wbo/glsl/shaders/";

        opaque = new Opaque(gl4, shadersFilepath, new String[]{"opaque_VS.glsl", "shade_VS.glsl"},
                new String[]{"opaque_FS.glsl", "shade_FS.glsl"}, blockBinding);

        init = new Init(gl4, shadersFilepath, new String[]{"init_VS.glsl", "shade_VS.glsl"},
                new String[]{"init_FS.glsl", "shade_FS.glsl"}, blockBinding);
        init.bind(gl4);
        {
            gl4.glUniform1i(init.getOpaqueDepthTexUL(), 0);
        }
        init.unbind(gl4);

        Mat4 modelToClip = Jglm.orthographic2D(0, 1, 0, 1);

        finale = new Final(gl4, shadersFilepath, new String[]{"final_VS.glsl"}, new String[]{"final_FS.glsl"});
        finale.bind(gl4);
        {
            gl4.glUniform1i(finale.getColorTex0UL(), 0);
            gl4.glUniform1i(finale.getColorTex1UL(), 1);
            gl4.glUniform1i(finale.getOpaqueColorTexUL(), 2);
            gl4.glUniformMatrix4fv(finale.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        finale.unbind(gl4);
    }

    public void reshape(GL4 gl4, int width, int height) {

        imageSize = new Vec2i(width, height);

        deleteRenderTargets(gl4);
        initRenderTargets(gl4);
    }

    private void initRenderTargets(GL4 gl4) {

        accumulationTexId = new int[2];
        gl4.glGenTextures(2, accumulationTexId, 0);

        gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, accumulationTexId[0]);

        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTexImage2D(GL4.GL_TEXTURE_RECTANGLE, 0, GL4.GL_RGBA16F,
                imageSize.x, imageSize.y, 0, GL4.GL_RGBA, GL4.GL_FLOAT, null);

        gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, accumulationTexId[1]);

        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTexImage2D(GL4.GL_TEXTURE_RECTANGLE, 0, GL4.GL_R8,
                imageSize.x, imageSize.y, 0, GL4.GL_RED, GL4.GL_FLOAT, null);

        accumulationFboId = new int[1];
        gl4.glGenFramebuffers(1, accumulationFboId, 0);

        gl4.glBindFramebuffer(GL4.GL_FRAMEBUFFER, accumulationFboId[0]);

        gl4.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0,
                GL4.GL_TEXTURE_RECTANGLE, accumulationTexId[0], 0);
        gl4.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT1,
                GL4.GL_TEXTURE_RECTANGLE, accumulationTexId[1], 0);
        /**
         * Opaque targets.
         */
        opaqueDepthTexId = new int[1];
        gl4.glGenTextures(1, opaqueDepthTexId, 0);

        gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0]);

        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTexImage2D(GL4.GL_TEXTURE_RECTANGLE, 0, GL4.GL_DEPTH_COMPONENT32F,
                imageSize.x, imageSize.y, 0, GL4.GL_DEPTH_COMPONENT, GL4.GL_FLOAT, null);

        opaqueColorTexId = new int[1];
        gl4.glGenTextures(1, opaqueColorTexId, 0);
        gl4.glBindTexture(GL4.GL_TEXTURE_RECTANGLE, opaqueColorTexId[0]);

        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTexParameteri(GL4.GL_TEXTURE_RECTANGLE, GL4.GL_TEXTURE_MAX_LEVEL, 0);

        gl4.glTexImage2D(GL4.GL_TEXTURE_RECTANGLE, 0, GL4.GL_RGBA,
                imageSize.x, imageSize.y, 0, GL4.GL_RGBA, GL4.GL_FLOAT, null);

        opaqueFboId = new int[1];
        gl4.glGenFramebuffers(1, opaqueFboId, 0);
        gl4.glBindFramebuffer(GL4.GL_FRAMEBUFFER, opaqueFboId[0]);

        gl4.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT,
                GL4.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0], 0);
        gl4.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0,
                GL4.GL_TEXTURE_RECTANGLE, opaqueColorTexId[0], 0);
    }

    private void deleteRenderTargets(GL4 gl4) {

        if (accumulationFboId != null) {
            gl4.glDeleteFramebuffers(accumulationFboId.length, accumulationFboId, 0);
            accumulationFboId = null;
        }
        if (accumulationTexId != null) {
            gl4.glDeleteFramebuffers(accumulationTexId.length, accumulationTexId, 0);
            accumulationTexId = null;
        }
        if (opaqueFboId != null) {
            gl4.glDeleteFramebuffers(opaqueFboId.length, opaqueFboId, 0);
            opaqueFboId = null;
        }
        if (opaqueDepthTexId != null) {
            gl4.glDeleteFramebuffers(opaqueDepthTexId.length, opaqueDepthTexId, 0);
            opaqueDepthTexId = null;
        }
        if (opaqueColorTexId != null) {
            gl4.glDeleteFramebuffers(opaqueColorTexId.length, opaqueColorTexId, 0);
            opaqueColorTexId = null;
        }
    }

    private void initSampler(GL4 gl4) {

        sampler = new int[1];
        gl4.glGenSamplers(1, sampler, 0);

        gl4.glSamplerParameteri(sampler[0], GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(sampler[0], GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(sampler[0], GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        gl4.glSamplerParameteri(sampler[0], GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
    }
}
