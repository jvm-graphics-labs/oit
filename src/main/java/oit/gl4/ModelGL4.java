/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import glm.vec._3.Vec3;
import java.io.IOException;
import oit.framework.Model;

/**
 *
 * @author gbarbieri
 */
public final class ModelGL4 extends Model {

    public ModelGL4(GL4 gl3) throws IOException {
        super(gl3);
    }

    @Override
    protected void initBuffers(GL3 gl3) {
        /**
         * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
         */
        boolean bug1287 = true;

        GL4 gl4 = (GL4) gl3;

        gl4.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(Buffer.VERTEX), vertices.capacity() * Float.BYTES, vertices,
                    GL_STATIC_DRAW);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.ELEMENT), indices.capacity() * Integer.BYTES,
                    indices, GL_STATIC_DRAW);

        } else {

            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glBufferStorage(GL_ARRAY_BUFFER, vertices.capacity() * Float.BYTES, vertices, 0);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            gl4.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * Integer.BYTES, indices, 0);
        }
    }

    @Override
    protected void initVertexArray(GL3 gl3) {

        GL4 gl4 = (GL4) gl3;

        gl4.glCreateVertexArrays(1, vertexArrayName);

        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.POSITION, Semantic.Stream._0);
        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.NORMAL, Semantic.Stream._0);

        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.POSITION, 3, GL_FLOAT, false, 0);
        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.NORMAL, 3, GL_FLOAT, false, Vec3.SIZE);

        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.POSITION);
        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.NORMAL);

        gl4.glVertexArrayElementBuffer(vertexArrayName.get(0), bufferName.get(Buffer.ELEMENT));
        gl4.glVertexArrayVertexBuffer(vertexArrayName.get(0), Semantic.Stream._0, bufferName.get(Buffer.VERTEX), 0,
                2 * Vec3.SIZE);
    }

    @Override
    public void render(GL3 gl3) {
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glDrawElements(GL_TRIANGLES, getCompiledIndexCount(), GL_UNSIGNED_INT, 0);
        }
        gl3.glBindVertexArray(0);
    }
}
