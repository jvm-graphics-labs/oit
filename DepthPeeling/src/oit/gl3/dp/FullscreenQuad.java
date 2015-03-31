/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dp;

import com.jogamp.opengl.util.GLBuffers;
import java.nio.FloatBuffer;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class FullscreenQuad {

    private int[] objects;

    public FullscreenQuad(GL3 gl3) {

        objects = new int[Objects.size.ordinal()];

        initVbo(gl3);

        initVao(gl3);
    }

    public void render(GL3 gl3) {

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);

        gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
        {
            //  Render, passing the vertex number
            gl3.glDrawArrays(GL3.GL_QUADS, 0, 4);
        }
        gl3.glBindVertexArray(0);
    }

    private void initVbo(GL3 gl3) {

        float[] vertexAttributes = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f};

        gl3.glGenBuffers(1, objects, Objects.vbo.ordinal());

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexAttributes);

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexAttributes.length * 4, buffer, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private void initVao(GL3 gl3) {

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);

        gl3.glGenVertexArrays(1, objects, Objects.vao.ordinal());
        gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
        {
            gl3.glEnableVertexAttribArray(0);
            {
                gl3.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 0, 0);
            }
        }
        gl3.glBindVertexArray(0);
    }

    private enum Objects {

        vbo,
        vao,
        size
    }
}
