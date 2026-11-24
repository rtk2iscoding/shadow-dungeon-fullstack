record Rectangle(double x, double y, double width, double height) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    Point topLeft() {
        return new Point(x, y);
    }

    Point bottomRight() {
        return new Point(x + width, y + height);
    }

    boolean intersects(Rectangle other) {
        return x < other.x + other.width && x + width > other.x
            && y < other.y + other.height && y + height > other.y;
    }
}
