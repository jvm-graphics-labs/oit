/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dpo.glsl;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class Opaque extends glsl.GLSLProgramObject {

    private int modelToWorldUL;

    public Opaque(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int blockBinding) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, blockBinding);

        modelToWorldUL = gl3.glGetUniformLocation(getProgramId(), "modelToWorld");

        if (projectionUBI == -1 || modelToWorldUL == -1) {
            System.out.println("[Opaque] UL error");
        }
    }

    public int getModelToWorldUL() {
        return modelToWorldUL;
    }
}
