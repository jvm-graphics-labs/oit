/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ddp;

import javax.media.opengl.GL3;
import jglm.Vec2i;

/**
 *
 * @author gbarbieri
 */
public class DualDepthPeeling {
    
    private Vec2i imageSize;
    
    public DualDepthPeeling(GL3 gl3, Vec2i imageSize, int blockBinding) {
        
        this.imageSize = imageSize;
        
        initShaders(gl3, blockBinding);
    }
    
    private void initShaders(GL3 gl3, int blockBinding) {
        System.out.println("building shaders..");
        
        
    }
}
