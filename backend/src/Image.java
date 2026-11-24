import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

final class Image {
    private record Size(int width, int height) {}
    private static final Map<String, Size> SIZES = new ConcurrentHashMap<>();

    private final String path;
    private final Size size;

    Image(String path) {
        this.path = path;
        size = SIZES.computeIfAbsent(path, Image::readSize);
    }

    private static Size readSize(String path) {
        try {
            var image = ImageIO.read(Path.of(path).toFile());
            if (image == null) throw new IOException("Unsupported image: " + path);
            return new Size(image.getWidth(), image.getHeight());
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    Rectangle getBoundingBoxAt(Point center) {
        return new Rectangle(center.x - size.width / 2.0, center.y - size.height / 2.0,
                size.width, size.height);
    }

    void draw(double x, double y) {
        Frame.image(path, x, y);
    }
}
