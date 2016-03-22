/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4.wbo.glsl;

import com.jogamp.opengl.GL4;

/**
 *
 * @author gbarbieri
 */
public class Init extends glsl.GLSLProgramObject {

    private int alphaUL;
    private int depthScaleUL;
    private int opaqueDepthTexUL;

    public Init(GL4 gl4, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int blockBinding) {

        super(gl4, shadersFilepath, vertexShaders, fragmentShaders);

        alphaUL = gl4.glGetUniformLocation(getProgramId(), "alpha");

        depthScaleUL = gl4.glGetUniformLocation(getProgramId(), "depthScale");

        opaqueDepthTexUL = gl4.glGetUniformLocation(getProgramId(), "opaqueDepthTex");

        if (alphaUL == -1 || depthScaleUL == -1 || opaqueDepthTexUL == -1) {
            System.out.println("[Init] UL error");
        }
    }

    public int getAlphaUL() {
        return alphaUL;
    }

    public int getDepthScaleUL() {
        return depthScaleUL;
    }

    public int getOpaqueDepthTexUL() {
        return opaqueDepthTexUL;
    }
}
