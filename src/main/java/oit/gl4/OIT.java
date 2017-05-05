/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import static com.jogamp.opengl.GL2ES2.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public abstract class OIT {

    public static IntBuffer drawBuffers = GLBuffers.newDirectIntBuffer(new int[]{
        GL_COLOR_ATTACHMENT0,
        GL_COLOR_ATTACHMENT1,
        GL_COLOR_ATTACHMENT2,
        GL_COLOR_ATTACHMENT3,
        GL_COLOR_ATTACHMENT4,
        GL_COLOR_ATTACHMENT5,
        GL_COLOR_ATTACHMENT6}
    ),
            queryName = GLBuffers.newDirectIntBuffer(1), samplerName = GLBuffers.newDirectIntBuffer(1),
            samplesCount = GLBuffers.newDirectIntBuffer(1);

    public abstract void init(GL4 gl4);

    public abstract void render(GL4 gl4, Scene scene);

    public abstract void reshape(GL4 gl4);

    public abstract void dispose(GL4 gl4);
}
