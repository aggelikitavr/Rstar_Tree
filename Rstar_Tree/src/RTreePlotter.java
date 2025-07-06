import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class RTreePlotter {

    // Coordinates - Normalisation
    static final double LAT_MARGIN = 0.006;
    static final double LON_MARGIN = 0.006;

    static final double MIN_LAT = 40.601066 - LAT_MARGIN;
    static final double MAX_LAT = 40.6440069 + LAT_MARGIN;
    static final double MIN_LON = 22.9287108 - LON_MARGIN;
    static final double MAX_LON = 22.9926466 + LON_MARGIN;


    public static void plotTree(RstarTree tree, String filename, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        drawAxes(g, width, height);

        // Drawing tree
        drawNode(g, tree.root, width, height, 0);

        g.dispose();

        // Saving image
        ImageIO.write(image, "png", new File(filename));
        System.out.println("Tree saved to: " + filename);
    }

    private static void drawNode(Graphics2D g, Node node, int width, int height, int level) throws IOException {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.ORANGE};
        g.setColor(colors[level % colors.length]);
        g.setStroke(new BasicStroke(2f));

        if (node.isLeaf) {
            for (MBR mbr : node.getMBRs()) {
                drawMBR(g, mbr, width, height);
            }
        } else {
            drawMBR(g, node.nodeMBR, width, height);
            for (Node child : node.children) {
                drawNode(g, child, width, height, level + 1);
            }
        }
    }

    private static void drawMBR(Graphics2D g, MBR mbr, int width, int height) {
        // latitude = Y, longitude = X
        double minLat = mbr.getLower(0);  // Y
        double minLon = mbr.getLower(1);  // X
        double maxLat = mbr.getUpper(0);
        double maxLon = mbr.getUpper(1);

        // Normalisation in [0,1]
        double normMinX = (minLon - MIN_LON) / (MAX_LON - MIN_LON);
        double normMaxX = (maxLon - MIN_LON) / (MAX_LON - MIN_LON);
        double normMinY = (minLat - MIN_LAT) / (MAX_LAT - MIN_LAT);
        double normMaxY = (maxLat - MIN_LAT) / (MAX_LAT - MIN_LAT);

        // Pixels (flip Y)
        int x = (int) (normMinX * width);
        int y = (int) ((1.0 - normMaxY) * height);
        int w = (int) ((normMaxX - normMinX) * width);
        int h = (int) ((normMaxY - normMinY) * height);

        g.drawRect(x, y, w, h);
    }

    private static void drawAxes(Graphics2D g, int width, int height) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));

        // Axis x
        g.drawLine(40, height - 40, width - 20, height - 40);
        // Axis y
        g.drawLine(40, 20, 40, height - 40);

        // X axis labels (longitude)
        for (int i = 0; i <= 10; i++) {
            int x = 40 + i * (width - 60) / 10;
            g.drawLine(x, height - 45, x, height - 35);
            double lon = MIN_LON + i * (MAX_LON - MIN_LON) / 10;
            g.drawString(String.format("%.4f", lon), x - 15, height - 20);
        }

        // Y axis labels (latitude)
        for (int i = 0; i <= 10; i++) {
            int y = height - 40 - i * (height - 60) / 10;
            g.drawLine(35, y, 45, y);
            double lat = MIN_LAT + i * (MAX_LAT - MIN_LAT) / 10;
            g.drawString(String.format("%.4f", lat), 5, y + 5);
        }
    }
}
