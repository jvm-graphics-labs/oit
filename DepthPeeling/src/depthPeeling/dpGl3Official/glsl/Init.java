/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3Official.glsl;

import com.jogamp.opengl.util.GLBuffers;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class Init extends glsl.GLSLProgramObject {

    private int alphaUL;
    private int modelToWorldUL;

    public Init(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int blockBinding) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, blockBinding);

        alphaUL = gl3.glGetUniformLocation(getProgramId(), "alpha");

        modelToWorldUL = gl3.glGetUniformLocation(getProgramId(), "modelToWorld");
    }

    public int getAlphaUL() {
        return alphaUL;
    }

    public int getModelToWorldUL() {
        return modelToWorldUL;
    }
}
