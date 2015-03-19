/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3Official.glsl;

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
