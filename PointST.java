import dsa.Point2D;
import dsa.RectHV;

public interface PointST<Value> {
    // Returns true if this symbol table is empty, and false otherwise.
    public boolean isEmpty();

    // Returns the number of key-value pairs in this symbol table.
    public int size();

    // Inserts the given point and value into this symbol table.
    public void put(Point2D p, Value value);

    // Returns the value associated with the given point in this symbol table, or null.
    public Value get(Point2D p);

    // Returns true if this symbol table contains the given point, and false otherwise.
    public boolean contains(Point2D p);

    // Returns all the points in this symbol table.
    public Iterable<Point2D> points();

    // Returns all the points in this symbol table that are inside the given rectangle.
    public Iterable<Point2D> range(RectHV rect);

    // Returns the point in this symbol table that is different from and closest to the given point,
    // or null.
    public Point2D nearest(Point2D p);

    // Returns up to k points from this symbol table that are different from and closest to the
    // given point.
    public Iterable<Point2D> nearest(Point2D p, int k);
}
