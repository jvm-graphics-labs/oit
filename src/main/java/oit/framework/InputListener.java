/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.framework;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import glm.vec2.Vec2;
import glm.vec2.Vec2i;
import glm.vec3.Vec3;
import oit.gl3.Viewer;

/**
 *
 * @author GBarbieri
 */
public class InputListener implements KeyListener, MouseListener {

    private boolean rotating = false, panning = false, scaling = false;
    public static Vec2 rot = new Vec2(20, 45f);
    public static Vec3 pos = new Vec3(0f, 0f, 3f);
    private Vec2i start = new Vec2i(), diff = new Vec2i();

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

        rotating = false;
        panning = false;
        scaling = false;

        switch (e.getButton()) {

            case MouseEvent.BUTTON1:
                rotating = true;
                break;
            case MouseEvent.BUTTON2:
                scaling = true;
                break;
            case MouseEvent.BUTTON3:
                panning = true;
                break;
        }
        start.set(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
 
   }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        diff.put(e.getX(), e.getY()).minus_(start);
        start.put(e.getX(), e.getY());

        float relX = diff.x / (float) Resources.imageSize.x;
        float relY = diff.y / (float) Resources.imageSize.y;

        if (rotating) {
            rot.y += relX * 180;
            rot.x += relY * 180;
        } else if (panning) {
            pos.x -= relX;
            pos.y += relY;
        } else if (scaling) {
            pos.z -= relY * pos.z;
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_Q:
                Resources.useOQ = !Resources.useOQ;
                break;
            case KeyEvent.VK_RIGHT:
                Resources.numPasses++;
                Resources.numLayers = (Resources.numPasses - 1) * 2;
                break;
            case KeyEvent.VK_LEFT:
                Resources.numPasses--;
                Resources.numPasses = Math.max(Resources.numPasses, 1);
                Resources.numLayers = (Resources.numPasses - 1) * 2;
                break;
            case KeyEvent.VK_B:
                Resources.backgroundColor = (Resources.backgroundColor.x == Resources.white.x)
                        ? Resources.black : Resources.white;
                break;
            case KeyEvent.VK_O:
//                Viewer.showOsd = !Viewer.showOsd;
                break;
            case KeyEvent.VK_1:
                Viewer.newOit = Viewer.Oit.DUAL_DEPTH_PEELING;
                break;
            case KeyEvent.VK_2:
                Viewer.newOit = Viewer.Oit.DEPTH_PEELING;
                break;
            case KeyEvent.VK_3:
                Viewer.newOit = Viewer.Oit.WEIGHTED_AVERAGE;
                break;
            case KeyEvent.VK_4:
                Viewer.newOit = Viewer.Oit.WEIGHTED_SUM;
                break;
            case KeyEvent.VK_5:
                Viewer.newOit = Viewer.Oit.WEIGHTED_BLENDED;
                break;
            case KeyEvent.VK_A:
                Resources.opacity -= 0.05f;
                Resources.opacity = (float) Math.max(Resources.opacity, 0.0);
                break;
            case KeyEvent.VK_D:
                Resources.opacity += 0.05f;
                Resources.opacity = (float) Math.min(Resources.opacity, 1.0);
                break;
            case KeyEvent.VK_W:
                Resources.weight -= 0.05f;
                Resources.weight = (float) Math.max(Resources.weight, 0.0);
                break;
            case KeyEvent.VK_S:
                Resources.weight += 0.05f;
                Resources.weight = (float) Math.min(Resources.weight, 1.0);
                break;
            case KeyEvent.VK_ESCAPE:
                Resources.animator.stop();
                break;
        }
    }
}
