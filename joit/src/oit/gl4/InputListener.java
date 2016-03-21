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
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import glm.vec._2.i.Vec2i;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;

/**
 *
 * @author GBarbieri
 */
public class InputListener implements KeyListener, MouseListener {

    private float radius;
    private Mat4 orient;
    private Mat4 tmpM;
    private Vec3 target;
    private Vec4 targetOffset;
    private Vec3 tmpV;
    private Mat4 view;
    private Vec2i firstInput;
    private Vec2i deltaInput;
    private Vec2 rotation;
    private long startPause;

    public InputListener() {

        radius = 0.5f;
        orient = new Mat4();
        target = new Vec3(0f, .12495125f, 0f);
        targetOffset = new Vec4();

        tmpV = new Vec3();
        tmpM = new Mat4();

        view = new Mat4();

        firstInput = new Vec2i();
        deltaInput = new Vec2i();

        rotation = new Vec2();
    }

    public void update() {

        view
                .identity()
                .translate(0, 0, -radius)
                .mul(orient)
                .translate(target.negate(tmpV));
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

        if (e.getButton() == MouseEvent.BUTTON1) {

            firstInput.set(e.getX(), e.getY());

        } else if (e.getButton() == MouseEvent.BUTTON3) {

            firstInput.set(e.getX(), -e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {

            firstInput.sub(e.getX(), e.getY(), deltaInput);

            rotation.add(-deltaInput.x * 0.01f, -deltaInput.y * 0.01f);

            orient
                    .identity()
                    .rotateX(rotation.y)
                    .rotateY(rotation.x);

            firstInput.set(e.getX(), e.getY());

        } else if (e.getButton() == MouseEvent.BUTTON3) {

            firstInput.sub(e.getX(), -e.getY(), deltaInput);

            targetOffset.set(-deltaInput.x * (radius * 0.001f), -deltaInput.y * (radius * 0.001f), 0, 1);

            orient.inverse(tmpM).mul(targetOffset);

            target.add(targetOffset.x, targetOffset.y, targetOffset.z);

            firstInput.set(e.getX(), -e.getY());
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {

        radius += e.getRotation()[1] * radius / 10;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            Viewer.animator.stop();
        }
        if (e.getKeyCode() == KeyEvent.VK_R) {
            reset();
        }
    }

    private void reset() {

        radius = 5_000f;
        orient.identity();
        target.set(0, 0, 0);
        targetOffset.set(0, 0, 0, 0);

        tmpV.set(0, 0, 0);
        tmpM.identity();

        view.identity();

        firstInput.set(0, 0);
        deltaInput.set(0, 0);

        rotation.set(0, 0);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public Mat4 getView() {
        return view;
    }
}
