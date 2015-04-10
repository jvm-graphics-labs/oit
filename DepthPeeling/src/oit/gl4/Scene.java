/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import java.io.IOException;
import javax.media.opengl.GL4;
import jglm.Mat4;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class Scene {

    private Model model;
    private Mat4[] positions;
    private float[] opacities;

    public Scene(GL4 gl4, String filepath) throws IOException {

        model = new Model(gl4, filepath);

        Mat4 m0 = Mat4.translate(new Vec3(0f, 0f, 0f));

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
        positions = new Mat4[]{m0};
//        positions = new Mat4[]{m0, m1, m2, m3};
//        positions = new Mat4[]{m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11};
        opacities = new float[]{.6f};
//        opacities = new float[]{1f, 1f, .6f, .6f};
//        opacities = new float[]{1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f, 1f, 1f, .6f, .6f};
    }

    public void render(GL4 gl4, int modelMatrixUL, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            gl4.glUniform1f(alphaUL, opacities[i]);
            gl4.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);

            model.render(gl4);
        }
    }

    public void renderDpOpaque(GL4 gl4, int modelMatrixUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] == 1) {

                gl4.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);

                model.render(gl4);
            }
        }
    }

    public void renderDpoTransparent(GL4 gl4, int modelMatrixUL, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] < 1) {

                gl4.glUniform1f(alphaUL, opacities[i]);
                gl4.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);

                model.render(gl4);
            }
        }
    }

}
