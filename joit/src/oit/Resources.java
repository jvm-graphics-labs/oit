/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat._4.Mat4;
import glm.vec._2.i.Vec2i;
import glm.vec._3.Vec3;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import oit.gl3.FullscreenQuad;
import oit.gl3.OIT;

/**
 *
 * @author GBarbieri
 */
public class Resources {

    public static boolean useOQ = true;

    public static int numPasses = 4;
    public static int numGeoPasses = 0;

    public static Vec3 backgroundColor = new Vec3(1);

    public static Vec3 white = new Vec3(1);
    public static Vec3 black = new Vec3(0);

    public static Vec2i imageSize = new Vec2i(1024, 768);

    public static Animator animator;

    public static GLWindow glWindow;

    public static ByteBuffer parameters = GLBuffers.newDirectByteBuffer(Float.BYTES * 2);

    public static float opacity = 0.6f;
    public static float weight = 0.5f;

    public static ByteBuffer matBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);

    public static int numLayers = (Resources.numPasses - 1) * 2;
}
