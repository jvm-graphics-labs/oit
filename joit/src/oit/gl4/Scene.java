/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat._4.Mat4;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gbarbieri
 */
public class Scene {

    private ModelGL4 model;
    private Mat4[] opaques;
    private Mat4[] transparents;
    private ByteBuffer modelBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);

    public Scene(GL4 gl4) {

        try {
            model = new ModelGL4(gl4);
        } catch (IOException ex) {
            Logger.getLogger(Scene.class.getName()).log(Level.SEVERE, null, ex);
        }

        opaques = new Mat4[]{
            new Mat4()
//            new Mat4().translate(new Vec3(-.1f, 0f, 0f)),
//            new Mat4().translate(new Vec3(+.1f, 0f, 0f))
        };
        transparents = new Mat4[]{
            new Mat4()
//            new Mat4().translate(new Vec3(0f, 0f, -.1f)),
//            new Mat4().translate(new Vec3(0f, 0f, +.1f))
        };
    }

    public void renderOpaque(GL4 gl4) {

        for (int i = 0; i < opaques.length; i++) {

            opaques[i].toDbb(modelBuffer);

            gl4.glNamedBufferSubData(
                    Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1),
                    0,
                    glm.mat._4.Mat4.SIZE,
                    modelBuffer);

            model.render(gl4);
        }
    }

    public void renderTransparent(GL4 gl4) {

        for (int i = 0; i < transparents.length; i++) {

            transparents[i].toDbb(modelBuffer);

            gl4.glNamedBufferSubData(
                    Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1),
                    0,
                    glm.mat._4.Mat4.SIZE,
                    modelBuffer);
            
            model.render(gl4);
        }
    }
}
