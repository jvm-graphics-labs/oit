/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ddp;

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
public class DualDepthPeeling extends OIT {

    private static final String SHADERS_ROOT = "/oit/gl3/ddp/shaders/";
    private static final String[] SHADERS_SRC = new String[]{"init", "peel", "blend", "final"};
    private static final float MAX_DEPTH = 1f;

    private class Program {

        public final static int INIT = 0;
        public final static int PEEL = 1;
        public final static int BLEND = 2;
        public final static int FINAL = 3;
        public final static int MAX = 4;
    }

    private class Texture {

        public final static int DEPTH0 = 0;
        public final static int DEPTH1 = 1;
        public final static int FRONT_BLENDER0 = 2;
        public final static int FRONT_BLENDER1 = 3;
        public final static int BACK_TEMP0 = 4;
        public final static int BACK_TEMP1 = 5;
        public final static int MAX = 6;
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(1);

    @Override
    public void init(GL3 gl3) {

        initPrograms(gl3);

        initTargets(gl3);
    }

    private void initPrograms(GL3 gl3) {

        for (int program = Program.INIT; program <= Program.PEEL; program++) {

            ShaderCode vertShader = (program == Program.INIT) ? ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(),
                    SHADERS_ROOT, null, SHADERS_SRC[program], "vert", null, true)
                    : ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT,
                            new String[]{SHADERS_SRC[program], "shade"}, "vert", null, null, null, true);;
            ShaderCode fragShader = (program == Program.INIT) ? ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(),
                    SHADERS_ROOT, null, SHADERS_SRC[program], "frag", null, true)
                    : ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT,
                            new String[]{SHADERS_SRC[program], "shade"}, "frag", null, null, null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[program] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Transform0"),
                    Semantic.Uniform.TRANSFORM0);

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Transform1"),
                    Semantic.Uniform.TRANSFORM1);
        }
        gl3.glUseProgram(programName[Program.INIT]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.INIT], "opaqueDepthTex"),
                Semantic.Sampler.OPAQUE_DEPTH);

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

        for (int program = Program.BLEND; program <= Program.FINAL; program++) {

            ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[program], "vert", null, true);
            ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SRC[program], "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl3, System.out);

            programName[program] = shaderProgram.program();

            gl3.glUniformBlockBinding(
                    programName[program],
                    gl3.glGetUniformBlockIndex(programName[program], "Transform2"),
                    Semantic.Uniform.TRANSFORM2);
        }
        gl3.glUseProgram(programName[Program.BLEND]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.BLEND], "tempTex"),
                Semantic.Sampler.BACK_TEMP);

        gl3.glUseProgram(programName[Program.FINAL]);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.FINAL], "frontBlenderTex"),
                Semantic.Sampler.FRONT_BLENDER);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(programName[Program.FINAL], "backBlenderTex"),
                Semantic.Sampler.OPAQUE_COLOR);
    }

    @Override
    public void render(GL3 gl3, Scene scene) {

        gl3.glDisable(GL_DEPTH_TEST);
        gl3.glEnable(GL_BLEND);

        /**
         * (1) Initialize Min-Max Depth Buffer.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));

        /**
         * Render targets 1 and 2 store the front and back colors
         * Clear to 0.0 and use MAX blending to filter written color
         * At most one front color and one back color can be written every pass.
         */
        drawBuffers.position(1);
        gl3.glDrawBuffers(2, drawBuffers);
        gl3.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 0));
        gl3.glClearBufferfv(GL_COLOR, 1, Resources.clearColor);
        
        /**
         * Render target 0 stores (-minDepth, maxDepth).
         */
        gl3.glDrawBuffer(drawBuffers.get(0));
        gl3.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, -MAX_DEPTH).put(1, -MAX_DEPTH));
        gl3.glBlendEquation(GL_MAX);

        if (Resources.useOQ) {
            gl3.glBeginQuery(GL_SAMPLES_PASSED, queryName.get(0));
        }

        gl3.glUseProgram(programName[Program.INIT]);
        bindRectTex(gl3, Viewer.textureName.get(Viewer.Texture.DEPTH), Semantic.Sampler.OPAQUE_DEPTH);
        scene.renderTransparent(gl3);

        boolean occluded = false;

        if (Resources.useOQ) {
            gl3.glEndQuery(GL_SAMPLES_PASSED);
            gl3.glGetQueryObjectuiv(queryName.get(0), GL_QUERY_RESULT, samplesCount);
            occluded = samplesCount.get(0) == 0;
        }

        /**
         * (2) Dual Depth Peeling + Blending
         *
         * Since we cannot blend the back colors in the geometry passes,
         * we use another render target to do the alpha blending.
         */
        gl3.glColorMask(false, false, false, true);
        gl3.glClearBufferfv(GL_COLOR, 6, Resources.clearColor.put(3, 0));
        gl3.glColorMask(true, true, true, true);

        int currId = 0;

        if (!occluded) {

            for (int pass = 1; Resources.useOQ || pass < Resources.numPasses; pass++) {

                currId = pass % 2;
                int prevId = 1 - currId;
                int buffId = currId * 3;

                drawBuffers.position(buffId + 1);
                gl3.glDrawBuffers(2, drawBuffers);
                gl3.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 0));
                gl3.glClearBufferfv(GL_COLOR, 1, Resources.clearColor);

                gl3.glDrawBuffer(drawBuffers.get(buffId + 0));
                gl3.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, -MAX_DEPTH).put(1, -MAX_DEPTH));

                /**
                 * Render target 0: RG32F MAX blending
                 * Render target 1: RGBA MAX blending
                 * Render target 2: RGBA MAX blending.
                 */
                drawBuffers.position(buffId + 0);
                gl3.glDrawBuffers(3, drawBuffers);
                gl3.glBlendEquation(GL_MAX);

                gl3.glUseProgram(programName[Program.PEEL]);
                bindRectTex(gl3, textureName.get(Texture.DEPTH0 + prevId), Semantic.Sampler.DEPTH);
                bindRectTex(gl3, textureName.get(Texture.FRONT_BLENDER0 + prevId), Semantic.Sampler.FRONT_BLENDER);

                scene.renderTransparent(gl3);

                /**
                 * Full screen pass to alpha-blend the back color.
                 */
                gl3.glDrawBuffer(drawBuffers.get(6));

                gl3.glBlendEquation(GL_FUNC_ADD);
                gl3.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                if (Resources.useOQ) {
                    gl3.glBeginQuery(GL_SAMPLES_PASSED, queryName.get(0));
                }

                gl3.glUseProgram(programName[Program.BLEND]);
                bindRectTex(gl3, textureName.get(Texture.BACK_TEMP0 + currId), Semantic.Sampler.BACK_TEMP);
                Viewer.fullscreenQuad.render(gl3);

                if (Resources.useOQ) {
                    gl3.glEndQuery(GL_SAMPLES_PASSED);
                    gl3.glGetQueryObjectuiv(queryName.get(0), GL_QUERY_RESULT, samplesCount);
                    if (samplesCount.get(0) == 0) {
                        break;
                    }
                }
            }
        }

        gl3.glDisable(GL_BLEND);

        /**
         * (3) Final Pass.
         */
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL_BACK);

        gl3.glUseProgram(programName[Program.FINAL]);
        bindRectTex(gl3, textureName.get(Texture.FRONT_BLENDER0 + currId), Semantic.Sampler.FRONT_BLENDER);
        bindRectTex(gl3, Viewer.textureName.get(Viewer.Texture.COLOR), Semantic.Sampler.OPAQUE_COLOR);

        Viewer.fullscreenQuad.render(gl3);
    }

    @Override
    public void reshape(GL3 gl3) {
        deleteTargets(gl3);
        initTargets(gl3);
    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);
        gl3.glGenFramebuffers(1, framebufferName);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RG32F, Resources.imageSize.x, Resources.imageSize.y, 0,
                    GL_RG, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.FRONT_BLENDER0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Resources.imageSize.x, Resources.imageSize.y, 0,
                    GL_RGBA, GL_FLOAT, null);

            gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.BACK_TEMP0 + i));

            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Resources.imageSize.x, Resources.imageSize.y, 0,
                    GL_RGBA, GL_FLOAT, null);
        }

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        for (int i = 0; i < 2; i++) {
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + 3 * i, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.DEPTH0 + i), 0);
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1 + 3 * i, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.FRONT_BLENDER0 + i), 0);
            gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2 + 3 * i, GL_TEXTURE_RECTANGLE,
                    textureName.get(Texture.BACK_TEMP0 + i), 0);
        }
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT6, GL_TEXTURE_RECTANGLE,
                Viewer.textureName.get(Viewer.Texture.COLOR), 0);
    }

    private void deleteTargets(GL3 gl3) {
        gl3.glDeleteTextures(Texture.MAX, textureName);
        gl3.glDeleteFramebuffers(1, framebufferName);
    }

    @Override
    public void dispose(GL3 gl3) {
        deleteTargets(gl3);
    }
}
