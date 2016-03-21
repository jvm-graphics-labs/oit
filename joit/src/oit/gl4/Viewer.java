/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL.GL_DONT_CARE;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
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
import oit.gl4.wb.WeightedBlended;
import oit.gl4.wbo.WeightedBlendedOpaque;

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
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(imageSize.x, imageSize.y);
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(false);
        glWindow.setAlwaysOnTop(false);
        glWindow.setFullscreen(false);
        glWindow.setPointerVisible(true);
        glWindow.confinePointer(false);
        glWindow.setTitle("Weighted Blended");
        glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        glWindow.setVisible(true);

        Viewer viewer = new Viewer();
        glWindow.addGLEventListener(viewer);

        animator = new Animator(glWindow);
        animator.start();
    }
    
    private ViewPole viewPole;
    private int[] ubo;
    private InputListener inputListener;
    public static float projectionBase;
    private Scene scene;
    private WeightedBlended weightedBlended;
    private WeightedBlendedOpaque weightedBlendedOpaque;

    @Override
    public void init(GLAutoDrawable glad) {

        GL4 gl4 = glad.getGL().getGL4();
        
        initDebug(gl4);

        try {
            scene = new Scene(gl4, "/data/dragon.obj");
        } catch (IOException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Vec3 target = new Vec3(0f, .12495125f, 0f);
        Quat orient = new Quat(0.0f, 0.0f, 0.0f, 1.0f);
        ViewData initialViewData = new ViewData(target, orient, 0.5f, 0.0f);

        ViewScale viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.0005f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale, ViewPole.Projection.perspective);

        inputListener = new InputListener(viewPole);
        glWindow.addMouseListener(inputListener);
        glWindow.addKeyListener(inputListener);

        int blockBinding = 0;

        initUBO(gl4, blockBinding);

        weightedBlended = new WeightedBlended(gl4, blockBinding);
        weightedBlendedOpaque = new WeightedBlendedOpaque(gl4, blockBinding);

        gl4.glDisable(GL4.GL_CULL_FACE);

        projectionBase = 5000f;

        animator.setUpdateFPSFrames(60, System.out);

        checkError(gl4);
    }
    
    private void initDebug(GL4 gl4) {

        glWindow.getContext().addGLDebugListener(new GlDebugOutput());
        // Turn off all the debug
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DONT_CARE, // severity
                0, // count
                null, // id
                false); // enabled
        // Turn on all OpenGL Errors, shader compilation/linking errors, or highly-dangerous undefined behavior 
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DEBUG_SEVERITY_HIGH, // severity
                0, // count
                null, // id
                true); // enabled
        // Turn on all major performance warnings, shader compilation/linking warnings or the use of deprecated functions
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DEBUG_SEVERITY_MEDIUM, // severity
                0, // count
                null, // id
                true); // enabled
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
        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL4 gl4 = glad.getGL().getGL4();

        updateCamera(gl4);

//        weightedBlended.render(gl4, scene);
        weightedBlendedOpaque.render(gl4, scene);

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
        weightedBlendedOpaque.reshape(gl4, width, height);

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

    public GLWindow getGlWindow() {
        return glWindow;
    }

    public Animator getAnimator() {
        return animator;
    }
}
