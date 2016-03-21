/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL.GL_DONT_CARE;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL4;
import static com.jogamp.opengl.GL4.GL_DYNAMIC_STORAGE_BIT;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import glm.glm;
import glm.mat._4.Mat4;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jglm.Jglm;
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
    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private InputListener inputListener1;
    public static float projectionBase;
    private Scene scene;
    private WeightedBlended weightedBlended;
    private WeightedBlendedOpaque weightedBlendedOpaque;
    private FloatBuffer viewProjBuffer = GLBuffers.newDirectFloatBuffer(16);
    private Mat4 proj = new Mat4(), viewProj = new Mat4();
    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    @Override
    public void init(GLAutoDrawable glad) {

        GL4 gl4 = glad.getGL().getGL4();

        initDebug(gl4);

        initBuffers(gl4);

        try {
            scene = new Scene(gl4, "/data/dragon.obj");
        } catch (IOException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        inputListener1 = new InputListener();
        glWindow.addMouseListener(inputListener1);
        glWindow.addKeyListener(inputListener1);

        int blockBinding = 0;

        weightedBlended = new WeightedBlended(gl4, blockBinding);
        weightedBlendedOpaque = new WeightedBlendedOpaque(gl4, blockBinding);

        gl4.glDisable(GL4.GL_CULL_FACE);

        projectionBase = 5000f;

        animator.setUpdateFPSFrames(60, System.out);
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

    private void initBuffers(GL4 gl4) {

        gl4.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(Buffer.VIEW_PROJ), Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.MODEL), Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

        } else {

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.VIEW_PROJ));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MODEL));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL4 gl4 = glad.getGL().getGL4();

        {
            inputListener1.update();

            viewProj.set(proj).mul(inputListener1.getView());
            viewProjBuffer.put(viewProj.toFa_()).rewind();

            gl4.glNamedBufferSubData(bufferName.get(Buffer.VIEW_PROJ), 0, Mat4.SIZE, viewProjBuffer);
        }
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.VIEW_PROJ));

//        weightedBlended.render(gl4, scene);
        weightedBlendedOpaque.render(gl4, scene);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL4 gl4 = glad.getGL().getGL4();

        weightedBlended.reshape(gl4, width, height);
        weightedBlendedOpaque.reshape(gl4, width, height);

        imageSize = new Vec2i(width, height);

        glm.perspective(30f, (float) width / height, 0.0001f, 10, proj);

        gl4.glViewport(0, 0, width, height);
    }

    public class Buffer {

        public static final int VIEW_PROJ = 0;
        public static final int MODEL = 1;
        public static final int MAX = 2;
    }
}
