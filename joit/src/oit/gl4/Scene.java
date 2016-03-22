/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author gbarbieri
 */
public class Scene {

    private Model model;
    private Mat4[] positions;
    private float[] opacities;
    private ByteBuffer modelBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE);

    public Scene(GL4 gl4, String filepath) throws IOException {

        model = new Model(gl4, filepath);

        Mat4 m0 = Mat4.translation(new Mat4(), +.1f, 0f, 0f);
        Mat4 m1 = Mat4.translation(new Mat4(), -.1f, 0f, 0f);
        Mat4 m2 = Mat4.translation(new Mat4(), 0f, 0f, +.1f);
        Mat4 m3 = Mat4.translation(new Mat4(), 0f, 0f, -.1f);
//        
//        Mat4 m0 = Mat4.translate(new Vec3(0f, 0f, 0f));
//        Mat4 m0 = Mat4.translate(new Vec3(.1f, 0f, 0f));
//        Mat4 m1 = Mat4.translate(new Vec3(-.1f, 0f, 0f));
//        Mat4 m2 = Mat4.translate(new Vec3(.3f, 0f, 0f));
//        Mat4 m3 = Mat4.translate(new Vec3(-.3f, 0f, 0f));
//        Mat4 m4 = Mat4.translate(new Vec3(0f, 0f, .1f));
//        Mat4 m5 = Mat4.translate(new Vec3(.2f, 0f, .1f));
//        Mat4 m6 = Mat4.translate(new Vec3(-.2f, 0f, .1f));
//        Mat4 m7 = Mat4.translate(new Vec3(-.2f, 0f, -.1f));
//        Mat4 m8 = Mat4.translate(new Vec3(0f, 0f, -.1f));
//        Mat4 m9 = Mat4.translate(new Vec3(.2f, 0f, -.1f));
//        Mat4 m10 = Mat4.translate(new Vec3(0f, 0f, .2f));
//        Mat4 m11 = Mat4.translate(new Vec3(0f, 0f, -.2f));
//
//        Mat4 m12 = Mat4.translate(new Vec3(.1f, .2f, 0f));
//        Mat4 m13 = Mat4.translate(new Vec3(-.1f, .2f, 0f));
//        Mat4 m14 = Mat4.translate(new Vec3(.3f, .2f, 0f));
//        Mat4 m15 = Mat4.translate(new Vec3(-.3f, .2f, 0f));
//        Mat4 m16 = Mat4.translate(new Vec3(0f, .2f, .1f));
//        Mat4 m17 = Mat4.translate(new Vec3(.2f, .2f, .1f));
//        Mat4 m18 = Mat4.translate(new Vec3(-.2f, .2f, .1f));
//        Mat4 m19 = Mat4.translate(new Vec3(-.2f, .2f, -.1f));
//        Mat4 m20 = Mat4.translate(new Vec3(0f, .2f, -.1f));
//        Mat4 m21 = Mat4.translate(new Vec3(.2f, .2f, -.1f));
//        Mat4 m22 = Mat4.translate(new Vec3(0f, .2f, .2f));
//        Mat4 m23 = Mat4.translate(new Vec3(0f, .2f, -.2f));
//
//        Mat4 m24 = Mat4.translate(new Vec3(.1f, -.2f, 0f));
//        Mat4 m25 = Mat4.translate(new Vec3(-.1f, -.2f, 0f));
//        Mat4 m26 = Mat4.translate(new Vec3(.3f, -.2f, 0f));
//        Mat4 m27 = Mat4.translate(new Vec3(-.3f, -.2f, 0f));
//        Mat4 m28 = Mat4.translate(new Vec3(0f, -.2f, .1f));
//        Mat4 m29 = Mat4.translate(new Vec3(.2f, -.2f, .1f));
//        Mat4 m30 = Mat4.translate(new Vec3(-.2f, -.2f, .1f));
//        Mat4 m31 = Mat4.translate(new Vec3(-.2f, -.2f, -.1f));
//        Mat4 m32 = Mat4.translate(new Vec3(0f, -.2f, -.1f));
//        Mat4 m33 = Mat4.translate(new Vec3(.2f, -.2f, -.1f));
//        Mat4 m34 = Mat4.translate(new Vec3(0f, -.2f, .2f));
//        Mat4 m35 = Mat4.translate(new Vec3(0f, -.2f, -.2f));

//        Mat4 m0 = Mat4.translate(new Vec3(.1f, 0f, 0f));
//        Mat4 m1 = Mat4.translate(new Vec3(-.1f, 0f, 0f));
//        Mat4 m2 = Mat4.translate(new Vec3(0f, 0f, .1f));
//        Mat4 m3 = Mat4.translate(new Vec3(0f, 0f, -.1f));
//        Mat4 m4 = Mat4.translate(new Vec3(.1f, .2f, 0f));
//        Mat4 m5 = Mat4.translate(new Vec3(-.1f, .2f, 0f));
//        Mat4 m6 = Mat4.translate(new Vec3(0f, .2f, .1f));
//        Mat4 m7 = Mat4.translate(new Vec3(0f, .2f, -.1f));
//
//        Mat4 m8 = Mat4.translate(new Vec3(.1f, -.2f, 0f));
//        Mat4 m9 = Mat4.translate(new Vec3(-.1f, -.2f, 0f));
//        Mat4 m10 = Mat4.translate(new Vec3(0f, -.2f, .1f));
//        Mat4 m11 = Mat4.translate(new Vec3(0f, -.2f, -.1f));
//        positions = new Mat4[]{m0};
        positions = new Mat4[]{m0, m1, m2, m3};
//        positions = new Mat4[]{m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11};
//        positions = new Mat4[]{m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, 
//            m19, m20, m21, m22, m23, m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35};
//        opacities = new float[]{.6f};
        opacities = new float[]{1f, 1f, .6f, .6f};
//        opacities = new float[]{1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f,
//            .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f};
    }

    public void render(GL4 gl4, int modelMatrixUL, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            gl4.glUniform1f(alphaUL, opacities[i]);
            gl4.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFa_(), 0);

            model.render(gl4);
        }
    }

    public void renderOpaque(GL4 gl4, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] == 1) {

                gl4.glUniform1f(alphaUL, 1);
                
                modelBuffer.asFloatBuffer().put(positions[i].toFa_());

                gl4.glNamedBufferSubData(
                        Viewer.bufferName.get(Viewer.Buffer.MODEL),
                        0,
                        glm.mat._4.Mat4.SIZE,
                        modelBuffer);

                gl4.glBindBufferBase(GL_UNIFORM_BUFFER,
                        Semantic.Uniform.TRANSFORM1,
                        Viewer.bufferName.get(Viewer.Buffer.MODEL));

                model.render(gl4);
            }
        }
    }

    public void renderWaTransparent(GL4 gl4, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] < 1) {

                gl4.glUniform1f(alphaUL, opacities[i]);

                modelBuffer.asFloatBuffer().put(positions[i].toFa_());

                gl4.glNamedBufferSubData(
                        Viewer.bufferName.get(Viewer.Buffer.MODEL),
                        0,
                        glm.mat._4.Mat4.SIZE,
                        modelBuffer);

                gl4.glBindBufferBase(GL_UNIFORM_BUFFER,
                        Semantic.Uniform.TRANSFORM1,
                        Viewer.bufferName.get(Viewer.Buffer.MODEL));

                model.render(gl4);
            }
        }
    }

}
