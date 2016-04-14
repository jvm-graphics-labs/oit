/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import com.jogamp.common.nio.Buffers;
import static com.jogamp.opengl.GL.*;
import com.jogamp.opengl.GL3;
import java.io.IOException;
import oit.framework.Model;

/**
 *
 * @author gbarbieri
 */
public final class ModelGL3 extends Model {

    public ModelGL3(GL3 gl3) throws IOException {
        super(gl3);
    }

    @Override
    protected void initBuffers(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, getCompiledVertexCount() * Float.BYTES, vertices, GL_STATIC_DRAW);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, getCompiledIndexCount() * Integer.BYTES, indices, GL_STATIC_DRAW);
    }

    @Override
    protected void initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));

            int stride = vtxSize * Buffers.SIZEOF_FLOAT;
            int offset = pOffset;
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, stride, offset);
            offset = nOffset * Buffers.SIZEOF_FLOAT;
            gl3.glVertexAttribPointer(Semantic.Attr.NORMAL, 3, GL_FLOAT, false, stride, offset);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.NORMAL);

            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl3.glBindVertexArray(0);
    }

    public void render(GL3 gl3) {
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glDrawElements(GL_TRIANGLES, getCompiledIndexCount(), GL_UNSIGNED_INT, 0);
        }
        gl3.glBindVertexArray(0);
    }
}
