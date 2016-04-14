/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.ab;

import oit.gl4.*;
import static com.jogamp.opengl.GL.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import glm.vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author gbarbieri
 */
public class FullscreenQuad {

    private int vertexCount = 4;
    private int vertexSize = vertexCount * Vec4.SIZE;
    // TODO check 3rd and 4th component utility
    private FloatBuffer vertexData = GLBuffers.newDirectFloatBuffer(new float[]{
        -1.0f, -1.0f, 0.0f, 1.0f,
        +1.0f, -1.0f, 0.0f, 1.0f,
        -1.0f, +1.0f, 0.0f, 1.0f,
        +1.0f, +1.0f, 0.0f, 1.0f});

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private ShortBuffer elementData = GLBuffers.newDirectShortBuffer(new short[]{
        0, 1, 2,
        1, 3, 2});

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    public FullscreenQuad(GL4 gl4) {

        initBuffer(gl4);
        initVertexArray(gl4);
    }

    private void initBuffer(GL4 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferStorage(GL_ARRAY_BUFFER, vertexSize, vertexData, 0);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl3.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementData, 0);
    }

    private void initVertexArray(GL4 gl4) {

        gl4.glCreateVertexArrays(1, vertexArrayName);

        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.POSITION, Semantic.Stream._0);
        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.POSITION, 4, GL_FLOAT, false, 0);
        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.POSITION);

        gl4.glVertexArrayElementBuffer(vertexArrayName.get(0), bufferName.get(Buffer.ELEMENT));
        gl4.glVertexArrayVertexBuffer(vertexArrayName.get(0), Semantic.Stream._0, bufferName.get(Buffer.VERTEX), 0, 
                Vec4.SIZE);
    }

    public void render(GL4 gl4) {

        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0);
        gl4.glBindVertexArray(0);
    }
}
