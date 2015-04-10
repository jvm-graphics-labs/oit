/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import com.jogamp.opengl.util.GLBuffers;
import java.nio.FloatBuffer;
import javax.media.opengl.GL4;

/**
 *
 * @author gbarbieri
 */
public class FullscreenQuad {

    private int[] objects;

    public FullscreenQuad(GL4 gl4) {

        objects = new int[Objects.size.ordinal()];

        initVbo(gl4);

        initVao(gl4);
    }

    public void render(GL4 gl4) {

        gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);

        gl4.glBindVertexArray(objects[Objects.vao.ordinal()]);
        {
            //  Render, passing the vertex number
            gl4.glDrawArrays(GL4.GL_QUADS, 0, 4);
        }
        gl4.glBindVertexArray(0);
    }

    private void initVbo(GL4 gl3) {

        float[] vertexAttributes = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f};

        gl3.glGenBuffers(1, objects, Objects.vbo.ordinal());

        gl3.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexAttributes);

            gl3.glBufferData(GL4.GL_ARRAY_BUFFER, vertexAttributes.length * 4, buffer, GL4.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
    }

    private void initVao(GL4 gl4) {

        gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);

        gl4.glGenVertexArrays(1, objects, Objects.vao.ordinal());
        gl4.glBindVertexArray(objects[Objects.vao.ordinal()]);
        {
            gl4.glEnableVertexAttribArray(0);
            {
                gl4.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);
            }
        }
        gl4.glBindVertexArray(0);
    }

    private enum Objects {

        vbo,
        vao,
        size
    }
}
