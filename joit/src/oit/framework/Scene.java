/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.framework;

import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import java.io.IOException;
import java.nio.ByteBuffer;
import oit.gl3.Viewer;

/**
 *
 * @author gbarbieri
 */
public class Scene {

    private Model model;
    private Mat4[] opaques;
    private Mat4[] transparents;
    private ByteBuffer modelBuffer = GLBuffers.newDirectByteBuffer(glm.mat._4.Mat4.SIZE);

    public Scene(GL3 gl3, Model model) throws IOException {

        this.model = model;

        opaques = new Mat4[]{
            new Mat4().translate(new Vec3(-.1f, 0f, 0f)),
            new Mat4().translate(new Vec3(+.1f, 0f, 0f))
        };
        transparents = new Mat4[]{
            new Mat4().translate(new Vec3(0f, 0f, -.1f)),
            new Mat4().translate(new Vec3(0f, 0f, +.1f))
        };
    }

    public void renderOpaque(GL3 gl3) {

        for (Mat4 opaque : opaques) {

            opaque.toDbb(modelBuffer);
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1));
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, modelBuffer);

            model.render(gl3);
            Resources.numGeoPasses++;
        }
    }

    public void renderTransparent(GL3 gl3) {

        for (Mat4 transparent : transparents) {
            
            transparent.toDbb(modelBuffer);
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1));
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, modelBuffer);

            model.render(gl3);
            Resources.numGeoPasses++;
        }
    }
}
