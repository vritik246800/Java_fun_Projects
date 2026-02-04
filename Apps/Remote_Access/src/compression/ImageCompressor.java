package compression;

import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

public class ImageCompressor {

    public static byte[] compress(BufferedImage img, float quality)
            throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 0.4–0.6 ideal

        writer.setOutput(ImageIO.createImageOutputStream(baos));
        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();

        return baos.toByteArray();
    }
}