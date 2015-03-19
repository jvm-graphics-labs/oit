/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3Official.glsl;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class Simple extends glsl.GLSLProgramObject {

    private int modelToWorldUL;

    public Simple(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int blockBinding) {
        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, blockBinding);

        modelToWorldUL = gl3.glGetUniformLocation(getProgramId(), "modelToWorld");
    }

    public int getModelToWorldUL() {
        return modelToWorldUL;
    }

}
