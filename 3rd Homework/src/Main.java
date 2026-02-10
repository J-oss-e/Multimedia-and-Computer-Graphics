import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        String originalPath = "Barycentric.jpg";
        String compressedPath = "compressed.png";
        String decompressedPath = "decompressed.png";

        ImageCompressor compressor = new ImageCompressor();
        compressor.compress(originalPath, compressedPath);

        ImageDecompressor decompressor = new ImageDecompressor();
        decompressor.decompress(compressedPath, decompressedPath);

        System.out.println("Finish process.");
    }
}
