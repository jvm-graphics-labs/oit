/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import glm.vec._2.Vec2;
import glm.vec._2.i.Vec2i;
import glm.vec._3.Vec3;

/**
 *
 * @author GBarbieri
 */
public class InputListener implements KeyListener, MouseListener {

    private boolean rotating = false, panning = false, scaling = false;
    public static Vec2 rot = new Vec2(0, 45f);
    public static Vec3 pos = new Vec3(0, 0, 2);
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
        
        diff.set(e.getX(), e.getY()).sub(start);
        start.set(e.getX(), e.getY());

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
            case KeyEvent.VK_PLUS:
                Resources.numPasses++;
                break;
            case KeyEvent.VK_MINUS:
                Resources.numPasses--;
                break;
            case KeyEvent.VK_B:
                Resources.backgroundColor = (Resources.backgroundColor.equal(Resources.white).all())
                        ? Resources.black : Resources.white;
                break;
            case KeyEvent.VK_O:
//                Viewer.showOsd = !Viewer.showOsd;
                break;
            case KeyEvent.VK_1:
//                oit = Viewer.Oit.DUAL_DEPTH_PEELING;
                break;
            case KeyEvent.VK_2:
//                oit = Viewer.Oit.DEPTH_PEELING;
                break;
            case KeyEvent.VK_3:
//                oit = Viewer.Oit.WEIGHTED_AVERAGE;
                break;
            case KeyEvent.VK_4:
//                oit = Viewer.Oit.WEIGHTED_SUM;
                break;
            case KeyEvent.VK_A:
//                opacity[0] -= 0.05;
//                opacity[0] = (float) Math.max(opacity[0], 0.0);
                break;
            case KeyEvent.VK_D:
//                opacity[0] += 0.05;
//                opacity[0] = (float) Math.min(opacity[0], 1.0);
                break;
            case KeyEvent.VK_ESCAPE:
                Resources.animator.stop();
                break;
        }
    }
}
