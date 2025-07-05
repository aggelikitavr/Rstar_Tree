import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class RTreePlotter {

    public static void plotTree(RstarTree tree, String filename, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Λευκό φόντο
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        drawAxes(g, width, height);
        drawNode(g, tree.root, width, height, 0);

        // Σχεδίαση του δέντρου
        drawNode(g, tree.root, width, height, 0);

        g.dispose();

        // Αποθήκευση εικόνας
        try {
            ImageIO.write(image, "png", new File(filename));
            System.out.println("Tree saved to: " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void drawNode(Graphics2D g, Node node, int width, int height, int level) throws IOException {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.ORANGE};
        g.setColor(colors[level % colors.length]);
        g.setStroke(new BasicStroke(3f));

        if (node.isLeaf) {
            for (MBR mbr : node.getMBRs()) {
                drawMBR(g, mbr, width, height);
            }
        } else {
            drawMBR(g, node.nodeMBR, width, height);  // Σχεδιάζουμε MBR του εσωτερικού κόμβου
            for (Node child : node.children) {
                drawNode(g, child, width, height, level + 1);
            }
        }
    }


    private static void drawMBR(Graphics2D g, MBR mbr, int width, int height) {
        double minX = mbr.getLower(0);
        double minY = mbr.getLower(1);
        double maxX = mbr.getUpper(0);
        double maxY = mbr.getUpper(1);

        int x = (int) (minX / 100.0 * width);
        int y = (int) ((1.0 - maxY / 100.0) * height);
        int w = (int) ((maxX - minX) / 100.0 * width);
        int h = (int) ((maxY - minY) / 100.0 * height);

        g.drawRect(x, y, w, h);
    }

    private static void drawAxes(Graphics2D g, int width, int height) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));

        // Οριζόντιος άξονας (x)
        g.drawLine(40, height - 40, width - 20, height - 40);
        // Κάθετος άξονας (y)
        g.drawLine(40, 20, 40, height - 40);

        // Διαβαθμίσεις και labels στον x άξονα
        for (int i = 0; i <= 10; i++) {
            int x = 40 + i * (width - 60) / 10;
            g.drawLine(x, height - 45, x, height - 35);
            String label = String.valueOf(i * 10); // Αν η κλίμακα είναι 0-100
            g.drawString(label, x - 10, height - 20);
        }

        // Διαβαθμίσεις και labels στον y άξονα
        for (int i = 0; i <= 10; i++) {
            int y = height - 40 - i * (height - 60) / 10;
            g.drawLine(35, y, 45, y);
            String label = String.valueOf(i * 10);
            g.drawString(label, 5, y + 5);
        }
    }

}
