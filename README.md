OIT
============

Repository containing JOGL examples regarding the OIT techniques on old (GL3) and modern (GL4) OpenGL

It requires:

- the Jglm project https://github.com/elect86/Jglm
- the java unofficial opengl SDK https://github.com/elect86/java-unofficial-opengl-SDK

I modified the code through:
- a general cleaning of unused objects (like `g_dualBackBlenderFboId` inside Dual Depth Peeling)
- a substitution of Nvidia enums with standard ones (like `GL_FLOAT_RG32_NV` with `GL_RG32F`)
- specifying the right number of components on server side (like `GL_RGB` with `GL_RG`) and on client side (

      like `out vec2 outputColor;` 
      
      instead `out vec4 outputColor;`)
- specifying always everything, leaving nothing to be padded by the driver (

      like `outputColor.rgb = frontColor.rgb + opaqueColor * frontColor.a;` 
  
      with `outputColor = vec4(frontColor.rgb + opaqueColor * frontColor.a, 1);`)
- implementation of the OIT methods over plain opaque geometry rendered normally
- improving of dual depth peeling and depth peeling algorithms by performing the occulsion query also at the init stage in order to save potentially an additional geometric pass.

Package [`gl3`](https://github.com/elect86/depthPeeling/tree/master/DepthPeeling/src/oit/gl3) is instead my current `GL3` rewriting, I divided all the different methods of their program in order to make it more readable, as following:

- `ddp` -> Dual Depth Peeling
- `dp` -> Depth Peeling 
- `ws` -> Weighted Sum
- `wa` -> Weighted Average
- `wb` -> Weighted Blended

The newest algorithm of all is the Weighted Blended, OIT much faster! Here some interesting links if you want to deepen:

- [McGuire and Bavoil, Weighted Blended Order-Independent Transparency, Journal of Computer Graphics Techniques (JCGT), vol. 2, no. 2, 122--141, 2013](http://jcgt.org/published/0002/02/09/)
- [Full-Text High-Resolution PDF](http://jcgt.org/published/0002/02/09/paper.pdf)
- [Nvidia Weighted Blended OIT Sample](http://docs.nvidia.com/gameworks/content/gameworkslibrary/graphicssamples/opengl_samples/weightedblendedoitsample.htm), Graphics and Compute Samples

If you have `GL4`, you can look for the fastest `wb` algorithm in the [`gl4`](https://github.com/elect86/depthPeeling/tree/master/DepthPeeling/src/oit/gl4) package or, otherwise, you can still implement in `GL3` at a small addition cost, `oit.gl3.wb`.


Under [`originalBavoilMyers`](https://github.com/elect86/depthPeeling/tree/master/DepthPeeling/src/oit/originalBavoilMyers) you can find the original program of Louis Bavoil and Kevin Myers, they work is the ["Order Independent Transparency with Dual Depth Peeling"](http://developer.download.nvidia.com/SDK/10/opengl/src/dual_depth_peeling/doc/DualDepthPeeling.pdf) paper. I have slighlty modified it, just the minimum to get it working.
