/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demos.dualDepthPeeling;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import static demos.dualDepthPeeling.Viewer.backgroundColor;
import static demos.dualDepthPeeling.Viewer.g_black;
import static demos.dualDepthPeeling.Viewer.g_white;
import static demos.dualDepthPeeling.Viewer.numPasses;
import static demos.dualDepthPeeling.Viewer.opacity;
import static demos.dualDepthPeeling.Viewer.useOQ;
import glm.vec._2.i.Vec2i;

/**
 *
 * @author GBarbieri
 */
public class InputListener implements KeyListener, MouseListener {

    private boolean rotating = false, panning = false, scaling = false;
    private Vec2i start = new Vec2i(), diff = new Vec2i();

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_8:
                Viewer.showUI = !Viewer.showUI;
                break;
            case KeyEvent.VK_Q:
                useOQ = !useOQ;
                break;
            case KeyEvent.VK_PLUS:
                numPasses++;
                break;
            case KeyEvent.VK_MINUS:
                numPasses--;
                break;
            case KeyEvent.VK_B:
                backgroundColor = (backgroundColor == g_white) ? g_black : g_white;
                break;
            case KeyEvent.VK_O:
                Viewer.showOsd = !Viewer.showOsd;
                break;
            case KeyEvent.VK_R:
//                ReloadShaders(gl2);
                Viewer.scale = Model.scale;
                Viewer.trans = Model.trans;
                Viewer.rot = new float[]{0.0f, 45.0f};
                Viewer.pos = new float[]{0.0f, 0.0f, 2.0f};
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
                opacity[0] -= 0.05;
                opacity[0] = (float) Math.max(opacity[0], 0.0);
                break;
            case KeyEvent.VK_D:
                opacity[0] += 0.05;
                opacity[0] = (float) Math.min(opacity[0], 1.0);
                break;
            case KeyEvent.VK_ESCAPE:
                Viewer.animator.stop();
                break;
        }
    }

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

        float relX = diff.x / (float) Viewer.imageSize.x;
        float relY = diff.y / (float) Viewer.imageSize.y;

        if (rotating) {
            Viewer.rot[1] += relX * 180;
            Viewer.rot[0] += relY * 180;
        } else if (panning) {
            Viewer.pos[0] -= relX;
            Viewer.pos[1] += relY;
        } else if (scaling) {
            Viewer.pos[2] -= relY * Viewer.pos[2];
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

}
