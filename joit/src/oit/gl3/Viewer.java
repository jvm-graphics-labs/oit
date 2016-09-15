/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import oit.framework.Scene;
import oit.gl3.dp.DepthPeeling;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
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
import glm.Glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import oit.framework.BufferUtils;
import oit.framework.InputListener;
import oit.framework.Model;
import oit.framework.Resources;
import oit.gl3.ddp.DualDepthPeeling;
import oit.gl3.wa.WeightedAverage;
import oit.gl3.wb.WeightedBlended;
import oit.gl3.ws.WeightedSum;

/**
 *
 * @author gbarbieri
 */
public class Viewer implements GLEventListener {

    public static final String SHADERS_ROOT = "/oit/gl3/shaders/";
    public static final String[] SHADERS_SRC = new String[]{"opaque", "shade"};
    public static final String TITLE = "OIT";

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        Resources.glWindow = GLWindow.create(screen, glCapabilities);

        Resources.glWindow.setSize(Resources.imageSize.x, Resources.imageSize.y);
        Resources.glWindow.setPosition(50, 50);
        Resources.glWindow.setUndecorated(false);
        Resources.glWindow.setAlwaysOnTop(false);
        Resources.glWindow.setFullscreen(false);
        Resources.glWindow.setPointerVisible(true);
        Resources.glWindow.confinePointer(false);
        Resources.glWindow.setTitle(TITLE);

        Viewer viewer = new Viewer();

        Resources.glWindow.addGLEventListener(viewer);

        InputListener inputListener = new InputListener();
        Resources.glWindow.addMouseListener(inputListener);
        Resources.glWindow.addKeyListener(inputListener);

        Resources.animator = new Animator(Resources.glWindow);
        Resources.animator.setRunAsFastAsPossible(true);
        Resources.animator.setExclusiveContext(true);
        Resources.animator.start();

        Resources.glWindow.setVisible(true);
    }

    public class Oit {

        public static final int DEPTH_PEELING = 0;
        public static final int DUAL_DEPTH_PEELING = 1;
        public static final int WEIGHTED_AVERAGE = 2;
        public static final int WEIGHTED_SUM = 3;
        public static final int WEIGHTED_BLENDED = 4;
        public static final int MAX = 5;
    }

    public class Buffer {

        public static final int TRANSFORM0 = 0;
        public static final int TRANSFORM1 = 1;
        public static final int TRANSFORM2 = 2;
        public static final int PARAMETERS = 3;
        public static final int MAX = 4;
    }

    public class Texture {

        public static final int DEPTH = 0;
        public static final int COLOR = 1;
        public static final int MAX = 2;
    }

    public static IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    public static int newOit;
    public static OIT[] oit = new OIT[Oit.MAX];
    public static FullscreenQuad fullscreenQuad;

    private Scene scene;
    private IntBuffer framebufferName = GLBuffers.newDirectIntBuffer(1), queryName = GLBuffers.newDirectIntBuffer(1),
            timeBuffer = GLBuffers.newDirectIntBuffer(1);
    private Mat4 view = new Mat4(), proj = new Mat4(), projView = new Mat4();
    private int programName, currOit;
    private long start;
    private ArrayList<Integer> times = new ArrayList<>();

    @Override
    public void init(GLAutoDrawable glad) {

        GL3 gl3 = glad.getGL().getGL3();

        gl3.setSwapInterval(0);

        try {
            scene = new Scene(gl3, new ModelGL3(gl3));
        } catch (IOException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
        }

        initBuffers(gl3);

        initTargets(gl3);

        initPrograms(gl3);

        initSampler(gl3);

        initOIT(gl3);

        gl3.glGenQueries(1, queryName);

        gl3.glDisable(GL_CULL_FACE);

        fullscreenQuad = new FullscreenQuad(gl3);
        gl3.glGenQueries(1, OIT.queryName);

        printInfo();

        start = System.currentTimeMillis();

        checkError(gl3, "init");
    }

    private void initBuffers(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM1));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Float.BYTES * 2, Resources.parameters, GL_DYNAMIC_DRAW);

        ByteBuffer modelToClip = GLBuffers.newDirectByteBuffer(Mat4.SIZE);
        modelToClip.asFloatBuffer().put(Glm.ortho_(0, 1, 0, 1).toFa_());

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM2));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, modelToClip, GL_STATIC_DRAW);

        BufferUtils.destroyDirectBuffer(modelToClip);

        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM0));
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM1, bufferName.get(Buffer.TRANSFORM1));
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM2, bufferName.get(Buffer.TRANSFORM2));
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.PARAMETERS, bufferName.get(Buffer.PARAMETERS));
    }

    private void initTargets(GL3 gl3) {

        gl3.glGenTextures(Texture.MAX, textureName);
        gl3.glGenFramebuffers(1, framebufferName);

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.DEPTH));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_DEPTH_COMPONENT32F, Resources.imageSize.x, Resources.imageSize.y, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, null);

        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName.get(Texture.COLOR));

        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, Resources.imageSize.x, Resources.imageSize.y, 0, GL_RGBA,
                GL_FLOAT, null);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));

        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.DEPTH), 0);
        gl3.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE,
                textureName.get(Texture.COLOR), 0);
    }

    private void deleteTargets(GL3 gl3) {
        gl3.glDeleteTextures(Texture.MAX, textureName);
        gl3.glDeleteFramebuffers(1, framebufferName);
    }

    private void initPrograms(GL3 gl3) {

        ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, 2, this.getClass(), SHADERS_ROOT, SHADERS_SRC,
                "vert", null, null, null, true);
        ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 2, this.getClass(), SHADERS_ROOT, SHADERS_SRC,
                "frag", null, null, null, true);

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

    private void initSampler(GL3 gl3) {

        gl3.glGenSamplers(1, OIT.samplerName);

        gl3.glSamplerParameteri(OIT.samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(OIT.samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(OIT.samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(OIT.samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    private void initOIT(GL3 gl3) {

        oit[Oit.DEPTH_PEELING] = new DepthPeeling();
        oit[Oit.DUAL_DEPTH_PEELING] = new DualDepthPeeling();
        oit[Oit.WEIGHTED_AVERAGE] = new WeightedAverage();
        oit[Oit.WEIGHTED_SUM] = new WeightedSum();
        oit[Oit.WEIGHTED_BLENDED] = new WeightedBlended();

        newOit = Oit.DUAL_DEPTH_PEELING;
        currOit = newOit;
        oit[currOit].init(gl3);
    }

    private void printInfo() {
        System.out.println("GL3 Order Independent Transparency comparison sample");
        System.out.println("Commands");
        System.out.println("A/D\t\t- Change uniform opacity");
        System.out.println("W/S\t\t- Change uniform weight (only for WB)");
        System.out.println(" 1\t\t- Dual Depth Peeling");
        System.out.println(" 2\t\t- Depth Peeling");
        System.out.println(" 3\t\t- Weighted Average");
        System.out.println(" 4\t\t- Weighted Sum");
        System.out.println(" 5\t\t- Weighted Blended");
        System.out.println(" B\t\t- Toggle background color (black/white)");
        System.out.println(" O\t\t- Toggle occlusion queries (only for DDP and DP)");
        System.out.println("left/right\t- Change number of max passes (only for DDP and DP)");
    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL3 gl3 = glad.getGL().getGL3();

        {
            Glm.lookAt(InputListener.pos, new Vec3(InputListener.pos.x, InputListener.pos.y, 0), new Vec3(0, 1, 0), view)
                    .rotate((float) Math.toRadians(InputListener.rot.x), 1, 0, 0)
                    .rotate((float) Math.toRadians(InputListener.rot.y), 0, 1, 0)
                    .translate(Model.trans[0], Model.trans[1], Model.trans[2])
                    .scale(Model.scale);
            proj.mul(view, projView).toDbb(Resources.matBuffer);

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM0));
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, Resources.matBuffer);
        }
        {
            Resources.parameters.putFloat(0 * Float.BYTES, Resources.opacity);
            Resources.parameters.putFloat(1 * Float.BYTES, Resources.weight);

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PARAMETERS));
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Float.BYTES * 2, Resources.parameters);
        }

        if (newOit != currOit) {
            oit[currOit].dispose(gl3);
            oit[newOit].init(gl3);
            currOit = newOit;
        }

        Resources.numGeoPasses = 0;

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        gl3.glClearBufferfv(GL_COLOR, 0, Resources.clearColor.put(0, Resources.backgroundColor.x)
                .put(1, Resources.backgroundColor.y).put(2, Resources.backgroundColor.z).put(3, 1));
        gl3.glClearBufferfv(GL_DEPTH, 0, Resources.clearDepth.put(0, 1));

        gl3.glEnable(GL_DEPTH_TEST);

        gl3.glUseProgram(programName);
//        gl3.glColorMask(false, false, false, false);
        scene.renderOpaque(gl3);
//        gl3.glColorMask(true, true, true, true);

        // Beginning of the time query
        gl3.glBeginQuery(GL_TIME_ELAPSED, queryName.get(0));
        {
            oit[currOit].render(gl3, scene);
        }
        // End of the time query
        gl3.glEndQuery(GL_TIME_ELAPSED);
        // If the result of the query isn't here yet, we wait here...
        gl3.glGetQueryObjectuiv(queryName.get(0), GL_QUERY_RESULT, timeBuffer);
        times.add(timeBuffer.get(0));

        manageTitle();

        checkError(gl3, "display");
    }

    private void manageTitle() {

        long now = System.currentTimeMillis();
        long diff = now - start;
        if (diff > 1_000) {
            double average = 0;
            for (Integer time : times) {
                average += time;
            }
            average /= times.size();
            String title = TITLE + ", mode: ";
            switch (currOit) {
                case Oit.DEPTH_PEELING:
                    title += "DP";
                    break;
                case Oit.DUAL_DEPTH_PEELING:
                    title += "DDP";
                    break;
                case Oit.WEIGHTED_AVERAGE:
                    title += "WA";
                    break;
                case Oit.WEIGHTED_SUM:
                    title += "WS";
                    break;
                case Oit.WEIGHTED_BLENDED:
                    title += "WB";
                    break;
            }
            title += ", oit time: " + String.format("%.2f", (average / 1_000_000)) + " ms";
            title += ", oit theor. fps: " + String.format("%.2f", (1_000 / (average / 1_000_000)));
            title += ", alpha: " + String.format("%.2f", Resources.opacity);
            title += ", geom. passes: " + Resources.numGeoPasses;
            switch (currOit) {
                case Oit.DEPTH_PEELING:
                case Oit.DUAL_DEPTH_PEELING:
                    title += ", maxLayers: " + Resources.numLayers + ", occlusion query: " + Resources.useOQ;
                    break;
                case Oit.WEIGHTED_AVERAGE:
                case Oit.WEIGHTED_SUM:
                case Oit.WEIGHTED_BLENDED:
                    title += ", weigth parameter: " + String.format("%.2f", Resources.weight);
                    break;
            }
            Resources.glWindow.setTitle(title);
            times.clear();
            start = now;
        }
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL3 gl3 = glad.getGL().getGL3();

        Resources.imageSize.set(width, height);

        deleteTargets(gl3);
        initTargets(gl3);

        oit[currOit].reshape(gl3);

        {
            Glm.perspective((float) Math.toRadians(30f), (float) width / height, 0.0001f, 10, proj);
            proj.toDbb(Resources.matBuffer);
        }

        gl3.glViewport(0, 0, width, height);

        checkError(gl3, "reshape");
    }

    @Override
    public void dispose(GLAutoDrawable glad) {

        GL3 gl3 = glad.getGL().getGL3();

        deleteTargets(gl3);

        oit[currOit].dispose(gl3);

        checkError(gl3, "dispose");

        System.exit(0);
    }

    public static void checkError(GL3 gl3, String string) {

        int error = gl3.glGetError();

        if (error != GL_NO_ERROR) {
            throw new Error("[" + string + "] error: " + error);
        }
    }
}
