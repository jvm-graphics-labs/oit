/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3Official;

import javax.media.opengl.GL3;
import depthPeeling.dpGl3Official.glsl.Blend;
import depthPeeling.dpGl3Official.glsl.Final;
import depthPeeling.dpGl3Official.glsl.Init;
import depthPeeling.dpGl3Official.glsl.Peel;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec2i;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class DepthPeeling {

    private Init dpInit;
    private Peel dpPeel;
    private Blend dpBlend;
    private Final dpFinal;
    private int[] depthTexId;
    private int[] colorTexId;
    private int[] fboId;
    private int[] colorBlenderTexId;
    private int[] colorBlenderFboId;
    private Vec2i imageSize;
    private float opacity;
    private Vec3 color;
    private FullscreenQuad fullscreenQuad;
    private int[] queryId;
    public static int numGeoPasses;
    public int numPasses;
    private boolean useOQ;
    private Vec3 backgroundColor;

    public DepthPeeling(GL3 gl3, Vec2i imageSize, int blockBinding) {

        this.imageSize = imageSize;

        buildShaders(gl3, blockBinding);

        initRenderTargets(gl3);

        opacity = 0.6f;
        color = new Vec3(.4f, .85f, 0f);
        numGeoPasses = 0;
        numPasses = 4;
        useOQ = true;
        backgroundColor = new Vec3(1f, 1f, 1f);

        fullscreenQuad = new FullscreenQuad(gl3);
    }

    public void render(GL3 gl3, Model model) {
        
        numGeoPasses = 0;
        /**
         * (1) Initialize Min Depth Buffer.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

        gl3.glClearColor(0, 0, 0, 1);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl3.glEnable(GL3.GL_DEPTH_TEST);

        dpInit.bind(gl3);
        {
            gl3.glUniform1f(dpInit.getAlphaUL(), opacity);

            model.render(gl3);
        }
        dpInit.unbind(gl3);
        /**
         * (2) Depth Peeling + Blending.
         */
        int numLayers = (numPasses - 1) * 2;

        for (int layer = 1; useOQ || layer < numLayers; layer++) {
//            System.out.println("layer " + layer);
            int currId = layer % 2;
            int prevId = 1 - currId;

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[currId]);
            gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

            gl3.glClearColor(0, 0, 0, 0);
            gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

            gl3.glDisable(GL3.GL_BLEND);
            gl3.glEnable(GL3.GL_DEPTH_TEST);

            if (useOQ) {
                gl3.glBeginQuery(GL3.GL_SAMPLES_PASSED, queryId[0]);
            }

            dpPeel.bind(gl3);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTexId[prevId]);
                gl3.glUniform1i(dpPeel.getDepthTexUL(), 0);
                {
                    gl3.glUniform1f(dpPeel.getAlphaUL(), opacity);

                    model.render(gl3);
                }
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            }
            dpPeel.unbind(gl3);

            if (useOQ) {
                gl3.glEndQuery(GL3.GL_SAMPLES_PASSED);
            }

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
            gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

            gl3.glDisable(GL3.GL_DEPTH_TEST);
            gl3.glEnable(GL3.GL_BLEND);

            gl3.glBlendEquation(GL3.GL_FUNC_ADD);
            gl3.glBlendFuncSeparate(GL3.GL_DST_ALPHA, GL3.GL_ONE, GL3.GL_ZERO, GL3.GL_ONE_MINUS_SRC_ALPHA);

            dpBlend.bind(gl3);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTexId[currId]);
                gl3.glUniform1i(dpBlend.getTempTexUL(), 0);
                {
                    fullscreenQuad.render(gl3);
                }
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            }
            dpBlend.unbind(gl3);

            gl3.glDisable(GL3.GL_BLEND);

            if (useOQ) {
                int[] samplesCount = new int[1];
                gl3.glGetQueryObjectuiv(queryId[0], GL3.GL_QUERY_RESULT, samplesCount, 0);
                if (samplesCount[0] == 0) {
                    break;
                }
            }
        }
        /**
         * (3) Final Pass.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL3.GL_BACK);
        gl3.glDisable(GL3.GL_DEPTH_TEST);

        dpFinal.bind(gl3);
        {
            gl3.glUniform3f(dpFinal.getBackgroundColorUL(), backgroundColor.x, backgroundColor.y, backgroundColor.z);

            gl3.glActiveTexture(GL3.GL_TEXTURE0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0]);
            gl3.glUniform1i(dpFinal.getColorTexUL(), 0);
            {
                fullscreenQuad.render(gl3);
            }
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
        }
        dpFinal.unbind(gl3);

//        System.out.println("numGeoPasses " + numGeoPasses);
    }

    public void reshape(GL3 gl3, int width, int height) {

        imageSize = new Vec2i(width, height);

        deleteRenderTargets(gl3);
        initRenderTargets(gl3);
    }

    private void buildShaders(GL3 gl3, int blockBinding) {
        System.out.print("buildShaders... ");

        String shadersFilepath = "/depthPeeling/dpGl3Official/glsl/shaders/";

        dpInit = new Init(gl3, shadersFilepath, "dpInit_VS.glsl", "dpInit_FS.glsl", blockBinding);

        dpPeel = new Peel(gl3, shadersFilepath, "dpPeel_VS.glsl", "dpPeel_FS.glsl", blockBinding);

        dpBlend = new Blend(gl3, shadersFilepath, "dpBlend_VS.glsl", "dpBlend_FS.glsl");

        dpFinal = new Final(gl3, shadersFilepath, "dpFinal_VS.glsl", "dpFinal_FS.glsl");

        Mat4 modelToClip = Jglm.orthographic2D(0, 1, 0, 1);

        dpInit.bind(gl3);
        {
            gl3.glUniformMatrix4fv(dpInit.getModelToWorldUL(), 1, false, new Mat4(1).toFloatArray(), 0);
        }
        dpInit.unbind(gl3);

        dpPeel.bind(gl3);
        {
            gl3.glUniformMatrix4fv(dpPeel.getModelToWorldUL(), 1, false, new Mat4(1).toFloatArray(), 0);
        }
        dpPeel.unbind(gl3);

        dpFinal.bind(gl3);
        {
            gl3.glUniformMatrix4fv(dpFinal.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        dpFinal.unbind(gl3);

        dpBlend.bind(gl3);
        {
            gl3.glUniformMatrix4fv(dpBlend.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        dpBlend.unbind(gl3);

        System.out.println("ok");
    }

    private void initRenderTargets(GL3 gl3) {

        depthTexId = new int[2];
        colorTexId = new int[2];
        fboId = new int[2];

        gl3.glGenTextures(depthTexId.length, depthTexId, 0);
        gl3.glGenTextures(colorTexId.length, colorTexId, 0);
        gl3.glGenFramebuffers(fboId.length, fboId, 0);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTexId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_DEPTH_COMPONENT32F,
                    imageSize.x, imageSize.y, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTexId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                    imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);
//
            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[i]);
            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                    GL3.GL_TEXTURE_RECTANGLE, depthTexId[i], 0);
            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                    GL3.GL_TEXTURE_RECTANGLE, colorTexId[i], 0);
        }
        colorBlenderTexId = new int[1];
        gl3.glGenTextures(1, colorBlenderTexId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        colorBlenderFboId = new int[1];
        gl3.glGenFramebuffers(1, colorBlenderFboId, 0);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                GL3.GL_TEXTURE_RECTANGLE, depthTexId[0], 0);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0], 0);

        queryId = new int[1];
        gl3.glGenQueries(1, queryId, 0);
    }

    private void deleteRenderTargets(GL3 gl3) {

        if (fboId != null) {
            gl3.glDeleteFramebuffers(fboId.length, fboId, 0);
            fboId = null;
        }
        if (colorBlenderFboId != null) {
            gl3.glDeleteFramebuffers(colorBlenderFboId.length, colorBlenderFboId, 0);
            colorBlenderFboId = null;
        }
        if (depthTexId != null) {
            gl3.glDeleteTextures(depthTexId.length, depthTexId, 0);
            depthTexId = null;
        }
        if (colorTexId != null) {
            gl3.glDeleteTextures(colorTexId.length, colorTexId, 0);
            colorTexId = null;
        }
        if (colorBlenderTexId != null) {
            gl3.glDeleteTextures(colorBlenderTexId.length, colorBlenderTexId, 0);
            colorBlenderTexId = null;
        }
        if (queryId != null) {
            gl3.glDeleteQueries(queryId.length, queryId, 0);
            queryId = null;
        }
    }
}
