/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3Official;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec2i;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class GlViewer implements GLEventListener {

    private Vec2i imageSize;
    private GLWindow glWindow;
    private NewtCanvasAWT newtCanvasAWT;
    private Animator animator;
    private DepthPeeling depthPeeling;
    private Model model;
    private ViewPole viewPole;
    private int[] ubo;
    private MouseListener mouseListener;
    public static float projectionBase;

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

        GL3 gl3 = glad.getGL().getGL3();

        try {
            model = new Model(gl3, "/data/dragon.obj");
        } catch (IOException ex) {
            Logger.getLogger(GlViewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Vec3 target = model.getCenter();
        Quat orient = new Quat(0.0f, 0.0f, 0.0f, 1.0f);
        ViewData initialViewData = new ViewData(target, orient, 0.5f, 0.0f);

        ViewScale viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.0005f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale, ViewPole.Projection.perspective);

        mouseListener = new MouseListener(viewPole);
        glWindow.addMouseListener(mouseListener);

        int blockBinding = 0;

        initUBO(gl3, blockBinding);

        depthPeeling = new DepthPeeling(gl3, imageSize, blockBinding);

        gl3.glDisable(GL3.GL_CULL_FACE);

        projectionBase = 5000f;

        checkError(gl3);
    }

    private void initUBO(GL3 gl3, int blockBinding) {

        ubo = new int[1];
        int size = 16 * GLBuffers.SIZEOF_FLOAT;

        gl3.glGenBuffers(1, ubo, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size * 2, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, blockBinding, ubo[0]);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo[0]);
        {
            int size = 16 * GLBuffers.SIZEOF_FLOAT;
            int offset = 0;

            FloatBuffer viewMat = GLBuffers.newDirectFloatBuffer(viewPole.calcMatrix().toFloatArray());

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, offset, size, viewMat);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        depthPeeling.render(gl3, model);

        checkError(gl3);
    }

    private void checkError(GL3 gl3) {

        int error = gl3.glGetError();

        if (error != GL3.GL_NO_ERROR) {
            System.out.println("error " + error);
        }
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL3 gl3 = glad.getGL().getGL3();

        depthPeeling.reshape(gl3, width, height);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, ubo[0]);
        {
            float aspect = (float) width / (float) height;
            int size = 16 * GLBuffers.SIZEOF_FLOAT;
            int offset = size;

            Mat4 projMat = Jglm.perspective(60f, aspect, 0.0001f, 10);
            FloatBuffer projFB = GLBuffers.newDirectFloatBuffer(projMat.toFloatArray());

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, offset, size, projFB);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(0, 0, width, height);

        checkError(gl3);
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
