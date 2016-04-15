/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.ab;

import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.vec._4.Vec4;
import java.nio.IntBuffer;
import oit.framework.Resources;
import oit.gl4.Scene;
import oit.gl4.Semantic;

/**
 *
 * @author GBarbieri
 */
public class ABuffer {

    private static final String SHADERS_ROOT = "/oit/gl4/ab/shaders/";
    private static final String[] VS_SRC = new String[]{"render", "passThrough", "passThrough"};
    private static final String[] FS_SRC = new String[]{"render", "display", "clear"};
    private static final int ABUFFER_SIZE = 16;

    private final boolean useABuffer = false;
    private final boolean abufferUseSorting = true;
    private final boolean resolveAlphaCorrection = true;
    private final boolean resolveGelly = false;
    private final boolean abufferUseTextures = true;

    private class Program {

        public static final int RENDER = 0;
        public static final int DISPLAY = 1;
        public static final int CLEAR = 2;
        public static final int MAX = 3;
    }

    private class Texture {

        public static final int ABUFFER = 0;
        public static final int ABUFFER_COUNTER = 1;
        public static final int MAX = 2;
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private Vec4 backgroundColor = new Vec4(1.0f);
    private FullscreenQuad fullscreenQuad;

    public ABuffer(GL4 gl4) {

        initPrograms(gl4);

        initTargets(gl4);

        fullscreenQuad = new FullscreenQuad(gl4);

        //Disable backface culling to keep all fragments
        gl4.glDisable(GL_CULL_FACE);
        gl4.glDisable(GL_DEPTH_TEST);
        gl4.glDisable(GL_STENCIL_TEST);
        gl4.glDisable(GL_BLEND);
    }

    private void initPrograms(GL4 gl4) {

        for (int program = Program.RENDER; program < Program.MAX; program++) {

            ShaderCode vertShader = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    VS_SRC[program], "vert", null, true);
            ShaderCode fragShader = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    FS_SRC[program], "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.link(gl4, System.out);

            programName[program] = shaderProgram.program();
        }
    }

    /**
     * Initialize A-Buffer storage. It is composed of a fragment buffer with
     * ABUFFER_SIZE layers and a "counter" buffer used to maintain the number
     * of fragments stored per pixel.
     *
     * @param gl4
     */
    private void initTargets(GL4 gl4) {

        if (abufferUseTextures) {

            textureName.position(Texture.ABUFFER);
            gl4.glCreateTextures(GL_TEXTURE_2D_ARRAY, 1, textureName);
            textureName.position(Texture.ABUFFER_COUNTER);
            gl4.glCreateTextures(GL_TEXTURE_2D, 1, textureName);
            textureName.position(0);

            gl4.glTextureParameteri(textureName.get(Texture.ABUFFER), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTextureParameteri(textureName.get(Texture.ABUFFER), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gl4.glTextureStorage3D(textureName.get(Texture.ABUFFER), 1, GL_RGBA32F, Resources.imageSize.x,
                    Resources.imageSize.y, ABUFFER_SIZE);

            gl4.glBindImageTexture(Semantic.Sampler.ABUFFER, textureName.get(Texture.ABUFFER), 0, true, 0, GL_READ_WRITE,
                    GL_RGBA32F);

            gl4.glTextureParameteri(textureName.get(Texture.ABUFFER_COUNTER), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTextureParameteri(textureName.get(Texture.ABUFFER_COUNTER), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

//            gl4.glTextureStorage2D(textureName.get(Texture.ABUFFER_COUNTER), 1, GL_R32F, Resources.imageSize.x,
            gl4.glTextureStorage2D(textureName.get(Texture.ABUFFER_COUNTER), 1, GL_R32UI, Resources.imageSize.x,
                    Resources.imageSize.y);

            gl4.glBindImageTexture(Semantic.Sampler.ABUFFER_COUNTER, textureName.get(Texture.ABUFFER_COUNTER), 0, false,
                    0, GL_READ_WRITE, GL_R32UI);

        } else {

            // TODO
        }
    }

    private void deleteTargets(GL4 gl4) {
        gl4.glDeleteTextures(Texture.MAX, textureName);
    }

    public void render(GL4 gl4, Scene scene) {

        gl4.glColorMask(true, true, true, true);
        gl4.glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
        gl4.glClear(GL_COLOR_BUFFER_BIT);

        // Clear A-Buffer
        if (useABuffer) {

            if (!abufferUseTextures) {
                // TODO
            }
            gl4.glUseProgram(programName[Program.CLEAR]);
            fullscreenQuad.render(gl4);

            // Ensure that all global memory write are done before starting to render
            gl4.glMemoryBarrier(0xffff);
        }

        // Renderg the model into the A-Buffer
        gl4.glUseProgram(programName[Program.RENDER]);
        scene.renderTransparent(gl4);

        // Resolve A-Buffer
        if (useABuffer) {

            // Ensure that all global memory write are done before resolving
            gl4.glMemoryBarrier(0xffff);

            gl4.glUseProgram(programName[Program.DISPLAY]);
            fullscreenQuad.render(gl4);
        }
    }

    public void reshape(GL4 gl4) {
        initTargets(gl4);
        deleteTargets(gl4);
    }
}
