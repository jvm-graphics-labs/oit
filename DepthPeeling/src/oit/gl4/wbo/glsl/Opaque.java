/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.wbo.glsl;

import javax.media.opengl.GL4;

/**
 *
 * @author gbarbieri
 */
public class Opaque extends glsl.GLSLProgramObject {

    private int modelToWorldUL;
    private int alphaUL;

    public Opaque(GL4 gl4, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int blockBinding) {

        super(gl4, shadersFilepath, vertexShaders, fragmentShaders);

        int projectionUBI = gl4.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl4.glUniformBlockBinding(getProgramId(), projectionUBI, blockBinding);

        alphaUL = gl4.glGetUniformLocation(getProgramId(), "alpha");

        modelToWorldUL = gl4.glGetUniformLocation(getProgramId(), "modelToWorld");

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
