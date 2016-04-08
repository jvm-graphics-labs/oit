/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import oit.gl3.dp.DepthPeeling;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL3;
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
    private ByteBuffer modelBuffer = GLBuffers.newDirectByteBuffer(glm.mat._4.Mat4.SIZE),
            alphaBuffer = GLBuffers.newDirectByteBuffer(Float.BYTES);

    public Scene(GL3 gl3) throws IOException {

        model = new Model(gl3);

//        Mat4 m0 = Mat4.translate(new Vec3(0f, 0f, 0f));
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
//        positions = new Mat4[]{m0, m1, m2, m3};
        positions = new Mat4[]{new Mat4(1)};
//        positions = new Mat4[]{m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11};
        opacities = new float[]{.6f};
//        opacities = new float[]{1f, 1f, .5f, .5f};
//        opacities = new float[]{1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f};
    }

    public void render(GL3 gl3, int modelMatrixUL, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            gl3.glUniform1f(alphaUL, opacities[i]);
//            gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);
            gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFa_(), 0);

            model.render(gl3);
        }
    }

    public void renderOpaque(GL3 gl3, int modelMatrixUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] == 1) {

//                gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);
                gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFa_(), 0);

                model.render(gl3);
            }
        }
    }

    public void renderTransparent(GL3 gl3, int modelMatrixUL, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] < 1) {

                gl3.glUniform1f(alphaUL, opacities[i]);
//                gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);
                gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFa_(), 0);

                model.render(gl3);
            }
        }
    }

    public void render(GL3 gl3) {

        for (int i = 0; i < positions.length; i++) {

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1));
            modelBuffer.asFloatBuffer().put(positions[i].toFa_());
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, glm.mat._4.Mat4.SIZE, modelBuffer);

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, DepthPeeling.bufferName.get(0));
            alphaBuffer.asFloatBuffer().put(opacities[i]);
            gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Float.BYTES, alphaBuffer);

            model.render(gl3);
        }
    }

    public void renderOpaque(GL3 gl3) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] == 1) {

                gl3.glBindBuffer(GL_UNIFORM_BUFFER, Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1));
//                modelBuffer.asFloatBuffer().put(positions[i].toFloatArray());
                modelBuffer.asFloatBuffer().put(positions[i].toFa_());
                gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, glm.mat._4.Mat4.SIZE, modelBuffer);

                model.render(gl3);
            }
        }
    }

    public void renderTransparent(GL3 gl3) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] < 1) {

//                modelBuffer.asFloatBuffer().put(positions[i].toFloatArray());
                Mat4 mat = new Mat4()
//                        .rotate((float) Math.toRadians(0f), 1, 0, 0)
//                        .rotate((float) Math.toRadians(45f), 0, 1, 0)
//                        .translate(0.03f, -0.70f, 0.03f)
//                        .scale(6f)
                        ;
                modelBuffer.asFloatBuffer().put(mat.toFa_());
//                System.out.println("model");
//                for (int j = 0; j < 16; j++) {
//                    System.out.println("" + modelBuffer.getFloat());
//                }
//                modelBuffer.rewind();
                gl3.glBindBuffer(GL_UNIFORM_BUFFER, Viewer.bufferName.get(Viewer.Buffer.TRANSFORM1));
                gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, glm.mat._4.Mat4.SIZE, modelBuffer);

                alphaBuffer.asFloatBuffer().put(opacities[i]);
                gl3.glBindBuffer(GL_UNIFORM_BUFFER, DepthPeeling.bufferName.get(0));
                gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Float.BYTES, alphaBuffer);

                model.render(gl3);
            }
        }
    }

}
