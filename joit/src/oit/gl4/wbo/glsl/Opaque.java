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
public class Opaque extends glsl.GLSLProgramObject {

    private int alphaUL;

    public Opaque(GL4 gl4, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int blockBinding) {

        super(gl4, shadersFilepath, vertexShaders, fragmentShaders);
        
        alphaUL = gl4.glGetUniformLocation(getProgramId(), "alpha");


        if (alphaUL == -1) {
            System.out.println("[Opaque] UL error");
        }
    }

    public int getAlphaUL() {
        return alphaUL;
    }

}
