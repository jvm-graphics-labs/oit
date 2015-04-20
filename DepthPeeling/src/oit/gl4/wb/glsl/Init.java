/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.wb.glsl;

import com.jogamp.opengl.GL4;


/**
 *
 * @author gbarbieri
 */
public class Init extends glsl.GLSLProgramObject {

    private int alphaUL;
    private int modelToWorldUL;
    private int depthScaleUL;

    public Init(GL4 gl4, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int blockBinding) {

        super(gl4, shadersFilepath, vertexShaders, fragmentShaders);

        int projectionUBI = gl4.glGetUniformBlockIndex(getProgramId(), "vpMatrixes");
        gl4.glUniformBlockBinding(getProgramId(), projectionUBI, blockBinding);

        alphaUL = gl4.glGetUniformLocation(getProgramId(), "alpha");

        modelToWorldUL = gl4.glGetUniformLocation(getProgramId(), "modelToWorld");

        depthScaleUL = gl4.glGetUniformLocation(getProgramId(), "depthScale");

        if (projectionUBI == -1 || alphaUL == -1 || modelToWorldUL == -1 || depthScaleUL == -1) {
            System.out.println("[Init] UL error");
        }
    }

    public int getAlphaUL() {
        return alphaUL;
    }

    public int getModelToWorldUL() {
        return modelToWorldUL;
    }

    public int getDepthScaleUL() {
        return depthScaleUL;
    }
}
