record Input(int held, int pressed, boolean firing, double getMouseX, double getMouseY) {
    boolean isDown(Keys key) {
        return (held & key.mask()) != 0;
    }

    boolean wasPressed(Keys key) {
        return (pressed & key.mask()) != 0;
    }

    boolean isDown(MouseButtons button) {
        return firing;
    }

    Input withoutPresses() {
        return new Input(held, 0, firing, getMouseX, getMouseY);
    }
}
