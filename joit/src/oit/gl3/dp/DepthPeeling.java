/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dp;

import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.IntBuffer;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec3;
import oit.gl3.FullscreenQuad;
import oit.gl3.Scene;
import oit.gl3.Semantic;
import oit.gl3.Viewer;
import oit.gl3.dp.glsl.Blend;
import oit.gl3.dp.glsl.Final;
import oit.gl3.dp.glsl.Peel;

/**
 *
 * @author gbarbieri
 */
public class DepthPeeling {

    private static final String SHADERS_ROOT = "/oit/gl3/dp/glsl/shaders/";
    private static final String[] SHADERS_SRC = new String[]{"init", "peel", "blend", "final"};
//    private Init init;
    private Peel peel;
    private Blend blend;
    private Final finale;
    private int[] depthTexId;
    private int[] colorTexId;
    private int[] fboId;
    private int[] colorBlenderTexId;
    private int[] colorBlenderFboId;
    private FullscreenQuad fullscreenQuad;
    private int[] queryId;
    public static int numGeoPasses;
    public int numPasses;
    private boolean useOQ;
    private int[] sampler;
    private Vec3 backgroundColor;

    private class Program {

        public final static int INIT = 0;
        public final static int PEEL = 1;
        public final static int BLEND = 2;
        public final static int FINAL = 3;
        public final static int MAX = 4;
    }

    private int[] programName = new int[Program.MAX];
    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(1);

    public DepthPeeling(GL3 gl3) {

        initBuffers(gl3);
        
        initPrograms(gl3);

        initSampler(gl3);

        initQuery(gl3);

        numGeoPasses = 0;
        numPasses = 4;
        useOQ = true;

        backgroundColor = new Vec3(1f, 1f, 1f);

        fullscreenQuad = new FullscreenQuad(gl3);
    }

    private void initBuffers(GL3 gl3) {
        
        gl3.glGenBuffers(1, bufferName);
        
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Float.BYTES, null, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(0));
    }
    
    private void initPrograms(GL3 gl3) {
        
        System.out.print("initPrograms... ");

        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.INIT], "shade"}, "vs", null, null, null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.INIT], "shade"}, "fs", null, null, null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);
            
            programName[Program.INIT] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    shaderProgram.program(), 
                    gl3.glGetUniformBlockIndex(programName[Program.INIT], "Transform0"), 
                    Semantic.Uniform.TRANSFORM0);

            gl3.glUniformBlockBinding(
                    shaderProgram.program(), 
                    gl3.glGetUniformBlockIndex(programName[Program.INIT], "Transform1"), 
                    Semantic.Uniform.TRANSFORM1);

            gl3.glUniformBlockBinding(
                    shaderProgram.program(), 
                    gl3.glGetUniformBlockIndex(programName[Program.INIT], "Parameters"), 
                    Semantic.Uniform.PARAMETERS);
        }
        peel = new Peel(gl3, SHADERS_ROOT, new String[]{"peel_VS.glsl", "shade_VS.glsl"},
                new String[]{"peel_FS.glsl", "shade_FS.glsl"});
        peel.bind(gl3);
        {
            gl3.glUniform1i(peel.getDepthTexUL(), 0);
        }
        peel.unbind(gl3);

        Mat4 modelToClip = Jglm.orthographic2D(0, 1, 0, 1);

        blend = new Blend(gl3, SHADERS_ROOT, "blend_VS.glsl", "blend_FS.glsl");
        blend.bind(gl3);
        {
            gl3.glUniform1i(blend.getTempTexUL(), 0);
            gl3.glUniformMatrix4fv(blend.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        blend.unbind(gl3);

        finale = new Final(gl3, SHADERS_ROOT, "final_VS.glsl", "final_FS.glsl");
        finale.bind(gl3);
        {
            gl3.glUniform1i(finale.getColorTexUL(), 0);
            gl3.glUniformMatrix4fv(finale.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        finale.unbind(gl3);

        System.out.println("ok");
    }

    public void render(GL3 gl3, Scene scene) {

        numGeoPasses = 0;
        /**
         * (1) Initialize Min Depth Buffer.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

        gl3.glClearColor(0, 0, 0, 1);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl3.glEnable(GL3.GL_DEPTH_TEST);

//        init.bind(gl3);
        gl3.glUseProgram(programName[Program.INIT]);
        {
//            scene.render(gl3, modelUL, alphaUL);
            scene.render(gl3, 0);
        }
//        init.unbind(gl3);
        gl3.glUseProgram(0);
        /**
         * (2) Depth Peeling + Blending.
         *
         * numLayers is useful if occlusion queries are disabled. In this case,
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
         * yet the max numLayers
         */
        for (int layer = 1; useOQ || layer < numLayers; layer++) {

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

            peel.bind(gl3);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTexId[prevId]);
                gl3.glBindSampler(0, sampler[0]);
                {
                    scene.render(gl3, peel.getModelToWorldUL(), peel.getAlphaUL());
                }
                gl3.glBindSampler(0, 0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            }
            peel.unbind(gl3);

            if (useOQ) {
                gl3.glEndQuery(GL3.GL_SAMPLES_PASSED);
            }

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
            gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

            gl3.glDisable(GL3.GL_DEPTH_TEST);
            gl3.glEnable(GL3.GL_BLEND);

            gl3.glBlendEquation(GL3.GL_FUNC_ADD);
            gl3.glBlendFuncSeparate(GL3.GL_DST_ALPHA, GL3.GL_ONE, GL3.GL_ZERO, GL3.GL_ONE_MINUS_SRC_ALPHA);

            blend.bind(gl3);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTexId[currId]);
                gl3.glBindSampler(0, sampler[0]);
                {
                    fullscreenQuad.render(gl3);
                }
                gl3.glBindSampler(0, 0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            }
            blend.unbind(gl3);

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

        finale.bind(gl3);
        {
            gl3.glActiveTexture(GL3.GL_TEXTURE0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0]);
            gl3.glBindSampler(0, sampler[0]);
            {
                gl3.glUniform3f(finale.getBackgroundColorUL(), backgroundColor.x, backgroundColor.y, backgroundColor.z);
                {
                    fullscreenQuad.render(gl3);
                }
                gl3.glBindSampler(1, 0);
            }
            gl3.glBindSampler(0, 0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
        }
        finale.unbind(gl3);

//        System.out.println("numGeoPasses " + numGeoPasses);
    }

    public void reshape(GL3 gl3, int width, int height) {

        deleteRenderTargets(gl3);
        initRenderTargets(gl3);
    }

    private void initRenderTargets(GL3 gl3) {
        /**
         * Default Depth Peeling resources.
         */
        depthTexId = new int[2];
        colorTexId = new int[2];
        fboId = new int[2];

        gl3.glGenTextures(depthTexId.length, depthTexId, 0);
        gl3.glGenTextures(colorTexId.length, colorTexId, 0);
        gl3.glGenFramebuffers(fboId.length, fboId, 0);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTexId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_DEPTH_COMPONENT32F,
                    Viewer.imageSize.x, Viewer.imageSize.y, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTexId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                    Viewer.imageSize.x, Viewer.imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[i]);

            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                    GL3.GL_TEXTURE_RECTANGLE, depthTexId[i], 0);
            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                    GL3.GL_TEXTURE_RECTANGLE, colorTexId[i], 0);

            checkBindedFrameBuffer(gl3);
        }
        colorBlenderTexId = new int[1];
        gl3.glGenTextures(1, colorBlenderTexId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                Viewer.imageSize.x, Viewer.imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        colorBlenderFboId = new int[1];
        gl3.glGenFramebuffers(1, colorBlenderFboId, 0);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                GL3.GL_TEXTURE_RECTANGLE, depthTexId[0], 0);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0], 0);

        checkBindedFrameBuffer(gl3);
    }

    private void checkBindedFrameBuffer(GL3 gl3) {

        int frameBufferStatus = gl3.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER);

        if (frameBufferStatus != GL3.GL_FRAMEBUFFER_COMPLETE) {

            System.out.println("FrameBuffer Incomplete!");

            switch (frameBufferStatus) {

                case GL3.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                    System.out.println("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
                    break;

                case GL3.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                    System.out.println("GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
                    break;

                case GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                    System.out.println("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
                    break;

                case GL3.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                    System.out.println("GL_FRAMEBUFFER_INCOMPLETE_FORMATS");
                    break;

                case GL3.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                    System.out.println("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
                    break;

                case GL3.GL_FRAMEBUFFER_UNSUPPORTED:
                    System.out.println("GL_FRAMEBUFFER_UNSUPPORTED");
                    break;
            }
        }
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
    }

    private void initSampler(GL3 gl3) {

        sampler = new int[1];
        gl3.glGenSamplers(1, sampler, 0);

        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
    }

    private void initQuery(GL3 gl3) {

        queryId = new int[1];
        gl3.glGenQueries(1, queryId, 0);
    }
}
