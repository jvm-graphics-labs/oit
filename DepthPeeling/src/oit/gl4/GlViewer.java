/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec2i;
import jglm.Vec3;
import oit.gl4.wb.WeightedBlended;

/**
 *
 * @author gbarbieri
 */
public class GlViewer implements GLEventListener {

    private Vec2i imageSize;
    private GLWindow glWindow;
    private NewtCanvasAWT newtCanvasAWT;
    private Animator animator;
    private ViewPole viewPole;
    private int[] ubo;
    private MouseListener mouseListener;
    public static float projectionBase;
    private Scene scene;
    private WeightedBlended weightedBlended;

    public GlViewer() {

        imageSize = new Vec2i(1024, 768);

        initGL();
    }

    private void initGL() {

        GLProfile gLProfile = GLProfile.getDefault();

        GLCapabilities gLCapabilities = new GLCapabilities(gLProfile);

        glWindow = GLWindow.create(gLCapabilities);

        newtCanvasAWT = new NewtCanvasAWT(glWindow);

        glWindow.setSize(imageSize.x, imageSize.y);

        glWindow.addGLEventListener(this);

        animator = new Animator(glWindow);
        animator.start();
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        GL4 gl4 = glad.getGL().getGL4();

        try {
            scene = new Scene(gl4, "/data/dragon.obj");
        } catch (IOException ex) {
            Logger.getLogger(GlViewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Vec3 target = new Vec3(0f, .12495125f, 0f);
        Quat orient = new Quat(0.0f, 0.0f, 0.0f, 1.0f);
        ViewData initialViewData = new ViewData(target, orient, 0.5f, 0.0f);

        ViewScale viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.0005f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale, ViewPole.Projection.perspective);

        mouseListener = new MouseListener(viewPole);
        glWindow.addMouseListener(mouseListener);

        int blockBinding = 0;

        initUBO(gl4, blockBinding);

        weightedBlended = new WeightedBlended(gl4, blockBinding);

        gl4.glDisable(GL4.GL_CULL_FACE);

        projectionBase = 5000f;

        animator.setUpdateFPSFrames(60, System.out);

        checkError(gl4);
    }

    private void initUBO(GL4 gl4, int blockBinding) {

        ubo = new int[1];
        int size = 16 * GLBuffers.SIZEOF_FLOAT;

        gl4.glGenBuffers(1, ubo, 0);
        gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, ubo[0]);
        {
            gl4.glBufferData(GL4.GL_UNIFORM_BUFFER, size * 2, null, GL4.GL_DYNAMIC_DRAW);

            gl4.glBindBufferBase(GL4.GL_UNIFORM_BUFFER, blockBinding, ubo[0]);
        }
        gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL4 gl4 = glad.getGL().getGL4();

        updateCamera(gl4);

        weightedBlended.render(gl4, scene);

        checkError(gl4);
    }

    private void updateCamera(GL4 gl4) {

        gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, ubo[0]);
        {
            int size = 16 * GLBuffers.SIZEOF_FLOAT;
            int offset = 0;

            FloatBuffer viewMat = GLBuffers.newDirectFloatBuffer(viewPole.calcMatrix().toFloatArray());

            gl4.glBufferSubData(GL4.GL_UNIFORM_BUFFER, offset, size, viewMat);
        }
        gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, 0);
    }

    private void checkError(GL4 gl4) {

        int error = gl4.glGetError();

        if (error != GL4.GL_NO_ERROR) {
            System.out.println("error " + error);
        }
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL4 gl4 = glad.getGL().getGL4();

        weightedBlended.reshape(gl4, width, height);

        imageSize = new Vec2i(width, height);

        updateProjection(gl4, width, height);

        gl4.glViewport(0, 0, width, height);

        checkError(gl4);
    }

    private void updateProjection(GL4 gl3, int width, int height) {

        gl3.glBindBuffer(GL4.GL_UNIFORM_BUFFER, ubo[0]);
        {
            float aspect = (float) width / (float) height;
            int size = 16 * GLBuffers.SIZEOF_FLOAT;
            int offset = size;

            Mat4 projMat = Jglm.perspective(60f, aspect, 0.0001f, 10);
            FloatBuffer projFB = GLBuffers.newDirectFloatBuffer(projMat.toFloatArray());

            gl3.glBufferSubData(GL4.GL_UNIFORM_BUFFER, offset, size, projFB);
        }
        gl3.glBindBuffer(GL4.GL_UNIFORM_BUFFER, 0);
    }

    public NewtCanvasAWT getNewtCanvasAWT() {
        return newtCanvasAWT;
    }

    public GLWindow getGlWindow() {
        return glWindow;
    }

    public Animator getAnimator() {
        return animator;
    }
}
