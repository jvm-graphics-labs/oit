/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.wbo.glsl;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class Opaque extends glsl.GLSLProgramObject {

    private int modelToWorldUL;
    private int alphaUL;

    public Opaque(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int blockBinding) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, blockBinding);

        alphaUL = gl3.glGetUniformLocation(getProgramId(), "alpha");

        modelToWorldUL = gl3.glGetUniformLocation(getProgramId(), "modelToWorld");

        if (projectionUBI == -1 || alphaUL == -1 || modelToWorldUL == -1) {
            System.out.println("[Opaque] UL error");
        }
    }

    public int getModelToWorldUL() {
        return modelToWorldUL;
    }

    public int getAlphaUL() {
        return alphaUL;
    }

}
