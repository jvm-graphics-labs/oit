/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demos.dualDepthPeeling.ddp;

import com.jogamp.opengl.GL2;
import static com.jogamp.opengl.GL2.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import demos.dualDepthPeeling.BufferUtils;
import demos.dualDepthPeeling.GLSLProgramObject;
import demos.dualDepthPeeling.Model;
import demos.dualDepthPeeling.Semantic;
import demos.dualDepthPeeling.Viewer;
import static demos.dualDepthPeeling.Viewer.MAX_DEPTH;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class DualDeepPeeling {

    private static final String SHADERS_ROOT = "demos/dualDepthPeeling/ddp/shaders/";
    private static final String[] SHADERS_SRC = new String[]{"init", "peel", "blend", "final"};

    private class Program {

        public static final int INIT = 0;
        public static final int PEEL = 1;
        public static final int BLEND = 2;
        public static final int FINAL = 3;
        public static final int MAX = 4;
    }

    private GLSLProgramObject[] programObjects = new GLSLProgramObject[Program.MAX];

    private class Framebuffer {

        public static final int BACK_BLENDER = 0;
        public static final int SINGLE = 1;
        public static final int MAX = 2;
    }

    private class Texture {

        public final static int DEPTH0 = 0;
        public final static int DEPTH1 = 1;
        public final static int FRONT_BLENDER0 = 2;
        public final static int FRONT_BLENDER1 = 3;
        public final static int BACK_TEMP0 = 4;
        public final static int BACK_TEMP1 = 5;
        public final static int BACK_BLENDER = 6;
        public final static int MAX = 7;
    }

    private class Buffer {

        public static final int TRANSFORM0 = 0;
        public static final int TRANSFORM1 = 1;
        public static final int PARAMETERS = 2;
        public static final int MAX = 3;
    }

    private IntBuffer framebufferName = GLBuffers.newDirectIntBuffer(Framebuffer.MAX), textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            samplerName = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private int[] programName = new int[Program.MAX];

    private int[] drawBuffers = new int[]{
        GL_COLOR_ATTACHMENT0,
        GL_COLOR_ATTACHMENT1,
        GL_COLOR_ATTACHMENT2,
        GL_COLOR_ATTACHMENT3,
        GL_COLOR_ATTACHMENT4,
        GL_COLOR_ATTACHMENT5,
        GL_COLOR_ATTACHMENT6};

    private ByteBuffer matBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);

    public DualDeepPeeling(GL3 gl3) {

        buildShaders((GL2) gl3);

        initBuffers(gl3);

        initPrograms(gl3);

        initTargets(gl3);

//        initSampler(gl3);
    }

    private void initBuffers(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE * 2, null, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM0));

        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);
        new Mat4(1).toFb(buffer);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM1));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, buffer, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM1, bufferName.get(Buffer.TRANSFORM1));

        buffer.putFloat(0 * Float.BYTES, 0.6f);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Float.BYTES, buffer, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(Buffer.PARAMETERS));

        BufferUtils.destroyDirectBuffer(buffer);
    }

    private void buildShaders(GL2 gl) {
        System.err.println("\nloading shaders...\n");

        programObjects[Program.INIT] = new GLSLProgramObject();
        programObjects[Program.INIT].attachVertexShader(gl, "dual_peeling_init_vertex.glsl", SHADERS_ROOT);
        programObjects[Program.INIT].attachFragmentShader(gl, "dual_peeling_init_fragment.glsl", SHADERS_ROOT);
        programObjects[Program.INIT].link(gl);

        programObjects[Program.PEEL] = new GLSLProgramObject();
        programObjects[Program.PEEL].attachVertexShader(gl, "shade_vertex.glsl", SHADERS_ROOT);
        programObjects[Program.PEEL].attachVertexShader(gl, "dual_peeling_peel_vertex.glsl", SHADERS_ROOT);
        programObjects[Program.PEEL].attachFragmentShader(gl, "shade_fragment.glsl", SHADERS_ROOT);
        programObjects[Program.PEEL].attachFragmentShader(gl, "dual_peeling_peel_fragment.glsl", SHADERS_ROOT);
        programObjects[Program.PEEL].link(gl);

        programObjects[Program.BLEND] = new GLSLProgramObject();
        programObjects[Program.BLEND].attachVertexShader(gl, "dual_peeling_blend_vertex.glsl", SHADERS_ROOT);
        programObjects[Program.BLEND].attachFragmentShader(gl, "dual_peeling_blend_fragment.glsl", SHADERS_ROOT);
        programObjects[Program.BLEND].link(gl);

        programObjects[Program.FINAL] = new GLSLProgramObject();
        programObjects[Program.FINAL].attachVertexShader(gl, "dual_peeling_final_vertex.glsl", SHADERS_ROOT);
        programObjects[Program.FINAL].attachFragmentShader(gl, "dual_peeling_final_fragment.glsl", SHADERS_ROOT);
        programObjects[Program.FINAL].link(gl);
    }

    private void initPrograms(GL3 gl3) {

        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[Program.INIT], "vs", null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[Program.INIT], "fs", null, true);

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
        }

        {
            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.PEEL], "shade"}, "vs", null, null, null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                    new String[]{SHADERS_SRC[Program.PEEL], "shade"}, "fs", null, null, null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[Program.PEEL] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[Program.PEEL],
                    gl3.glGetUniformBlockIndex(programName[Program.PEEL], "Transform0"),
                    Semantic.Uniform.TRANSFORM0);

            gl3.glUniformBlockBinding(
                    programName[Program.PEEL],
                    gl3.glGetUniformBlockIndex(programName[Program.PEEL], "Transform1"),
                    Semantic.Uniform.TRANSFORM1);

            gl3.glUniformBlockBinding(
                    programName[Program.PEEL],
                    gl3.glGetUniformBlockIndex(programName[Program.PEEL], "Parameters"),
                    Semantic.Uniform.PARAMETERS);

            gl3.glUseProgram(programName[Program.PEEL]);
            gl3.glUniform1i(
                    gl3.glGetUniformLocation(programName[Program.PEEL], "depthBlenderTex"),
                    Semantic.Sampler.DEPTH);
            gl3.glUniform1i(
                    gl3.glGetUniformLocation(programName[Program.PEEL], "frontBlenderTex"),
                    Semantic.Sampler.FRONT_BLENDER);
        }
    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);
        gl3.glGenFramebuffers(Framebuffer.MAX, framebufferName);

        for (int i = 0; i < 2; i++) {
            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH0 + i));
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            //gl.glEnable( GL2.GL_PIXEL_UNPACK_BUFFER );
            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RG32F, Viewer.imageSize.x, Viewer.imageSize.y,
                    0, GL_RG, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.FRONT_BLENDER0 + i));
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA, Viewer.imageSize.x, Viewer.imageSize.y,
                    0, GL_RGBA, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_TEMP0 + i));
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA, Viewer.imageSize.x, Viewer.imageSize.y,
                    0, GL_RGBA, GL_FLOAT, null);
        }

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_BLENDER));
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGB, Viewer.imageSize.x, Viewer.imageSize.y,
                0, GL_RGB, GL_FLOAT, null);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.BACK_BLENDER));
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_BLENDER), 0);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.SINGLE));

        int j = 0;
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH0 + j), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.FRONT_BLENDER0 + j), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_TEMP0 + j), 0);

        j = 1;
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH0 + j), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT4,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.FRONT_BLENDER0 + j), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT5,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_TEMP0 + j), 0);

        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT6,
                GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_BLENDER), 0);
    }

    private void initSampler(GL3 gl3) {

        gl3.glGenSamplers(1, samplerName);

        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    public void render(GL2 gl2, Model model) {

        {
            Mat4 mat = glm.lookAt_(new Vec3(Viewer.pos), new Vec3(Viewer.pos[0], Viewer.pos[1], 0), new Vec3(0, 1, 0));
//            Mat4 mat = glm.lookAt_(new Vec3(0, 0, 2), new Vec3(0), new Vec3(0, 1, 0));
            mat.toFb(matBuffer);
//            System.out.println("view");
//            for (int j = 0; j < 16; j++) {
//                System.out.println("" + matBuffer.getFloat());
//            }
//            matBuffer.rewind();
            gl2.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
            gl2.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, matBuffer);

            glm.perspective((float) Math.toRadians(Viewer.FOVY), (float) Viewer.imageSize.x / Viewer.imageSize.y, Viewer.ZNEAR, Viewer.ZFAR, mat);
            mat.toFb(matBuffer);
//            System.out.println("proj");
//            for (int j = 0; j < 16; j++) {
//                System.out.println("" + matBuffer.getFloat());
//            }
//            matBuffer.rewind();
            gl2.glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE, Mat4.SIZE, matBuffer);

            mat.identity()
                    .rotate((float) Math.toRadians(Viewer.rot[0]), 1, 0, 0)
                    .rotate((float) Math.toRadians(Viewer.rot[1]), 0, 1, 0)
                    .translate(Viewer.trans)
                    .scale(Viewer.scale);
//            mat.identity() 
//                    .rotate((float) Math.toRadians(0f), 1, 0, 0)
                    //                    .rotate((float) Math.toRadians(45f), 0, 1, 0)
                    //                    .translate(0.03f, -0.70f, 0.03f)
                    //                    .scale(6f)
                    ;
            mat.toFb(matBuffer);
//            System.out.println("model");
//            for (int j = 0; j < 16; j++) {
//                System.out.println("" + matBuffer.getFloat());
//            }
//            matBuffer.rewind();
            gl2.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM1));
            gl2.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, matBuffer);
        }

        gl2.glDisable(GL_DEPTH_TEST);
        gl2.glEnable(GL_BLEND);

        // ---------------------------------------------------------------------
        // 1. Initialize Min-Max Depth Buffer
        // ---------------------------------------------------------------------
        gl2.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.SINGLE));

        // Render targets 1 and 2 store the front and back colors
        // Clear to 0.0 and use MAX blending to filter written color
        // At most one front color and one back color can be written every pass
        gl2.glDrawBuffers(2, drawBuffers, 1);
        gl2.glClearColor(0, 0, 0, 0);
        gl2.glClear(GL_COLOR_BUFFER_BIT);

        // Render target 0 stores (-minDepth, maxDepth, alphaMultiplier)
        gl2.glDrawBuffer(drawBuffers[0]);
        gl2.glClearColor(-MAX_DEPTH, -MAX_DEPTH, 0, 0);
        gl2.glClear(GL_COLOR_BUFFER_BIT);
        gl2.glBlendEquation(GL_MAX);

//        gl2.glUseProgram(programName[Program.INIT]);
        programObjects[Program.INIT].bind(gl2);
        model.render(gl2);

        // ---------------------------------------------------------------------
        // 2. Dual Depth Peeling + Blending
        // ---------------------------------------------------------------------
        // Since we cannot blend the back colors in the geometry passes,
        // we use another render target to do the alpha blending
        //glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, g_dualBackBlenderFboId);
        gl2.glDrawBuffer(drawBuffers[6]);
        gl2.glClearColor(Viewer.backgroundColor[0], Viewer.backgroundColor[1], Viewer.backgroundColor[2], 0);
        gl2.glClear(GL_COLOR_BUFFER_BIT);

        int currId = 0;

        for (int pass = 1; Viewer.useOQ || pass < Viewer.numPasses; pass++) {

            currId = pass % 2;
            int prevId = 1 - currId;
            int bufId = currId * 3;

            gl2.glDrawBuffers(2, drawBuffers, bufId + 1);
            gl2.glClearColor(0, 0, 0, 0);
            gl2.glClear(GL_COLOR_BUFFER_BIT);

            gl2.glDrawBuffer(drawBuffers[bufId + 0]);
            gl2.glClearColor(-MAX_DEPTH, -MAX_DEPTH, 0, 0);
            gl2.glClear(GL_COLOR_BUFFER_BIT);

            // Render target 0: RG32F MAX blending
            // Render target 1: RGBA MAX blending
            // Render target 2: RGBA MAX blending
            gl2.glDrawBuffers(3, drawBuffers, bufId + 0);
            gl2.glBlendEquation(GL_MAX);

            programObjects[Program.PEEL].bind(gl2);
            programObjects[Program.PEEL].bindTextureRECT(gl2, "DepthBlenderTex", textureName.get(Texture.DEPTH0 + prevId), 0);
            programObjects[Program.PEEL].bindTextureRECT(gl2, "FrontBlenderTex", textureName.get(Texture.FRONT_BLENDER0 + prevId), 1);
            programObjects[Program.PEEL].setUniform(gl2, "Alpha", Viewer.opacity, 1);
//            gl2.glUseProgram(programName[Program.PEEL]);
//            gl2.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DEPTH);
//            gl2.glBindTexture(GL_TEXTURE_RECTANGLE, g_dualDepthTexId[prevId]);
//            gl2.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.FRONT_BLENDER);
//            gl2.glBindTexture(GL_TEXTURE_RECTANGLE, g_dualFrontBlenderTexId[prevId]);
            model.render(gl2);

//            ByteBuffer buffer = GLBuffers.newDirectByteBuffer(Vec4.SIZE);
//            gl2.glReadBuffer(GL_COLOR_ATTACHMENT0 + bufId + 1);
//            for (int x = 0; x < Viewer.imageSize.x; x++) {
//                for (int y = 0; y < Viewer.imageSize.y; y++) {
//                    gl2.glReadPixels(x, y, 1, 1, GL_RGBA, GL_FLOAT, buffer.rewind());
//                    System.out.println("pixel[" + x + ", " + y + "]: (" + buffer.getFloat() + ", " + buffer.getFloat() + ", " + buffer.getFloat() + ", "
//                            + buffer.getFloat() + ")");
//                }
//            }

            // Full screen pass to alpha-blend the back color
            gl2.glDrawBuffer(drawBuffers[6]);

            gl2.glBlendEquation(GL_FUNC_ADD);
            gl2.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            if (Viewer.useOQ) {
                gl2.glBeginQuery(GL_SAMPLES_PASSED, Viewer.queryId[0]);
            }

            programObjects[Program.BLEND].bind(gl2);
            programObjects[Program.BLEND].bindTextureRECT(gl2, "TempTex", textureName.get(Texture.BACK_TEMP0 + currId), 0);
            gl2.glCallList(Viewer.quadDisplayList);
            programObjects[Program.BLEND].unbind(gl2);

            if (Viewer.useOQ) {
                gl2.glEndQuery(GL_SAMPLES_PASSED);
                int[] sample_count = new int[]{0};
                gl2.glGetQueryObjectuiv(Viewer.queryId[0], GL_QUERY_RESULT, sample_count, 0);
                if (sample_count[0] == 0) {
                    break;
                }
            }
        }

        gl2.glDisable(GL2.GL_BLEND);

        // ---------------------------------------------------------------------
        // 3. Final Pass
        // ---------------------------------------------------------------------
        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl2.glDrawBuffer(GL2.GL_BACK);

        programObjects[Program.FINAL].bind(gl2);
        programObjects[Program.FINAL].bindTextureRECT(gl2, "FrontBlenderTex", textureName.get(Texture.FRONT_BLENDER0 + currId), 1);
        programObjects[Program.FINAL].bindTextureRECT(gl2, "BackBlenderTex", textureName.get(Texture.BACK_BLENDER), 2);
        gl2.glCallList(Viewer.quadDisplayList);
        programObjects[Program.FINAL].unbind(gl2);
    }

    public void reshape(GL3 gl3) {

        deleteTargets(gl3);
        initTargets(gl3);
    }

    //--------------------------------------------------------------------------
    private void deleteTargets(GL3 gl3) {
//        gl3.glDeleteFramebuffers(Framebuffer.MAX, framebufferName);
//        gl3.glDeleteTextures(Texture.MAX, textureName);
    }
}
