/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dp.glsl;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class Blend extends glsl.GLSLProgramObject {

    private int modelToClipUL;
    private int tempTexUL;

    public Blend(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToClipUL = gl3.glGetUniformLocation(getProgramId(), "modelToClip");
        
        tempTexUL = gl3.glGetUniformLocation(getProgramId(), "tempTex");
        
        if (modelToClipUL == -1 || tempTexUL == -1 ) {
            System.out.println("[Blend] UL error");
        }
    }

    public Blend(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);
    }

    public int getModelToClipUL() {
        return modelToClipUL;
    }

    public int getTempTexUL() {
        return tempTexUL;
    }
}
