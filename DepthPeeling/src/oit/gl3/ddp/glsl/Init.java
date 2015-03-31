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
public class Init extends glsl.GLSLProgramObject{

    public Init(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int blockbinding) {
        
        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);
        
        init(gl3, blockbinding);
    }
    
    private void init(GL3 gl3, int blockBinding) {
        
        
    }
}
