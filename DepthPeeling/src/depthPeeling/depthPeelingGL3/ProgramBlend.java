
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
public class ProgramBlend extends glsl.GLSLProgramObject {

    private int tempTexUnLoc;
    private int modelToClipMatrixUnLoc;

    public ProgramBlend(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        tempTexUnLoc = gl3.glGetUniformLocation(getProgramId(), "TempTex");
        modelToClipMatrixUnLoc = gl3.glGetUniformLocation(getProgramId(), "modelToClipMatrix");
    }

    public int getTempTexUnLoc() {
        return tempTexUnLoc;
    }

    public int getModelToClipMatrixUnLoc() {
        return modelToClipMatrixUnLoc;
    }
}
