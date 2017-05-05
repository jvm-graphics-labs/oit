package oit.framework

import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.MouseEvent
import com.jogamp.newt.event.MouseListener
import glm.vec2.Vec2i

/**
 * Created by elect on 05/05/2017.
 */

class InputListener : KeyListener, MouseListener {

    private var rotating = false
    private var panning = false
    private var scaling = false
    private val start = Vec2i()
    private var diff = Vec2i()

    override fun mouseClicked(e: MouseEvent) {}

    override fun mouseEntered(e: MouseEvent) {}

    override fun mouseExited(e: MouseEvent) {}

    override fun mousePressed(e: MouseEvent) {

        rotating = false
        panning = false
        scaling = false

        when (e.getButton()) {

            MouseEvent.BUTTON1 -> rotating = true
            MouseEvent.BUTTON2 -> scaling = true
            MouseEvent.BUTTON3 -> panning = true
        }
        start.set(e.getX(), e.getY())
    }

    override fun mouseReleased(e: MouseEvent) {}

    override fun mouseMoved(e: MouseEvent) {}

    override fun mouseDragged(e: MouseEvent) {

        diff = Vec2i(e.x, e.y) - start
        start.set(e.x, e.y)

        val relX = diff.x / Resources.imageSize.x as Float
        val relY = diff.y / Resources.imageSize.y as Float

        if (rotating) {
            rot.y += relX * 180
            rot.x += relY * 180
        } else if (panning) {
            pos.x -= relX
            pos.y += relY
        } else if (scaling) {
            pos.z -= relY * pos.z
        }
    }

    fun mouseWheelMoved(e: MouseEvent) {

    }

    fun keyPressed(e: KeyEvent) {

    }

    fun keyReleased(e: KeyEvent) {

        when (e.getKeyCode()) {

            KeyEvent.VK_Q -> Resources.useOQ = !Resources.useOQ
            KeyEvent.VK_RIGHT -> {
                Resources.numPasses++
                Resources.numLayers = (Resources.numPasses - 1) * 2
            }
            KeyEvent.VK_LEFT -> {
                Resources.numPasses--
                Resources.numPasses = Math.max(Resources.numPasses, 1)
                Resources.numLayers = (Resources.numPasses - 1) * 2
            }
            KeyEvent.VK_B -> Resources.backgroundColor = if (Resources.backgroundColor.x === Resources.white.x)
                Resources.black
            else
                Resources.white
            KeyEvent.VK_O -> {
            }
            KeyEvent.VK_1 -> Viewer.newOit = Viewer.Oit.DUAL_DEPTH_PEELING
            KeyEvent.VK_2 -> Viewer.newOit = Viewer.Oit.DEPTH_PEELING
            KeyEvent.VK_3 -> Viewer.newOit = Viewer.Oit.WEIGHTED_AVERAGE
            KeyEvent.VK_4 -> Viewer.newOit = Viewer.Oit.WEIGHTED_SUM
            KeyEvent.VK_5 -> Viewer.newOit = Viewer.Oit.WEIGHTED_BLENDED
            KeyEvent.VK_A -> {
                Resources.opacity -= 0.05f
                Resources.opacity = Math.max(Resources.opacity, 0.0) as Float
            }
            KeyEvent.VK_D -> {
                Resources.opacity += 0.05f
                Resources.opacity = Math.min(Resources.opacity, 1.0) as Float
            }
            KeyEvent.VK_W -> {
                Resources.weight -= 0.05f
                Resources.weight = Math.max(Resources.weight, 0.0) as Float
            }
            KeyEvent.VK_S -> {
                Resources.weight += 0.05f
                Resources.weight = Math.min(Resources.weight, 1.0) as Float
            }
            KeyEvent.VK_ESCAPE -> Resources.animator.stop()
        }//                Viewer.showOsd = !Viewer.showOsd;
    }

    companion object {
        var rot = Vec2(20, 45f)
        var pos = Vec3(0, 0, 3)
    }
}
