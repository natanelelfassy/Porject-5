// Accepts mode ("brute" or "kdtree"), numBoids (int), and friends (int) as command-line
// arguments; and implements a boid simulator using BrutePointST if mode is "brute" and KdTreeST
// if mode is "kdtree", with the given number of boids and friends per boid. Instructions for
// using the boid simulator:
//    Press "o" to zoom out.
//    Press "i" to zoom in.
//    Press "t" to track the center of mass of all boids.
//    Press "h" to track the hawk.
//    Press "m" to manually control the camera.
//    While in "manual" mode, use arrow keys to control camera movement.

import java.awt.event.KeyEvent;

import dsa.LinkedQueue;
import dsa.Point2D;
import dsa.Vector;
import stdlib.StdDraw;
import stdlib.StdRandom;

public class BoidSimulator {
    // Mode selection constants.
    private static final char MANUAL_MODE = 'm';
    private static final char TRACKING_MODE = 't';
    private static final char HAWK_MODE = 'h';

    // Camera movement constants.
    private static final double ZOOM_FACTOR = 1.1;
    private static final double CAMERA_SPEED = 0.05;

    private static char mode = TRACKING_MODE; // start in "tracking" mode

    // Entry point.
    public static void main(String[] args) {
        String mode = args[0];
        int numBoids = Integer.parseInt(args[1]);
        int friends = Integer.parseInt(args[2]);

        Hawk hawk = new Hawk(0.5, 0.3);
        StdDraw.pause(20);

        // Each boid tracks a number of nearest neighbors equal to FRIENDS.
        Boid[] boids = new Boid[numBoids];
        double meanX, meanY;
        double radius = 0.5;
        double currentX = 0.5;
        double currentY = 0.5;

        // Generate random boids.
        for (int i = 0; i < numBoids; i++) {
            double startX = StdRandom.uniform();
            double startY = StdRandom.uniform();
            double velX = (StdRandom.uniform() - 0.5) / 1000;
            double velY = (StdRandom.uniform() - 0.5) / 1000;
            boids[i] = new Boid(startX, startY, velX, velY);
        }

        // Enable double buffering to avoid flicker.
        StdDraw.enableDoubleBuffering();

        while (true) {
            // Process keyboard input.
            if (StdDraw.isKeyPressed(KeyEvent.VK_I)) {
                // Press "i" to zoom in.
                radius *= 1 / ZOOM_FACTOR;
            }
            if (StdDraw.isKeyPressed(KeyEvent.VK_O)) {
                // Press "o" to zoom out.
                radius *= ZOOM_FACTOR;
            }
            if (StdDraw.isKeyPressed(KeyEvent.VK_M)) {
                // Press "m" to enter "manual" mode.
                BoidSimulator.mode = MANUAL_MODE;
            }
            if (StdDraw.isKeyPressed(KeyEvent.VK_H)) {
                // Press "h" to enter "hawk" mode.
                BoidSimulator.mode = HAWK_MODE;
            }
            if (StdDraw.isKeyPressed(KeyEvent.VK_T)) {
                // Press "t" to enter "tracking" mode.
                BoidSimulator.mode = TRACKING_MODE;
            }

            // Scale pen radius relative to zoom.
            StdDraw.setPenRadius(0.01 * (0.5 / radius));
            StdDraw.setXscale(currentX - radius, currentX + radius);
            StdDraw.setYscale(currentY - radius, currentY + radius);

            // Draw all boids and calculate their meanX and meanY.
            meanX = 0;
            meanY = 0;
            for (int i = 0; i < numBoids; i++) {
                meanX += boids[i].x() / numBoids;
                meanY += boids[i].y() / numBoids;
                boids[i].draw();
            }

            // Draw the hawk.
            hawk.draw();

            if (BoidSimulator.mode == TRACKING_MODE) {
                // Follow center of mass in tracking mode.
                currentX = meanX;
                currentY = meanY;
            } else if (BoidSimulator.mode == MANUAL_MODE) {
                // Allow user to control movement in manual mode.
                if (StdDraw.isKeyPressed(KeyEvent.VK_UP)) {
                    // Press "up arrow" to pan upwards.
                    currentY += radius * CAMERA_SPEED;
                }
                if (StdDraw.isKeyPressed(KeyEvent.VK_DOWN)) {
                    // Press "down arrow" to pan downwards.
                    currentY -= radius * CAMERA_SPEED;
                }
                if (StdDraw.isKeyPressed(KeyEvent.VK_LEFT)) {
                    // Press "left arrow" to pan to the left.
                    currentX -= radius * CAMERA_SPEED;
                }
                if (StdDraw.isKeyPressed(KeyEvent.VK_RIGHT)) {
                    // Press "right arrow" to pan to the right.
                    currentX += radius * CAMERA_SPEED;
                }
            } else if (BoidSimulator.mode == HAWK_MODE) {
                // Follow hawk in hawk mode.
                currentX = hawk.x();
                currentY = hawk.y();
            }

            // The entire symbol table must be rebuilt every frame. Since the boids are random,
            // we expect a roughly balanced tree, despite the lack of balancing in KdTreeST.
            PointST<Boid> st = null;
            if (mode.equals("brute")) {
                st = new BrutePointST<Boid>();
            } else if (mode.equals("kdtree")) {
                st = new KdTreePointST<Boid>();
            } else {
                throw new IllegalArgumentException("Illegal command-line argument");
            }
            for (int i = 0; i < numBoids; i++) {
                st.put(boids[i].position(), boids[i]);
            }
            for (int i = 0; i < numBoids; i++) {
                Iterable<Point2D> kNearestPoints = st.nearest(boids[i].position(), friends);
                Iterable<Boid> kNearest = lookUpBoids(st, kNearestPoints);
                boids[i].updatePositionAndVelocity(kNearest, hawk);
            }

            // The hawk will chase the nearest boid.
            Boid closestBoid = st.get(st.nearest(hawk.position()));
            hawk.updatePositionAndVelocity(closestBoid);

            StdDraw.show();
            StdDraw.pause(20);
            StdDraw.clear();
        }
    }

    private static Iterable<Boid> lookUpBoids(PointST<Boid> st, Iterable<Point2D> points) {
        LinkedQueue<Boid> values = new LinkedQueue<Boid>();
        for (Point2D p : points) {
            values.enqueue(st.get(p));
        }
        return values;
    }
}

class Boid {
    private static final double BOID_AVOIDANCE_WEIGHT = 0.01;
    private static final double HAWK_AVOIDANCE_WEIGHT = 0.01;
    private static final double VELOCITY_MATCH_WEIGHT = 1;
    private static final double PLUNGE_DEEPER_WEIGHT = 1;
    private static final double RETURN_TO_ORIGIN_WEIGHT = 0.05;
    private static final double THRUST_FACTOR = 0.0001;
    private Point2D position;
    private Vector velocity;

    public Boid(double x, double y) {
        position = new Point2D(x, y);
        velocity = new Vector(new double[]{0, 0});
    }

    public Boid(double x, double y, double xvel, double yvel) {
        position = new Point2D(x, y);
        velocity = new Vector(new double[]{xvel, yvel});
    }

    public Point2D position() {
        return position;
    }

    public double x() {
        return position.x();
    }

    public double y() {
        return position.y();
    }

    public Vector avoidCollision(Iterable<Boid> neighbors) {
        Vector requestedVector = new Vector(new double[]{0, 0});
        Vector myPosition = new Vector(new double[]{x(), y()});
        for (Boid b : neighbors) {
            Vector neighborPosition = new Vector(new double[]{b.x(), b.y()});
            double distanceTo = distance(myPosition, neighborPosition);
            if (distanceTo == 0.0) {
                break;
            }
            Vector avoidanceVector = myPosition.subtract(neighborPosition);
            Vector scaledAvoidanceVector = avoidanceVector.scale(1.0 / distanceTo);
            requestedVector = requestedVector.add(scaledAvoidanceVector);
        }
        return requestedVector;
    }

    public Vector avoidCollision(Hawk hawk) {
        Vector requestedVector = new Vector(new double[]{0, 0});
        Vector myPosition = new Vector(new double[]{x(), y()});
        Vector hawkPosition = new Vector(new double[]{hawk.x(), hawk.y()});
        double distanceTo = distance(myPosition, hawkPosition);
        Vector avoidanceVector = myPosition.subtract(hawkPosition);
        Vector scaledAvoidanceVector = avoidanceVector.scale(1.0 / distanceTo);
        requestedVector = requestedVector.add(scaledAvoidanceVector);
        return requestedVector;
    }

    public Vector matchVelocity(Iterable<Boid> neighbors) {
        Vector requestedVector = new Vector(new double[]{0, 0});
        for (Boid b : neighbors) {
            Vector neighborVelocity = b.getVelocity();
            Vector matchingVector = neighborVelocity.subtract(velocity);
            requestedVector = requestedVector.add(matchingVector);
        }
        return requestedVector;
    }

    public Vector plungeDeeper(Iterable<Boid> neighbors) {
        Vector requestedVector = new Vector(new double[]{0, 0});
        Vector centroid = new Vector(new double[]{0, 0});
        double neighborCnt = 0;
        for (Boid b : neighbors) {
            Vector neighborPosition = new Vector(new double[]{b.x(), b.y()});
            centroid = centroid.add(neighborPosition);
            neighborCnt++;
        }
        centroid = centroid.scale(1.0 / neighborCnt);
        Boid centroidPoint = new Boid(centroid.get(0), centroid.get(1));
        Vector myPosition = new Vector(new double[]{x(), y()});
        requestedVector = centroid.subtract(myPosition);
        return requestedVector;
    }

    public Vector returnToWorld() {
        Vector requestedVector = new Vector(new double[]{0, 0});
        Vector center = new Vector(new double[]{0.5, 0.5});
        Vector myPosition = new Vector(new double[]{x(), y()});
        requestedVector = center.subtract(myPosition);
        return requestedVector;
    }

    public Vector desiredAcceleration(Iterable<Boid> neighbors, Hawk hawk) {
        Vector avoidanceVector = avoidCollision(neighbors).scale(BOID_AVOIDANCE_WEIGHT);
        Vector hawkAvoidanceVector = avoidCollision(hawk).scale(HAWK_AVOIDANCE_WEIGHT);
        Vector matchingVector = matchVelocity(neighbors).scale(VELOCITY_MATCH_WEIGHT);
        Vector plungingVector = plungeDeeper(neighbors).scale(PLUNGE_DEEPER_WEIGHT);
        Vector returnVector = returnToWorld().scale(RETURN_TO_ORIGIN_WEIGHT);
        Vector desired = new Vector(new double[]{0, 0});
        desired = desired.add(avoidanceVector);
        desired = desired.add(hawkAvoidanceVector);
        desired = desired.add(matchingVector);
        desired = desired.add(plungingVector);
        desired = desired.add(returnVector);
        if (desired.magnitude() == 0.0) {
            return desired;
        }
        return desired.direction().scale(THRUST_FACTOR);
    }

    public void draw() {
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.point(x(), y());
    }

    public Vector getVelocity() {
        return velocity;
    }

    public Vector updatePositionAndVelocity(Iterable<Boid> neighbors, Hawk hawk) {
        double x = x() + velocity.get(0);
        double y = y() + velocity.get(1);
        position = new Point2D(x, y);
        Vector desire = desiredAcceleration(neighbors, hawk);
        velocity = velocity.add(desire);
        return desire;
    }

    private static double distance(Vector a, Vector b) {
        Vector c = a.subtract(b);
        return Math.sqrt(c.dot(c));
    }
}

class Hawk {
    private Point2D position;
    private Vector velocity;

    public Hawk(double x, double y) {
        position = new Point2D(x, y);
        velocity = new Vector(new double[]{0, 0});
    }

    public Point2D position() {
        return position;
    }

    public double x() {
        return position.x();
    }

    public double y() {
        return position.y();
    }

    public Vector eatBoid(Boid boid) {
        Vector boidPosition = new Vector(new double[]{boid.x(), boid.y()});
        Vector myPosition = new Vector(new double[]{x(), y()});
        return boidPosition.subtract(myPosition);
    }

    public Vector updatePositionAndVelocity(Boid nearest) {
        double x = x() + velocity.get(0);
        double y = y() + velocity.get(1);
        position = new Point2D(x, y);
        Vector desire = eatBoid(nearest).direction().scale(0.0003);
        velocity = velocity.add(desire);
        return desire;
    }

    public void draw() {
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.point(x(), y());
    }
}
