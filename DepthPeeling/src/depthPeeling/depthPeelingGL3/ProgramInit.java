/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3;

import com.jogamp.opengl.GL3;



/**
 *
 * @author gbarbieri
 */
public class ProgramInit extends glsl.GLSLProgramObject {

    private int alphaUnLoc;
    private int texture0UL;
    private int enableTextureUL;

    public ProgramInit(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int mvpMatrixUBB) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        alphaUnLoc = gl3.glGetUniformLocation(getProgramId(), "alpha");
        texture0UL = gl3.glGetUniformLocation(getProgramId(), "texture0");
        enableTextureUL = gl3.glGetUniformLocation(getProgramId(), "enableTexture");
//        System.out.println("texture0UL " + texture0UL);
//        System.out.println("enableTextureUL " + enableTextureUL);
        int mvpMatrixUBI = gl3.glGetUniformBlockIndex(getProgramId(), "mvpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), mvpMatrixUBI, mvpMatrixUBB);
    }

    public int getAlphaUnLoc() {
        return alphaUnLoc;
    }

    public int getTexture0UL() {
        return texture0UL;
    }

    public int getEnableTextureUL() {
        return enableTextureUL;
    }
}
