package demos.dualDepthPeeling;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3bc;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;
import demos.dualDepthPeeling.ddp.DualDeepPeeling;
import glm.vec._2.i.Vec2i;

// Translated from C++ Version see below:
//--------------------------------------------------------------------------------------
// Order Independent Transparency with Dual Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Depth peeling is traditionally used to perform order independent transparency (OIT)
// with N geometry passes for N transparency layers. Dual depth peeling enables peeling
// N transparency layers in N/2+1 passes, by peeling from the front and the back
// simultaneously using a min-max depth buffer. This sample performs either normal or
// dual depth peeling and blends on the fly.
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------
public class Viewer implements GLEventListener {

    private final String MODEL_FILENAME = "demos/dualDepthPeeling/media/models/dragon.obj";

    private class Oit {

        public final static int DUAL_DEPTH_PEELING = 0;
        public final static int DEPTH_PEELING = 1;
        public final static int WEIGHTED_AVERAGE = 2;
        public final static int WEIGHTED_SUM = 3;
    }

    private DualDeepPeeling dualDeepPeeling;

    public final static float FOVY = 30.0f;
    public final static float ZNEAR = 0.0001f;
    public final static float ZFAR = 10.0f;
    public final static float FPS_TIME_WINDOW = 1;
    public final static float MAX_DEPTH = 1.0f;

    public static int numPasses = 4;
    public static Vec2i imageSize = new Vec2i(1024, 768);

    public Model model;
    public static int quadDisplayList;

    public static boolean useOQ = true;
    public static int[] queryId = new int[1];

    public GLSLProgramObject g_shaderFrontInit;
    public GLSLProgramObject g_shaderFrontPeel;
    public GLSLProgramObject g_shaderFrontBlend;
    public GLSLProgramObject g_shaderFrontFinal;

    public GLSLProgramObject g_shaderAverageInit;
    public GLSLProgramObject g_shaderAverageFinal;

    public GLSLProgramObject g_shaderWeightedSumInit;
    public GLSLProgramObject g_shaderWeightedSumFinal;

    public static float[] opacity = new float[]{0.6f};
    public int oit = Oit.DUAL_DEPTH_PEELING;
    public static boolean showOsd = true;
    public static boolean showUI = true;
    public static int g_numGeoPasses = 0;

    public static float scale = 1.0f;
    public static float[] trans = new float[]{0.0f, 0.0f, 0.0f};
    public static float[] rot = new float[]{0.0f, 45.0f};
    public static float[] pos = new float[]{0.0f, 0.0f, 2.0f};

    public static float[] g_white = new float[]{1.0f, 1.0f, 1.0f};
    public static float[] g_black = new float[]{0.0f};
    public static float[] backgroundColor = g_white;

    public int[] g_dualBackBlenderFboId = new int[1];
    public int[] g_dualPeelingSingleFboId = new int[1];
    public int[] g_dualDepthTexId = new int[2];
    public int[] g_dualFrontBlenderTexId = new int[2];
    public int[] g_dualBackTempTexId = new int[2];
    public int[] g_dualBackBlenderTexId = new int[1];

    public int[] g_frontFboId = new int[2];
    public int[] g_frontDepthTexId = new int[2];
    public int[] g_frontColorTexId = new int[2];
    public int[] g_frontColorBlenderTexId = new int[1];
    public int[] g_frontColorBlenderFboId = new int[1];

    public int[] g_accumulationTexId = new int[2];
    public int[] g_accumulationFboId = new int[1];

    int g_drawBuffers[] = {GL2.GL_COLOR_ATTACHMENT0,
        GL2.GL_COLOR_ATTACHMENT1,
        GL2.GL_COLOR_ATTACHMENT2,
        GL2.GL_COLOR_ATTACHMENT3,
        GL2.GL_COLOR_ATTACHMENT4,
        GL2.GL_COLOR_ATTACHMENT5,
        GL2.GL_COLOR_ATTACHMENT6
    };

    //--------------------------------------------------------------------------
    void InitFrontPeelingRenderTargets(GL3bc gl) {
        gl.glGenTextures(2, g_frontDepthTexId, 0);
        gl.glGenTextures(2, g_frontColorTexId, 0);
        gl.glGenFramebuffers(2, g_frontFboId, 0);

        for (int i = 0; i < 2; i++) {
            gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontDepthTexId[i]);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
            gl.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_DEPTH_COMPONENT32F,
                    imageSize.x, imageSize.y, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, null);

            gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorTexId[i]);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
            gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
            gl.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_RGBA, imageSize.x, imageSize.y,
                    0, GL2.GL_RGBA, GL2.GL_FLOAT, null);

            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_frontFboId[i]);
            gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
                    GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontDepthTexId[i], 0);
            gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
                    GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorTexId[i], 0);
        }

        gl.glGenTextures(1, g_frontColorBlenderTexId, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorBlenderTexId[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_RGBA, imageSize.x, imageSize.y,
                0, GL2.GL_RGBA, GL2.GL_FLOAT, null);

        gl.glGenFramebuffers(1, g_frontColorBlenderFboId, 0);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_frontColorBlenderFboId[0]);
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
                GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontDepthTexId[0], 0);
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
                GL2.GL_TEXTURE_RECTANGLE_ARB, g_frontColorBlenderTexId[0], 0);
    }

    //--------------------------------------------------------------------------
    void DeleteFrontPeelingRenderTargets(GL3bc gl) {
        gl.glDeleteFramebuffers(2, g_frontFboId, 0);
        gl.glDeleteFramebuffers(1, g_frontColorBlenderFboId, 0);
        gl.glDeleteTextures(2, g_frontDepthTexId, 0);
        gl.glDeleteTextures(2, g_frontColorTexId, 0);
        gl.glDeleteTextures(1, g_frontColorBlenderTexId, 0);
    }

    //--------------------------------------------------------------------------
    void InitAccumulationRenderTargets(GL3bc gl) {
        gl.glGenTextures(2, g_accumulationTexId, 0);

        gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[0]);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_RGBA16F,
                imageSize.x, imageSize.y, 0, GL2.GL_RGBA, GL2.GL_FLOAT, null);

        gl.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[1]);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_FLOAT_R32_NV,
                imageSize.x, imageSize.y, 0, GL2.GL_RGBA, GL2.GL_FLOAT, null);

        //gl.glTexImage2D( GL2.GL_TEXTURE_RECTANGLE_ARB, 0,  GL2.GL_RGBA16F,
        //              g_imageWidth, g_imageHeight, 0,  GL2.GL_RGBA,  GL2.GL_FLOAT, null);
        gl.glGenFramebuffers(1, g_accumulationFboId, 0);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_accumulationFboId[0]);
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
                GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[0], 0);
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT1,
                GL2.GL_TEXTURE_RECTANGLE_ARB, g_accumulationTexId[1], 0);

    }

    //--------------------------------------------------------------------------
    void DeleteAccumulationRenderTargets(GL3bc gl) {
        gl.glDeleteFramebuffers(1, g_accumulationFboId, 0);
        gl.glDeleteTextures(2, g_accumulationTexId, 0);
    }

    //--------------------------------------------------------------------------
    void MakeFullScreenQuad(GL3bc gl) {
        GLU glu = GLU.createGLU(gl);

        quadDisplayList = gl.glGenLists(1);
        gl.glNewList(quadDisplayList, GL2.GL_COMPILE);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0f, 1.0f, 0.0f, 1.0f);
        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glVertex2f(0.0f, 0.0f);
            gl.glVertex2f(1.0f, 0.0f);
            gl.glVertex2f(1.0f, 1.0f);
            gl.glVertex2f(0.0f, 1.0f);
        }
        gl.glEnd();
        gl.glPopMatrix();

        gl.glEndList();
    }

    //--------------------------------------------------------------------------
    void BuildShaders(GL3bc gl) {
        System.err.println("\nloading shaders...\n");

        String allOthers = "demos/dualDepthPeeling/shaders/";

        g_shaderFrontInit = new GLSLProgramObject();
        g_shaderFrontInit.attachVertexShader(gl, "shade_vertex.glsl", allOthers);
        g_shaderFrontInit.attachVertexShader(gl, "front_peeling_init_vertex.glsl", allOthers);
        g_shaderFrontInit.attachFragmentShader(gl, "shade_fragment.glsl", allOthers);
        g_shaderFrontInit.attachFragmentShader(gl, "front_peeling_init_fragment.glsl", allOthers);
        g_shaderFrontInit.link(gl);

        g_shaderFrontPeel = new GLSLProgramObject();
        g_shaderFrontPeel.attachVertexShader(gl, "shade_vertex.glsl", allOthers);
        g_shaderFrontPeel.attachVertexShader(gl, "front_peeling_peel_vertex.glsl", allOthers);
        g_shaderFrontPeel.attachFragmentShader(gl, "shade_fragment.glsl", allOthers);
        g_shaderFrontPeel.attachFragmentShader(gl, "front_peeling_peel_fragment.glsl", allOthers);
        g_shaderFrontPeel.link(gl);

        g_shaderFrontBlend = new GLSLProgramObject();
        g_shaderFrontBlend.attachVertexShader(gl, "front_peeling_blend_vertex.glsl", allOthers);
        g_shaderFrontBlend.attachFragmentShader(gl, "front_peeling_blend_fragment.glsl", allOthers);
        g_shaderFrontBlend.link(gl);

        g_shaderFrontFinal = new GLSLProgramObject();
        g_shaderFrontFinal.attachVertexShader(gl, "front_peeling_final_vertex.glsl", allOthers);
        g_shaderFrontFinal.attachFragmentShader(gl, "front_peeling_final_fragment.glsl", allOthers);
        g_shaderFrontFinal.link(gl);

        g_shaderAverageInit = new GLSLProgramObject();
        g_shaderAverageInit.attachVertexShader(gl, "shade_vertex.glsl", allOthers);
        g_shaderAverageInit.attachVertexShader(gl, "wavg_init_vertex.glsl", allOthers);
        g_shaderAverageInit.attachFragmentShader(gl, "shade_fragment.glsl", allOthers);
        g_shaderAverageInit.attachFragmentShader(gl, "wavg_init_fragment.glsl", allOthers);
        g_shaderAverageInit.link(gl);

        g_shaderAverageFinal = new GLSLProgramObject();
        g_shaderAverageFinal.attachVertexShader(gl, "wavg_final_vertex.glsl", allOthers);
        g_shaderAverageFinal.attachFragmentShader(gl, "wavg_final_fragment.glsl", allOthers);
        g_shaderAverageFinal.link(gl);

        g_shaderWeightedSumInit = new GLSLProgramObject();
        g_shaderWeightedSumInit.attachVertexShader(gl, "shade_vertex.glsl", allOthers);
        g_shaderWeightedSumInit.attachVertexShader(gl, "wsum_init_vertex.glsl", allOthers);
        g_shaderWeightedSumInit.attachFragmentShader(gl, "shade_fragment.glsl", allOthers);
        g_shaderWeightedSumInit.attachFragmentShader(gl, "wsum_init_fragment.glsl", allOthers);
        g_shaderWeightedSumInit.link(gl);

        g_shaderWeightedSumFinal = new GLSLProgramObject();
        g_shaderWeightedSumFinal.attachVertexShader(gl, "wsum_final_vertex.glsl", allOthers);
        g_shaderWeightedSumFinal.attachFragmentShader(gl, "wsum_final_fragment.glsl", allOthers);
        g_shaderWeightedSumFinal.link(gl);
    }

    //--------------------------------------------------------------------------
    void DestroyShaders(GL3bc gl) {

        g_shaderFrontInit.destroy(gl);
        g_shaderFrontPeel.destroy(gl);
        g_shaderFrontBlend.destroy(gl);
        g_shaderFrontFinal.destroy(gl);

        g_shaderAverageInit.destroy(gl);
        g_shaderAverageFinal.destroy(gl);

        g_shaderWeightedSumInit.destroy(gl);
        g_shaderWeightedSumFinal.destroy(gl);
    }

    //--------------------------------------------------------------------------
    void ReloadShaders(GL3bc gl) {
        DestroyShaders(gl);
        BuildShaders(gl);
    }

    private static GLWindow glWindow;
    public static Animator animator;

    //--------------------------------------------------------------------------
    void RenderFrontToBackPeeling(GL3bc gl) {
        // ---------------------------------------------------------------------
        // 1. Initialize Min Depth Buffer
        // ---------------------------------------------------------------------

        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_frontColorBlenderFboId[0]);
        gl.glDrawBuffer(g_drawBuffers[0]);

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GL2.GL_DEPTH_TEST);

        g_shaderFrontInit.bind(gl);
        g_shaderFrontInit.setUniform(gl, "Alpha", opacity, 1);
        model.render(gl);
        g_shaderFrontInit.unbind(gl);

        // ---------------------------------------------------------------------
        // 2. Depth Peeling + Blending
        // ---------------------------------------------------------------------
        int numLayers = (numPasses - 1) * 2;
        for (int layer = 1; useOQ || layer < numLayers; layer++) {
            int currId = layer % 2;
            int prevId = 1 - currId;

            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_frontFboId[currId]);
            gl.glDrawBuffer(g_drawBuffers[0]);

            gl.glClearColor(0, 0, 0, 0);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

            gl.glDisable(GL2.GL_BLEND);
            gl.glEnable(GL2.GL_DEPTH_TEST);

            if (useOQ) {
                gl.glBeginQuery(GL2.GL_SAMPLES_PASSED, queryId[0]);
            }

            g_shaderFrontPeel.bind(gl);
            g_shaderFrontPeel.bindTextureRECT(gl, "DepthTex", g_frontDepthTexId[prevId], 0);
            g_shaderFrontPeel.setUniform(gl, "Alpha", opacity, 1);
            model.render(gl);
            g_shaderFrontPeel.unbind(gl);

            if (useOQ) {
                gl.glEndQuery(GL2.GL_SAMPLES_PASSED);
            }

            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_frontColorBlenderFboId[0]);
            gl.glDrawBuffer(g_drawBuffers[0]);

            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glEnable(GL2.GL_BLEND);

            gl.glBlendEquation(GL2.GL_FUNC_ADD);
            gl.glBlendFuncSeparate(GL2.GL_DST_ALPHA, GL2.GL_ONE,
                    GL2.GL_ZERO, GL2.GL_ONE_MINUS_SRC_ALPHA);

            g_shaderFrontBlend.bind(gl);
            g_shaderFrontBlend.bindTextureRECT(gl, "TempTex", g_frontColorTexId[currId], 0);
            gl.glCallList(quadDisplayList);
            g_shaderFrontBlend.unbind(gl);

            gl.glDisable(GL2.GL_BLEND);

            if (useOQ) {
                int[] sample_count = new int[]{0};
                gl.glGetQueryObjectuiv(queryId[0], GL2.GL_QUERY_RESULT, sample_count, 0);
                if (sample_count[0] == 0) {
                    break;
                }
            }
        }

        // ---------------------------------------------------------------------
        // 3. Final Pass
        // ---------------------------------------------------------------------
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl.glDrawBuffer(GL2.GL_BACK);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        g_shaderFrontFinal.bind(gl);
        g_shaderFrontFinal.setUniform(gl, "BackgroundColor", backgroundColor, 3);
        g_shaderFrontFinal.bindTextureRECT(gl, "ColorTex", g_frontColorBlenderTexId[0], 0);
        gl.glCallList(quadDisplayList);
        g_shaderFrontFinal.unbind(gl);
    }

    //--------------------------------------------------------------------------
    void RenderAverageColors(GL3bc gl) {
        gl.glDisable(GL2.GL_DEPTH_TEST);

        // ---------------------------------------------------------------------
        // 1. Accumulate Colors and Depth Complexity
        // ---------------------------------------------------------------------
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_accumulationFboId[0]);
        gl.glDrawBuffers(2, g_drawBuffers, 0);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);
        gl.glEnable(GL2.GL_BLEND);

        g_shaderAverageInit.bind(gl);
        g_shaderAverageInit.setUniform(gl, "Alpha", opacity, 1);
        model.render(gl);
        g_shaderAverageInit.unbind(gl);

        gl.glDisable(GL2.GL_BLEND);

        // ---------------------------------------------------------------------
        // 2. Approximate Blending
        // ---------------------------------------------------------------------
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl.glDrawBuffer(GL2.GL_BACK);

        g_shaderAverageFinal.bind(gl);
        g_shaderAverageFinal.setUniform(gl, "BackgroundColor", backgroundColor, 3);
        g_shaderAverageFinal.bindTextureRECT(gl, "ColorTex0", g_accumulationTexId[0], 0);
        g_shaderAverageFinal.bindTextureRECT(gl, "ColorTex1", g_accumulationTexId[1], 1);
        gl.glCallList(quadDisplayList);
        g_shaderAverageFinal.unbind(gl);
    }

    //--------------------------------------------------------------------------
    void RenderWeightedSum(GL3bc gl) {
        gl.glDisable(GL2.GL_DEPTH_TEST);

        // ---------------------------------------------------------------------
        // 1. Accumulate (alpha * color) and (alpha)
        // ---------------------------------------------------------------------
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, g_accumulationFboId[0]);
        gl.glDrawBuffer(g_drawBuffers[0]);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);
        gl.glEnable(GL2.GL_BLEND);

        g_shaderWeightedSumInit.bind(gl);
        g_shaderWeightedSumInit.setUniform(gl, "Alpha", opacity, 1);
        model.render(gl);
        g_shaderWeightedSumInit.unbind(gl);

        gl.glDisable(GL2.GL_BLEND);

        // ---------------------------------------------------------------------
        // 2. Weighted Sum
        // ---------------------------------------------------------------------
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl.glDrawBuffer(GL2.GL_BACK);

        g_shaderWeightedSumFinal.bind(gl);
        g_shaderWeightedSumFinal.setUniform(gl, "BackgroundColor", backgroundColor, 3);
        g_shaderWeightedSumFinal.bindTextureRECT(gl, "ColorTex", g_accumulationTexId[0], 0);
        gl.glCallList(quadDisplayList);
        g_shaderWeightedSumFinal.unbind(gl);
    }

    public static void main(String[] args) {
        System.err.println("You may need to run your program with extended memory (using vm argument -Xmx1024m");
        System.out.println("dual_depth_peeling - sample comparing multiple order independent transparency techniques\n");
        System.out.println("  Commands:\n");
        System.out.println("     A/D       - Change uniform opacity\n");
        System.out.println("     1         - Dual peeling mode\n");
        System.out.println("     2         - Front to back peeling mode\n");
        System.out.println("     3         - Weighted average mode\n");
        System.out.println("     4         - Weighted sum mode\n");
        System.out.println("     R         - Reload all shaders\n");
        System.out.println("     B         - Change background color\n");
        System.out.println("     Q         - Toggle occlusion queries\n");
        System.out.println("     +/-       - Change number of geometry passes\n\n");

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL3bc);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(imageSize.x, imageSize.y);
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(false);
        glWindow.setAlwaysOnTop(false);
        glWindow.setFullscreen(false);
        glWindow.setPointerVisible(true);
        glWindow.confinePointer(false);
        glWindow.setTitle("Order Independent Transparency");

        Viewer viewer = new Viewer();
        glWindow.addGLEventListener(viewer);
        InputListener inputListener = new InputListener();
        glWindow.addMouseListener(inputListener);
        glWindow.addKeyListener(inputListener);

        animator = new Animator(glWindow);
        animator.start();

        glWindow.setVisible(true);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL3bc gl = drawable.getGL().getGL3bc();

        GLU glu = GLU.createGLU(gl);

        g_numGeoPasses = 0;

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(pos[0], pos[1], pos[2], pos[0], pos[1], 0, 0, 1, 0);
        gl.glRotatef(rot[0], 1, 0, 0);
        gl.glRotatef(rot[1], 0, 1, 0);
        gl.glTranslatef(trans[0], trans[1], trans[2]);
        gl.glScalef(scale, scale, scale);

//        switch (oit) {
//            case Oit.DUAL_DEPTH_PEELING:
//                RenderDualPeeling(gl2);
//                break;
//            case Oit.DEPTH_PEELING:
//                RenderFrontToBackPeeling(gl2);
//                break;
//            case Oit.WEIGHTED_AVERAGE:
//                RenderAverageColors(gl2);
//                break;
//            case Oit.WEIGHTED_SUM:
//                RenderWeightedSum(gl2);
//                break;
//        }
        dualDeepPeeling.render(gl, model);

        /* Call swapBuffers to render on-screen: */
        drawable.swapBuffers();
    }

    @Override
    public void dispose(GLAutoDrawable arg0) {
        // TODO Auto-generated method stub
        if (animator.isAnimating()) {
            animator.stop();
        }
        System.exit(0);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.err.println("init");

        GL3bc gl = drawable.getGL().getGL3bc();

//        m_kCanvas.setAutoSwapBufferMode(false);
        drawable.setAutoSwapBufferMode(false);

        InitFrontPeelingRenderTargets(gl);
        InitAccumulationRenderTargets(gl);

        dualDeepPeeling = new DualDeepPeeling(drawable.getGL().getGL3());

        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        BuildShaders(gl);
        model = new Model(gl, MODEL_FILENAME);
        MakeFullScreenQuad(gl);

        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_NORMALIZE);
        
        gl.glGenQueries(1, queryId, 0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL3bc gl = drawable.getGL().getGL3bc();

        if (imageSize.x != width || imageSize.y != height) {
            imageSize.set(width, height);

            dualDeepPeeling.reshape(drawable.getGL().getGL3());

            DeleteFrontPeelingRenderTargets(gl);
            InitFrontPeelingRenderTargets(gl);

            DeleteAccumulationRenderTargets(gl);
            InitAccumulationRenderTargets(gl);
        }

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU glu = GLU.createGLU(gl);
        glu.gluPerspective(FOVY, (float) imageSize.x / imageSize.y, ZNEAR, ZFAR);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glViewport(0, 0, imageSize.x, imageSize.y);

    }
}
