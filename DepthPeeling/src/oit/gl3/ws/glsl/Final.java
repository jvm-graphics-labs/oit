/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ws.glsl;

import com.jogamp.opengl.GL3;


/**
 *
 * @author gbarbieri
 */
public class Final extends glsl.GLSLProgramObject {

    private int modelToClipUL;
    private int colorTexUL;
    private int backgroundColorUL;

    public Final(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        modelToClipUL = gl3.glGetUniformLocation(getProgramId(), "modelToClip");

        colorTexUL = gl3.glGetUniformLocation(getProgramId(), "ColorTex");

        backgroundColorUL = gl3.glGetUniformLocation(getProgramId(), "BackgroundColor");

        if (colorTexUL == -1 || backgroundColorUL == -1 || modelToClipUL == -1) {
            System.out.println("[Final] UL error");
        }
    }

    public int getModelToClipUL() {
        return modelToClipUL;
    }

    public int getColorTexUL() {
        return colorTexUL;
    }

    public int getBackgroundColorUL() {
        return backgroundColorUL;
    }

}
