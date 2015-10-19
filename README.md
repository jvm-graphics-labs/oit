depthPeeling
============

Repository containing JOGL examples regarding the Depth Peeling technique on old (GL2) and modern (GL3/GL4) OpenGL

It requires:

- the Jglm project https://github.com/elect86/Jglm
- the java unofficial opengl SDK https://github.com/elect86/java-unofficial-opengl-SDK


[`depthPeeling`](https://github.com/elect86/depthPeeling/tree/master/DepthPeeling/src/depthPeeling) package represents some earlier attempts of mine

What it is important is the [`oit`](https://github.com/elect86/depthPeeling/tree/master/DepthPeeling/src/oit) one, (Order Independent Transparency).

Inside you can find under [`originalBavoilMyers`](https://github.com/elect86/depthPeeling/tree/master/DepthPeeling/src/oit/originalBavoilMyers) the original program of Louis Bavoil and Kevin Myers, they work is the ["Order Independent Transparency with Dual Depth Peeling"](http://developer.download.nvidia.com/SDK/10/opengl/src/dual_depth_peeling/doc/DualDepthPeeling.pdf) paper. I have slighlty modified it, just the minimum to get it working.

Package `oit.gl3` is instead my current (partial) `GL3` rewriting, I divided all the different methods of their program in order to make it more readable, as following:

- `ddp` standard Dual Depth Peeling
- `dp` standard Depth Peeling 
- `dpo` optimized Depth Peeling, called Depth Peeling Opaque, you render everything opaque (with `alpha`==1) normally and then you apply the DP only to the remaining meshes having `alpha` smaller than one, [0, 1).
- `ws` standard Weighted Sum
- `wa` standard Weighted Average

The newest algorithm of all is the Weighted Blended, OIT much faster! Here some interesting links if you want to deepen:

- [McGuire and Bavoil, Weighted Blended Order-Independent Transparency, Journal of Computer Graphics Techniques (JCGT), vol. 2, no. 2, 122--141, 2013](http://jcgt.org/published/0002/02/09/)
- [Full-Text High-Resolution PDF](http://jcgt.org/published/0002/02/09/paper.pdf)
- [Nvidia Weighted Blended OIT Sample](http://docs.nvidia.com/gameworks/content/gameworkslibrary/graphicssamples/opengl_samples/weightedblendedoitsample.htm), Graphics and Compute Samples

If you have `GL4`, you can look for the fastest standard `oit.gl4.wb` algorithm or, otherwise, you can still implement in `GL3` at a small addition cost, `oit.gl3.wb`.
Both `wbo` versions refers to the corresponding optimized algorithm that renders first opaque meshes.
