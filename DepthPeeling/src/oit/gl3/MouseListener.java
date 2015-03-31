/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import com.jogamp.newt.event.MouseEvent;
import glutil.ViewPole;

/**
 *
 * @author gbarbieri
 */
public class MouseListener implements com.jogamp.newt.event.MouseListener {

    private ViewPole viewPole;

    public MouseListener(ViewPole viewPole) {
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

}
