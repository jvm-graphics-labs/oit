/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import glutil.ViewPole;

/**
 *
 * @author gbarbieri
 */
public class InputListener implements MouseListener, KeyListener {

    private ViewPole viewPole;

    public InputListener(ViewPole viewPole) {
        this.viewPole = viewPole;
    }

    @Override
    public void mouseClicked(MouseEvent me) {

    }

    @Override
    public void mouseEntered(MouseEvent me) {

    }

    @Override
    public void mouseExited(MouseEvent me) {

    }

    @Override
    public void mousePressed(MouseEvent me) {
        viewPole.mousePressed(me);
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        viewPole.mouseReleased(me);
    }

    @Override
    public void mouseMoved(MouseEvent me) {

    }

    @Override
    public void mouseDragged(MouseEvent me) {
        viewPole.mouseMoved(me);
    }

    @Override
    public void mouseWheelMoved(MouseEvent me) {
        /**
         * It works currently only in perspective.
         *
         * additional me1 to make zooming acceptable faster
         */
        float[] newRotation = me.getRotation();
        for (int i = 0; i < newRotation.length; i++) {
            newRotation[i] *= 100;
        }
        MouseEvent me1 = new MouseEvent(me.getEventType(), me.getSource(), me.getWhen(), me.getModifiers(),
                me.getX(), me.getY(), me.getClickCount(), me.getButton(), newRotation, me.getRotationScale());
        viewPole.mouseWheelMoved(me);
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        if(ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
            Viewer.animator.stop();
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
    
    }

}
