/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dp.glsl;

import com.jogamp.opengl.GL3;


/**
 *
 * @author gbarbieri
 */
public class Final extends glsl.GLSLProgramObject {

    private int colorTexUL;
    private int backgroundColorUL;
    private int modelToClipUL;

    public Final(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        init(gl3);
    }

    public Final(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        init(gl3);
    }

    private void init(GL3 gl3) {

        modelToClipUL = gl3.glGetUniformLocation(getProgramId(), "modelToClip");

        colorTexUL = gl3.glGetUniformLocation(getProgramId(), "colorTex");

        backgroundColorUL = gl3.glGetUniformLocation(getProgramId(), "backgroundColor");

        if (modelToClipUL == -1 || colorTexUL == -1 || backgroundColorUL == -1) {
            System.out.println("[Final] UL error");
        }
    }

    public int getColorTexUL() {
        return colorTexUL;
    }

    public int getModelToClipUL() {
        return modelToClipUL;
    }

    public int getBackgroundColorUL() {
        return backgroundColorUL;
    }
}
