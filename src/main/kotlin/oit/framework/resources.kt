package oit.framework

import com.jogamp.common.nio.Buffers
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import glm.vec2.Vec2i
import glm.vec3.Vec3
import uno.buffer.byteBufferBig

/**
 * Created by elect on 05/05/2017.
 */


object resources {

    var useOQ = true

    var numPasses = 4
    var numGeoPasses = 0

    var backgroundColor = Vec3(1)

    var white = Vec3(1)
    var black = Vec3(0)

    var imageSize = Vec2i(1024, 768)

    lateinit var animator: Animator

    lateinit var glWindow: GLWindow

    var parameters = byteBufferBig(Float.BYTES * 2)

    var opacity = 0.6f
    var weight = 0.1f

    var matBuffer = GLBuffers.newDirectByteBuffer(Mat4.SIZE)

    var numLayers = (Resources.numPasses - 1) * 2

    var clearDepth = Buffers.newDirectFloatBuffer(1)
    var clearColor = Buffers.newDirectFloatBuffer(4)
}