/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL2ES2.*;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_RECTANGLE;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public abstract class OIT {

//    protected IntBuffer drawBuffers = GLBuffers.newDirectIntBuffer(new int[]{
//        GL_COLOR_ATTACHMENT0,
//        GL_COLOR_ATTACHMENT1,
//        GL_COLOR_ATTACHMENT2,
//        GL_COLOR_ATTACHMENT3,
//        GL_COLOR_ATTACHMENT4,
//        GL_COLOR_ATTACHMENT5,
//        GL_COLOR_ATTACHMENT5}
//    );
    protected int[] drawBuffers = new int[]{
        GL_COLOR_ATTACHMENT0,
        GL_COLOR_ATTACHMENT1,
        GL_COLOR_ATTACHMENT2,
        GL_COLOR_ATTACHMENT3,
        GL_COLOR_ATTACHMENT4,
        GL_COLOR_ATTACHMENT5,
        GL_COLOR_ATTACHMENT6};

    protected FloatBuffer clearColor, clearDepth;

    public abstract void init(GL3 gl3);

    public abstract void render(GL3 gl3, Scene scene);

    public abstract void reshape(GL3 gl3);

    public abstract void dispose(GL3 gl3);

    protected void bindTextureRect(GL3 gl3, int textureName, int textureUnit, IntBuffer samplerName) {

        gl3.glActiveTexture(GL_TEXTURE0 + textureUnit);
        gl3.glBindTexture(GL_TEXTURE_RECTANGLE, textureName);
        gl3.glBindSampler(textureUnit, samplerName.get(0));
    }
}
