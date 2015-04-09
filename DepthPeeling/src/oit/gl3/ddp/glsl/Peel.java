/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ddp.glsl;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class Peel extends glsl.GLSLProgramObject{

    public Peel(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {
        
        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);
    }
    
}
