
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramFinal extends glsl.GLSLProgramObject {

    private int colorTexUnLoc;
    private int backgroundColorUnLoc;
    private int modelToClipMatrixUnLoc;

    public ProgramFinal(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        colorTexUnLoc = gl3.glGetUniformLocation(getProgramId(), "ColorTex");
        backgroundColorUnLoc = gl3.glGetUniformLocation(getProgramId(), "BackgroundColor");
        modelToClipMatrixUnLoc = gl3.glGetUniformLocation(getProgramId(), "modelToClipMatrix");
    }

    public int getModelToClipMatrixUnLoc() {
        return modelToClipMatrixUnLoc;
    }

    public int getColorTexUnLoc() {
        return colorTexUnLoc;
    }

    public int getBackgroundColorUnLoc() {
        return backgroundColorUnLoc;
    }
}
