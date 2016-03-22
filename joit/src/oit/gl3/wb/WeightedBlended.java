/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.wb;

import com.jogamp.opengl.GL3;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec2i;
import oit.gl3.FullscreenQuad;
import oit.gl3.Scene;
import oit.gl3.wb.glsl.Final;
import oit.gl3.wb.glsl.Init;

/**
 *
 * @author gbarbieri
 */
public class WeightedBlended {

    private int[] accumulationFboId;
    private int[] accumulationTexId;
    private int[] sampler;
    private Vec2i imageSize;
    private Init init;
    private Final finale;
    private FullscreenQuad fullscreenQuad;
    private float weightParameter;

    public WeightedBlended(GL3 gl3, int blockBinding) {

        initSampler(gl3);

        buildShaders(gl3, blockBinding);

        fullscreenQuad = new FullscreenQuad(gl3);
        
        weightParameter = .5f;
    }

    public void render(GL3 gl3, Scene scene) {

        gl3.glDisable(GL3.GL_DEPTH_TEST);
        /**
         * (1) Geometry Pass.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, accumulationFboId[0]);
        gl3.glDrawBuffers(2, new int[]{GL3.GL_COLOR_ATTACHMENT0, GL3.GL_COLOR_ATTACHMENT1}, 0);
        /**
         * Render target 0 stores a sum (weighted RGBA colors), clear it to 0.f
         * Render target 1 stores a product (transmittances), clear it to 1.f.
         */
        float[] clearColorZero = new float[]{0f, 0f, 0f, 1f};
        float[] clearColorOne = new float[]{0f, 1f, 1f, 1f};
        gl3.glClearBufferfv(GL3.GL_COLOR, 0, clearColorZero, 0);
        gl3.glClearBufferfv(GL3.GL_COLOR, 1, clearColorOne, 0);

        gl3.glEnable(GL3.GL_BLEND);
        gl3.glBlendEquation(GL3.GL_FUNC_ADD);
        gl3.glBlendFuncSeparate(GL3.GL_ONE, GL3.GL_ONE, GL3.GL_ZERO, GL3.GL_ONE_MINUS_SRC_COLOR);

        init.bind(gl3);
        {
            gl3.glUniform1f(init.getDepthScaleUL(), weightParameter);
            scene.render(gl3, init.getModelToWorldUL(), init.getAlphaUL());
        }
        init.unbind(gl3);

        gl3.glDisable(GL3.GL_BLEND);
        /**
         * (2) Compositing Pass.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL3.GL_BACK);

        finale.bind(gl3);
        {
            gl3.glUniform3f(finale.getBackgroundColorUL(), .1f, .3f, .7f);

            gl3.glActiveTexture(GL3.GL_TEXTURE0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[0]);
            gl3.glBindSampler(0, sampler[0]);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE1);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[1]);
                gl3.glBindSampler(1, sampler[0]);
                {
                    fullscreenQuad.render(gl3);
                }
                gl3.glBindSampler(1, 0);
            }
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            gl3.glBindSampler(0, 0);
        }
        finale.unbind(gl3);
    }

    private void buildShaders(GL3 gl3, int blockBinding) {

        String shadersFilepath = "/oit/gl3/wb/glsl/shaders/";

        init = new Init(gl3, shadersFilepath, new String[]{"init_VS.glsl", "shade_VS.glsl"},
                new String[]{"init_FS.glsl", "shade_FS.glsl"}, blockBinding);

        Mat4 modelToClip = Jglm.orthographic2D(0, 1, 0, 1);

        finale = new Final(gl3, shadersFilepath, new String[]{"final_VS.glsl"}, new String[]{"final_FS.glsl"});
        finale.bind(gl3);
        {
            gl3.glUniform1i(finale.getColorTex0UL(), 0);
            gl3.glUniform1i(finale.getColorTex1UL(), 1);
            gl3.glUniformMatrix4fv(finale.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        finale.unbind(gl3);
    }

    public void reshape(GL3 gl3, int width, int height) {

        imageSize = new Vec2i(width, height);

        deleteRenderTargets(gl3);
        initRenderTargets(gl3);
    }

    private void initRenderTargets(GL3 gl3) {

        accumulationTexId = new int[2];
        gl3.glGenTextures(2, accumulationTexId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA16F,
                imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[1]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_R16F,
                imageSize.x, imageSize.y, 0, GL3.GL_RED, GL3.GL_FLOAT, null);

        accumulationFboId = new int[1];
        gl3.glGenFramebuffers(1, accumulationFboId, 0);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, accumulationFboId[0]);

        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[0], 0);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT1,
                GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[1], 0);
    }

    private void deleteRenderTargets(GL3 gl3) {

        if (accumulationFboId != null) {

            gl3.glDeleteFramebuffers(accumulationFboId.length, accumulationFboId, 0);
            accumulationFboId = null;
        }

        if (accumulationTexId != null) {

            gl3.glDeleteFramebuffers(accumulationTexId.length, accumulationTexId, 0);
            accumulationTexId = null;
        }
    }

    private void initSampler(GL3 gl3) {

        sampler = new int[1];
        gl3.glGenSamplers(1, sampler, 0);

        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
    }
}