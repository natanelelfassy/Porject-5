// Accepts filename (String) as command-line argument; draws points from the file using standard
// draw; and highlights all the points in the rectangle the user selects by dragging the mouse.
// The search results obtained using BrutePointST are highlighted in red while those obtained
// using the KdTreeST are highlighted in blue.

import dsa.Point2D;
import dsa.RectHV;
import stdlib.In;
import stdlib.StdDraw;

public class RangeSearchVisualizer {
    // Entry point.
    public static void main(String[] args) {
        String filename = args[0];
        In in = new In(filename);

        // Initialize the data structures with n points from the file.
        BrutePointST<Integer> brute = new BrutePointST<Integer>();
        KdTreePointST<Integer> kdtree = new KdTreePointST<Integer>();
        for (int i = 0; !in.isEmpty(); i++) {
            double x = in.readDouble();
            double y = in.readDouble();
            Point2D p = new Point2D(x, y);
            kdtree.put(p, i);
            brute.put(p, i);
        }

        double x0 = 0.0, y0 = 0.0;  // initial endpoint of rectangle
        double x1 = 0.0, y1 = 0.0;  // current location of mouse
        boolean isDragging = false; // is the user dragging a rectangle

        // Enable double buffering to avoid flicker.
        StdDraw.enableDoubleBuffering();

        while (true) {
            StdDraw.show();
            StdDraw.pause(40);
            if (StdDraw.isMousePressed() && !isDragging) {
                // User starts to drag a rectangle.
                x0 = StdDraw.mouseX();
                y0 = StdDraw.mouseY();
                isDragging = true;
                continue;
            } else if (StdDraw.isMousePressed() && isDragging) {
                // User is dragging a rectangle
                x1 = StdDraw.mouseX();
                y1 = StdDraw.mouseY();
                continue;
            } else if (!StdDraw.isMousePressed() && isDragging) {
                // mouse no longer pressed
                isDragging = false;
            }

            // The rectangle selected by the user.
            RectHV rect = new RectHV(Math.min(x0, x1), Math.min(y0, y1),
                    Math.max(x0, x1), Math.max(y0, y1));

            // Draw all the points.
            StdDraw.clear();
            StdDraw.setPenColor(StdDraw.BLACK);
            StdDraw.setPenRadius(0.01);
            for (Point2D p : brute.points()) {
                p.draw();
            }

            // Draw the rectangle.
            StdDraw.setPenColor(StdDraw.BLACK);
            StdDraw.setPenRadius();
            rect.draw();

            // Highlight the range search results from BrutePointST in red.
            StdDraw.setPenRadius(0.03);
            StdDraw.setPenColor(StdDraw.RED);
            for (Point2D p : brute.range(rect)) {
                p.draw();
            }

            // Highlight the range search results from KdTreeST in blue.
            StdDraw.setPenRadius(0.02);
            StdDraw.setPenColor(StdDraw.BLUE);
            for (Point2D p : kdtree.range(rect)) {
                p.draw();
            }

            StdDraw.show();
            StdDraw.pause(40);
        }
    }
}
