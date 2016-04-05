/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import oit.gl3.dpo.DepthPeelingOpaque;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import glm.glm;
import glm.mat._4.Mat4;
import java.io.IOException;
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

    private DepthPeelingOpaque depthPeelingOpaque;
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

    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private InputListener inputListener;
    public static float projectionBase;
    private Scene scene;

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        GL3 gl3 = glad.getGL().getGL3();

        try {
            scene = new Scene(gl3, "/data/dragon.obj");
        } catch (IOException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        inputListener = new InputListener(animator);
        glWindow.addMouseListener(inputListener);
        glWindow.addKeyListener(inputListener);

        initBuffers(gl3);

        depthPeeling = new DepthPeeling(gl3);
//        dualDepthPeeling = new DualDepthPeeling(gl3, imageSize, blockBinding);
//        weightedSum = new WeightedSum(gl3, blockBinding);
//        weightedAverage = new WeightedAverage(gl3, blockBinding);
//        weightedBlended = new WeightedBlended(gl3, blockBinding);
//
//        depthPeelingOpaque = new DepthPeelingOpaque(gl3, imageSize, blockBinding);
//        weightedBlendedOpaque = new WeightedBlendedOpaque(gl3, blockBinding);

        gl3.glDisable(GL_CULL_FACE);

        projectionBase = 5000f;

        animator.setUpdateFPSFrames(60, System.out);

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

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        updateCamera(gl3);

//        depthPeelingOpaque.render(gl3, scene);
        depthPeeling.render(gl3, scene);
//        weightedSum.render(gl3, scene);
//        weightedAverage.render(gl3, scene);
//        weightedBlended.render(gl3, scene);
//        weightedBlendedOpaque.render(gl3, scene);

        checkError(gl3, "display");
    }

    private void updateCamera(GL3 gl3) {

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
        {
            int size = 16 * GLBuffers.SIZEOF_FLOAT;
            int offset = 0;

            inputListener.update();

            FloatBuffer viewMat = GLBuffers.newDirectFloatBuffer(inputListener.getView().toFa_());

            gl3.glBufferSubData(GL_UNIFORM_BUFFER, offset, size, viewMat);
        }
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void checkError(GL3 gl3, String string) {

        int error = gl3.glGetError();

        if (error != GL_NO_ERROR) {
            System.out.println(string + "error " + error);
        }
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
//        depthPeelingOpaque.reshape(gl3, width, height);
//        weightedBlendedOpaque.reshape(gl3, width, height);

        imageSize = new Vec2i(width, height);

        updateProjection(gl3, width, height);

        gl3.glViewport(0, 0, width, height);

        checkError(gl3, "reshape");
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
//        System.out.println("dispose");
        System.exit(0);
    }

    private void updateProjection(GL3 gl3, int width, int height) {

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
        {

            FloatBuffer projFB
                    = GLBuffers.newDirectFloatBuffer(glm.perspective_(30f, (float) width / height, 0.001f, 10).toFa_());

            gl3.glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE, Mat4.SIZE, projFB);
        }
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
}
