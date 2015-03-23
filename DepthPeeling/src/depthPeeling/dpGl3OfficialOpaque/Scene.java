/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3OfficialOpaque;

import java.io.IOException;
import javax.media.opengl.GL3;
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

    public Scene(GL3 gl3, String filepath) throws IOException {

        model = new Model(gl3, filepath);

        Mat4 m0 = Mat4.translate(new Vec3(.1f, 0f, 0f));
        Mat4 m1 = Mat4.translate(new Vec3(-.1f, 0f, 0f));
        Mat4 m2 = Mat4.translate(new Vec3(0f, 0f, .1f));
        Mat4 m3 = Mat4.translate(new Vec3(0f, 0f, -.1f));

        positions = new Mat4[]{m0, m1, m2, m3};

        opacities = new float[]{1f, 1f, .6f, .6f};
    }

    public void renderOpaque(GL3 gl3, int modelMatrixUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] == 1) {

                gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);

                model.render(gl3);
            }
        }
    }

    public void renderTransparent(GL3 gl3, int modelMatrixUL, int alphaUL) {

        for (int i = 0; i < positions.length; i++) {

            if (opacities[i] < 1) {

                gl3.glUniform1f(alphaUL, opacities[i]);
                gl3.glUniformMatrix4fv(modelMatrixUL, 1, false, positions[i].toFloatArray(), 0);

                model.render(gl3);
            }
        }
    }
}
