/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author gbarbieri
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final GlViewer glViewer = new GlViewer();

        final Frame frame = new Frame("Order Independent Transparency");

        frame.add(glViewer.getNewtCanvasAWT());

        frame.setSize(glViewer.getGlWindow().getWidth(), glViewer.getGlWindow().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                glViewer.getAnimator().stop();
                glViewer.getGlWindow().destroy();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }
}
