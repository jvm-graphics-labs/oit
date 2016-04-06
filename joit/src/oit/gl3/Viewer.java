/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jglm.Vec2i;
import oit.InputListener;
import oit.gl3.ddp.DualDepthPeeling;
import oit.gl3.dp.DepthPeeling;
import oit.gl3.wa.WeightedAverage;
import oit.gl3.wb.WeightedBlended;
import oit.gl3.wbo.WeightedBlendedOpaque;
import oit.gl3.ws.WeightedSum;

/**
 *
 * @author gbarbieri
 */
public class Viewer implements GLEventListener {

    public static final String MODEL = "/data/dragon.obj";
    public static final String SHADERS_ROOT = "/oit/gl3/shaders/";
    public static final String[] SHADERS_SRC = new String[]{"opaque", "shade"};
    public static Vec2i imageSize = new Vec2i(1024, 768);
    public static GLWindow glWindow;
    public static Animator animator;

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
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

        animator = new Animator(glWindow);
        animator.start();

        glWindow.setVisible(true);
    }

    private DepthPeeling depthPeeling;
    private DualDepthPeeling dualDepthPeeling;
    private WeightedSum weightedSum;
    private WeightedAverage weightedAverage;
    private WeightedBlended weightedBlended;
    private WeightedBlendedOpaque weightedBlendedOpaque;

    public class Buffer {

        public static final int TRANSFORM0 = 0;
        public static final int TRANSFORM1 = 1;
        public static final int MAX = 2;
    }

    public class Texture {

        public static final int DEPTH = 0;
        public static final int COLOR = 1;
        public static final int MAX = 2;
    }

    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private InputListener inputListener;
    private Scene scene;
    private IntBuffer framebufferName = GLBuffers.newDirectIntBuffer(1);
    private ByteBuffer viewBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE),
            projBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);
    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4), clearDepth = GLBuffers.newDirectFloatBuffer(1);
    private int programName;

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        GL3 gl3 = glad.getGL().getGL3();

        try {
            scene = new Scene(gl3);
        } catch (IOException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        inputListener = new InputListener(animator);
        glWindow.addMouseListener(inputListener);
        glWindow.addKeyListener(inputListener);

        initBuffers(gl3);

        initTargets(gl3);

        initPrograms(gl3);

        depthPeeling = new DepthPeeling(gl3);
//        dualDepthPeeling = new DualDepthPeeling(gl3, imageSize, blockBinding);
//        weightedSum = new WeightedSum(gl3, blockBinding);
//        weightedAverage = new WeightedAverage(gl3, blockBinding);
//        weightedBlended = new WeightedBlended(gl3, blockBinding);

//        weightedBlendedOpaque = new WeightedBlendedOpaque(gl3, blockBinding);
        gl3.glDisable(GL_CULL_FACE);

        clearColor.put(new float[]{1, 1, 1, 1}).rewind();
        clearDepth.put(new float[]{1}).rewind();

        gl3.setSwapInterval(0);
        animator.setRunAsFastAsPossible(true);
        animator.setUpdateFPSFrames(30, System.out);

        checkError(gl3, "init");
    }

    private void initBuffers(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE * 2, null, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM0));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM1));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM1, bufferName.get(Buffer.TRANSFORM1));

    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);
        gl3.glGenFramebuffers(1, framebufferName);

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_DEPTH_COMPONENT32F, Viewer.imageSize.x, Viewer.imageSize.y, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, null);

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.COLOR));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Viewer.imageSize.x, Viewer.imageSize.y, 0, GL_RGBA,
                GL_FLOAT, null);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));

        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.DEPTH), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.COLOR), 0);

        if (gl3.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new Error("framebuffer " + framebufferName.get(0) + " incomplete!");
        }
    }

    private void initPrograms(GL3 gl3) {

        ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT, SHADERS_SRC,
                "vs", null, null, null, true);
        ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT, SHADERS_SRC,
                "fs", null, null, null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.link(gl3, System.out);

        programName = shaderProgram.program();

        gl3.glUniformBlockBinding(
                programName,
                gl3.glGetUniformBlockIndex(programName, "Transform0"),
                Semantic.Uniform.TRANSFORM0);

        gl3.glUniformBlockBinding(
                programName,
                gl3.glGetUniformBlockIndex(programName, "Transform1"),
                Semantic.Uniform.TRANSFORM1);

        gl3.glUniformBlockBinding(
                programName,
                gl3.glGetUniformBlockIndex(programName, "Parameters"),
                Semantic.Uniform.PARAMETERS);
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        {
            inputListener.update();

            viewBuffer.asFloatBuffer().put(inputListener.getView().toFa_());

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, viewBuffer);
        }

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        gl3.glClearBufferfv(GL_COLOR, 0, clearColor);
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth);

        gl3.glEnable(GL_DEPTH_TEST);

        gl3.glUseProgram(programName);
        scene.renderOpaque(gl3);

        depthPeeling.render(gl3, scene);
//        weightedSum.render(gl3, scene);
//        weightedAverage.render(gl3, scene);
//        weightedBlended.render(gl3, scene);
//        weightedBlendedOpaque.render(gl3, scene);
        checkError(gl3, "display");
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL3 gl3 = glad.getGL().getGL3();

        depthPeeling.reshape(gl3, width, height);
//        weightedSum.reshape(gl3, width, height);
//        weightedAverage.reshape(gl3, width, height);
//        weightedBlended.reshape(gl3, width, height);
//
//        weightedBlendedOpaque.reshape(gl3, width, height);

        imageSize = new Vec2i(width, height);

        {
            projBuffer.asFloatBuffer().put(glm.perspective_(30f, (float) width / height, 0.001f, 10).toFa_());

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE, Mat4.SIZE, projBuffer);
        }

        gl3.glViewport(0, 0, width, height);

        checkError(gl3, "reshape");
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
//        System.out.println("dispose");

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glDeleteTextures(Texture.MAX, textureName);
        gl3.glDeleteFramebuffers(1, framebufferName);

        checkError(gl3, "dispose");

        System.exit(0);
    }

    private void checkError(GL3 gl3, String string) {

        int error = gl3.glGetError();

        if (error != GL_NO_ERROR) {
            System.out.println(string + "error " + error);
        }
    }
}
