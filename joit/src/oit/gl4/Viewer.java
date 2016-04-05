/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import oit.InputListener;
import oit.BufferUtils;
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jglm.Vec2i;
import oit.gl4.wb.WeightedBlendedOpaque;

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

    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private InputListener inputListener;
    private Scene scene;
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

        inputListener = new InputListener(animator);
        glWindow.addMouseListener(inputListener);
        glWindow.addKeyListener(inputListener);

        weightedBlendedOpaque = new WeightedBlendedOpaque(gl4);

        gl4.glDisable(GL4.GL_CULL_FACE);

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

            gl4.glNamedBufferStorage(bufferName.get(Buffer.MODEL_CLIP), Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.PARAMETERS), 2 * Float.BYTES, null, GL_DYNAMIC_STORAGE_BIT);

        } else {

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.VIEW_PROJ));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MODEL));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_STORAGE_BIT);

            Mat4 modelToClip = glm.ortho_(0, 1, 0, 1);
            ByteBuffer buffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);
            buffer.asFloatBuffer().put(modelToClip.toFa_());
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MODEL_CLIP));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, Mat4.SIZE, buffer, GL_DYNAMIC_STORAGE_BIT);

            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
            gl4.glBufferStorage(GL_UNIFORM_BUFFER, 2 * Float.BYTES, null, GL_DYNAMIC_STORAGE_BIT);
            
            BufferUtils.destroyDirectBuffer(buffer);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        
        GL4 gl4 = glad.getGL().getGL4();        
        weightedBlendedOpaque.dispose(gl4);
        
        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL4 gl4 = glad.getGL().getGL4();

        {
            inputListener.update();

            viewProj.set(proj).mul(inputListener.getView());
            viewProjBuffer.put(viewProj.toFa_()).rewind();

            gl4.glNamedBufferSubData(bufferName.get(Buffer.VIEW_PROJ), 0, Mat4.SIZE, viewProjBuffer);
        }
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.VIEW_PROJ));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM2, bufferName.get(Buffer.MODEL_CLIP));

        weightedBlendedOpaque.render(gl4, scene);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {

        GL4 gl4 = glad.getGL().getGL4();

        weightedBlendedOpaque.reshape(gl4, width, height);

        imageSize = new Vec2i(width, height);

        glm.perspective(30f, (float) width / height, 0.0001f, 10, proj);

        gl4.glViewport(0, 0, width, height);
    }

    public class Buffer {

        public static final int VIEW_PROJ = 0;
        public static final int MODEL = 1;
        public static final int MODEL_CLIP = 2;
        public static final int PARAMETERS = 3;
        public static final int MAX = 4;
    }
}
