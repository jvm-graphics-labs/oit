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
public class Final extends glsl.GLSLProgramObject {

    private int modelToClipUL;
    private int colorTex0UL;
    private int colorTex1UL;
    private int opaqueColorTexUL;

    public Final(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        modelToClipUL = gl3.glGetUniformLocation(getProgramId(), "modelToClip");

        colorTex0UL = gl3.glGetUniformLocation(getProgramId(), "ColorTex0");

        colorTex1UL = gl3.glGetUniformLocation(getProgramId(), "ColorTex1");

        opaqueColorTexUL = gl3.glGetUniformLocation(getProgramId(), "opaqueColorTex");

        if (colorTex0UL == -1 || colorTex1UL == -1 || opaqueColorTexUL == -1 || modelToClipUL == -1) {
            System.out.println("[Final] UL error");
        }
    }

    public int getModelToClipUL() {
        return modelToClipUL;
    }

    public int getColorTex0UL() {
        return colorTex0UL;
    }

    public int getOpaqueColorTexUL() {
        return opaqueColorTexUL;
    }

    public int getColorTex1UL() {
        return colorTex1UL;
    }

}