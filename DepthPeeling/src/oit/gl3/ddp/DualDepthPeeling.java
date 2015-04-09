/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ddp;

import javax.media.opengl.GL3;
import jglm.Vec2i;
import oit.gl3.Scene;
import oit.gl3.ddp.glsl.Init;

/**
 *
 * @author gbarbieri
 */
public class DualDepthPeeling {

    private Vec2i imageSize;
    private Init init;
    private int[] sampler;
    private int[] queryId;

    public DualDepthPeeling(GL3 gl3, Vec2i imageSize, int blockBinding) {

        this.imageSize = imageSize;

        initShaders(gl3, blockBinding);
        
        initSampler(gl3);
        
        initQuery(gl3);
        
    }
    
    public void render(GL3 gl3, Scene scene) {
        
    }

    private void initShaders(GL3 gl3, int blockBinding) {
        System.out.println("building shaders..");

        String shadersFilepath = "/oit/gl3/ddp/glsl/shaders/";

        init = new Init(gl3, shadersFilepath, new String[]{"init_VS.glsl"},
                new String[]{"init_FS.glsl"}, blockBinding);
    }
    
    private void initSampler(GL3 gl3) {

        sampler = new int[1];
        gl3.glGenSamplers(1, sampler, 0);

        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
    }

    private void initQuery(GL3 gl3) {

        queryId = new int[1];
        gl3.glGenQueries(1, queryId, 0);
    }
}
