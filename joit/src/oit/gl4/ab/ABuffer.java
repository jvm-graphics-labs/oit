/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.ab;

import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL4;
import static com.jogamp.opengl.GL4.GL_DYNAMIC_STORAGE_BIT;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.vec._4.Vec4;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import oit.framework.Resources;
import oit.gl4.OIT;
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

    private static final boolean useABuffer = true;
    private static final boolean useSorting = true;
    private static final boolean resolveAlphaCorrection = false;
    private static final boolean resolveGelly = false;
    private static final boolean useTextures = true;

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

    private static class Parameters {

        public static final int SIZE = 8 * Integer.BYTES + Vec4.SIZE;
        private static ByteBuffer buffer = GLBuffers.newDirectByteBuffer(SIZE);

        public static ByteBuffer getBuffer() {
            buffer
                    .putInt(Resources.imageSize.x).putInt(Resources.imageSize.y)
                    .putInt(useABuffer ? 1 : 0)
                    .putInt(ABUFFER_SIZE)
                    .putInt(useTextures ? 1 : 0)
                    .putInt(resolveGelly ? 1 : 0)
                    .putInt(useSorting ? 1 : 0)
                    .putInt(resolveAlphaCorrection ? 1 : 0)
                    .putFloat(Resources.backgroundColor.x).putFloat(Resources.backgroundColor.y)
                    .putFloat(Resources.backgroundColor.z).putFloat(0f)
                    .rewind();

            return buffer;
        }
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            bufferName = GLBuffers.newDirectIntBuffer(1);
    private FullscreenQuad fullscreenQuad;
    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    public ABuffer(GL4 gl4) {

        initPrograms(gl4);

        initBuffer(gl4);

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

    private void initBuffer(GL4 gl4) {

        gl4.glCreateBuffers(1, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(0), Parameters.SIZE, Parameters.getBuffer(), GL_DYNAMIC_STORAGE_BIT);

        } else {

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(0));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Parameters.SIZE, null, GL_DYNAMIC_STORAGE_BIT);
        }

        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(0));
    }

    /**
     * Initialize A-Buffer storage. It is composed of a fragment buffer with
     * ABUFFER_SIZE layers and a "counter" buffer used to maintain the number
     * of fragments stored per pixel.
     *
     * @param gl4
     */
    private void initTargets(GL4 gl4) {

        if (useTextures) {

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
            gl4.glBindSampler(Semantic.Sampler.ABUFFER, OIT.samplerName.get(0));

            gl4.glTextureParameteri(textureName.get(Texture.ABUFFER_COUNTER), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTextureParameteri(textureName.get(Texture.ABUFFER_COUNTER), GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            gl4.glTextureStorage2D(textureName.get(Texture.ABUFFER_COUNTER), 1, GL_R32UI, Resources.imageSize.x,
                    Resources.imageSize.y);

            gl4.glBindImageTexture(Semantic.Sampler.ABUFFER_COUNTER, textureName.get(Texture.ABUFFER_COUNTER), 0, false,
                    0, GL_READ_WRITE, GL_R32UI);
            gl4.glBindSampler(Semantic.Sampler.ABUFFER_COUNTER, OIT.samplerName.get(0));

        } else {

            // TODO
        }
    }

    private void deleteTargets(GL4 gl4) {
        gl4.glDeleteTextures(Texture.MAX, textureName);
    }

    public void render(GL4 gl4, Scene scene) {

        {
            gl4.glNamedBufferSubData(bufferName.get(0), 0, Parameters.SIZE, Parameters.getBuffer());
        }

        gl4.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, Resources.backgroundColor.x)
                .put(1, Resources.backgroundColor.y).put(2, Resources.backgroundColor.z).put(3, 1));
        gl4.glClearBufferfv(GL_DEPTH, 0, Resources.clearDepth.put(0, 1));

        gl4.glEnable(GL_DEPTH_TEST);

        // Clear A-Buffer
        if (useABuffer) {

            if (!useTextures) {
                // TODO
            }
            gl4.glClearNamedBufferData(GL_BUFFER, GL_R8_SNORM, GL_MAX, GL_RG, textureName);
            gl4.glUseProgram(programName[Program.CLEAR]);
            fullscreenQuad.render(gl4);

            // Ensure that all global memory write are done before starting to render
            gl4.glMemoryBarrier(0xffff);
        }

        // Renderg the model into the A-Buffer
        gl4.glUseProgram(programName[Program.RENDER]);
//        scene.renderTransparent(gl4);

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
