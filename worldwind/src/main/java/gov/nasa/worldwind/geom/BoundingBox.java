/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.geom;

import java.util.Arrays;

import gov.nasa.worldwind.globe.Globe;
import gov.nasa.worldwind.util.Logger;

/**
 * Represents a bounding box in Cartesian coordinates. Typically used as a bounding volume.
 */
public class BoundingBox {

    /**
     * The box's center point.
     */
    protected Vec3 center = new Vec3(0, 0, 0);

    /**
     * The center point of the box's bottom. (The origin of the R axis.)
     */
    protected Vec3 bottomCenter = new Vec3(-0.5, 0, 0);

    /**
     * The center point of the box's top. (The end of the R axis.)
     */
    protected Vec3 topCenter = new Vec3(0.5, 0, 0);

    /**
     * The box's R axis, its longest axis.
     */
    protected Vec3 r = new Vec3(1, 0, 0);

    /**
     * The box's S axis, its mid-length axis.
     */
    protected Vec3 s = new Vec3(0, 1, 0);

    /**
     * The box's T axis, its shortest axis.
     */
    protected Vec3 t = new Vec3(0, 0, 1);

    /**
     * The box's radius. (The half-length of its diagonal.)
     */
    protected double radius = Math.sqrt(3);

    private Vec3 endPoint1 = new Vec3();

    private Vec3 endPoint2 = new Vec3();

    public BoundingBox() {
    }

    @Override
    public String toString() {
        return "center=[" + center +
            "], bottomCenter=[" + bottomCenter +
            "], topCenter=[" + topCenter +
            "], r=[" + r +
            "], s=[" + s +
            "], t=[" + t +
            "], radius=" + radius;
    }

    /**
     * Indicates whether this box is an uninitialized unit box centered at the Cartesian origin (0, 0, 0).
     *
     * @return true if this box is an uninitialized unit box, otherwise false
     */
    public boolean isUnitBox() {
        return this.center.x == 0
            && this.center.y == 0
            && this.center.z == 0
            && this.radius == Math.sqrt(3);
    }

    /**
     * Sets this bounding box such that it minimally encloses a specified array of points.
     *
     * @param array  the array of points to consider
     * @param count  the number of array elements to consider
     * @param stride the number of coordinates between the first coordinate of adjacent points - must be at least 3
     *
     * @return This bounding box set to contain the specified array of points.
     *
     * @throws IllegalArgumentException If the array is null or empty, if the count is less than 0, or if the stride is
     *                                  less than 3
     */
    public BoundingBox setToPoints(float[] array, int count, int stride) {
        if (array == null || array.length < stride) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "Matrix4", "setToCovarianceOfPoints", "missingArray"));
        }

        if (count < 0) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "Matrix4", "setToCovarianceOfPoints", "invalidCount"));
        }

        if (stride < 3) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "Matrix4", "setToCovarianceOfPoints", "invalidStride"));
        }

        // Compute this box's axes by performing a principal component analysis on the array of points.
        Matrix4 matrix = new Matrix4();
        matrix.setToCovarianceOfPoints(array, count, stride);
        matrix.extractEigenvectors(this.r, this.s, this.t);
        this.r.normalize();
        this.s.normalize();
        this.t.normalize();

        // Find the extremes along each axis.
        double rMin = Double.POSITIVE_INFINITY;
        double rMax = Double.NEGATIVE_INFINITY;
        double sMin = Double.POSITIVE_INFINITY;
        double sMax = Double.NEGATIVE_INFINITY;
        double tMin = Double.POSITIVE_INFINITY;
        double tMax = Double.NEGATIVE_INFINITY;

        Vec3 p = new Vec3();
        for (int idx = 0; idx < count; idx += stride) {
            p.set(array[idx], array[idx + 1], array[idx + 2]);

            double pdr = p.dot(this.r);
            if (rMin > pdr) {
                rMin = pdr;
            }
            if (rMax < pdr) {
                rMax = pdr;
            }

            double pds = p.dot(this.s);
            if (sMin > pds) {
                sMin = pds;
            }
            if (sMax < pds) {
                sMax = pds;
            }

            double pdt = p.dot(this.t);
            if (tMin > pdt) {
                tMin = pdt;
            }
            if (tMax < pdt) {
                tMax = pdt;
            }
        }

        // Ensure that the extremes along each axis have nonzero separation.
        if (rMax == rMin)
            rMax = rMin + 1;
        if (sMax == sMin)
            sMax = sMin + 1;
        if (tMax == tMin)
            tMax = tMin + 1;

        // Compute the box properties from its unit axes and the extremes along each axis.
        double rLen = rMax - rMin;
        double sLen = sMax - sMin;
        double tLen = tMax - tMin;
        double rSum = rMax + rMin;
        double sSum = sMax + sMin;
        double tSum = tMax + tMin;

        double cx = 0.5 * (this.r.x * rSum + this.s.x * sSum + this.t.x * tSum);
        double cy = 0.5 * (this.r.y * rSum + this.s.y * sSum + this.t.y * tSum);
        double cz = 0.5 * (this.r.z * rSum + this.s.z * sSum + this.t.z * tSum);
        double rx_2 = 0.5 * this.r.x * rLen;
        double ry_2 = 0.5 * this.r.y * rLen;
        double rz_2 = 0.5 * this.r.z * rLen;

        this.center.set(cx, cy, cz);
        this.topCenter.set(cx + rx_2, cy + ry_2, cz + rz_2);
        this.bottomCenter.set(cx - rx_2, cy - ry_2, cz - rz_2);

        this.r.multiply(rLen);
        this.s.multiply(sLen);
        this.t.multiply(tLen);

        this.radius = 0.5 * Math.sqrt(rLen * rLen + sLen * sLen + tLen * tLen);

        return this;
    }

    /**
     * Sets this bounding box such that it contains a specified sector on a specified globe with min and max elevation.
     * <p/>
     * To create a bounding box that contains the sector at mean sea level, specify zero for the minimum and maximum
     * elevations. To create a bounding box that contains the terrain surface in this sector, specify the actual minimum
     * and maximum elevation values associated with the sector, multiplied by the model's vertical exaggeration.
     * <p/>
     *
     * @param sector       The sector for which to create the bounding box.
     * @param globe        The globe associated with the sector.
     * @param minElevation The minimum elevation within the sector.
     * @param maxElevation The maximum elevation within the sector.
     *
     * @return This bounding box set to contain the specified sector.
     *
     * @throws IllegalArgumentException If either the specified sector or globe is null or undefined.
     */
    public BoundingBox setToSector(Sector sector, Globe globe, double minElevation, double maxElevation) {
        if (sector == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "BoundingBox", "setToSector", "missingSector"));
        }

        if (globe == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "BoundingBox", "setToSector", "missingGlobe"));
        }

        // Compute the cartesian points for a 3x3 geographic grid. This grid captures enough detail to bound the
        // sector. Use minimum elevation at the corners and max elevation everywhere else.
        int numLat = 3;
        int numLon = 3;
        int count = numLat * numLon;
        int stride = 3;

        double[] elevations = new double[count];
        Arrays.fill(elevations, maxElevation);
        elevations[0] = elevations[2] = elevations[6] = elevations[8] = minElevation;

        float[] points = new float[count * stride];
        globe.geographicToCartesianGrid(sector, numLat, numLon, elevations, null, points, stride, 0);

        // Compute the local coordinate axes. Since we know this box is bounding a geographic sector, we use the
        // local coordinate axes at its centroid as the box axes. Using these axes results in a box that has +-10%
        // the volume of a box with axes derived from a principal component analysis, but is faster to compute.
        double centroidLat = sector.centroidLatitude();
        double centroidLon = sector.centroidLongitude();
        Matrix4 matrix = globe.geographicToCartesianTransform(centroidLat, centroidLon, 0, new Matrix4());
        double m[] = matrix.m;

        this.r.set(m[0], m[4], m[8]);
        this.s.set(m[1], m[5], m[9]);
        this.t.set(m[2], m[6], m[10]);

        // Find the extremes along each axis.
        double rExtremes[] = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        double sExtremes[] = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        double tExtremes[] = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        Vec3 p = new Vec3();
        for (int idx = 0, len = points.length; idx < len; idx += stride) {
            p.set(points[idx], points[idx + 1], points[idx + 2]);
            adjustExtremes(this.r, rExtremes, this.s, sExtremes, this.t, tExtremes, p);
        }

        // Sort the axes from most prominent to least prominent. The frustum intersection methods assume that the axes 
        // are defined in this way.
        if (rExtremes[1] - rExtremes[0] < sExtremes[1] - sExtremes[0]) {
            swapAxes(this.r, rExtremes, this.s, sExtremes);
        }
        if (sExtremes[1] - sExtremes[0] < tExtremes[1] - tExtremes[0]) {
            swapAxes(this.s, sExtremes, this.t, tExtremes);
        }
        if (rExtremes[1] - rExtremes[0] < sExtremes[1] - sExtremes[0]) {
            swapAxes(this.r, rExtremes, this.s, sExtremes);
        }

        // Compute the box properties from its unit axes and the extremes along each axis.
        double rLen = rExtremes[1] - rExtremes[0];
        double sLen = sExtremes[1] - sExtremes[0];
        double tLen = tExtremes[1] - tExtremes[0];
        double rSum = rExtremes[1] + rExtremes[0];
        double sSum = sExtremes[1] + sExtremes[0];
        double tSum = tExtremes[1] + tExtremes[0];

        double cx = 0.5 * (this.r.x * rSum + this.s.x * sSum + this.t.x * tSum);
        double cy = 0.5 * (this.r.y * rSum + this.s.y * sSum + this.t.y * tSum);
        double cz = 0.5 * (this.r.z * rSum + this.s.z * sSum + this.t.z * tSum);
        double rx_2 = 0.5 * this.r.x * rLen;
        double ry_2 = 0.5 * this.r.y * rLen;
        double rz_2 = 0.5 * this.r.z * rLen;

        this.center.set(cx, cy, cz);
        this.topCenter.set(cx + rx_2, cy + ry_2, cz + rz_2);
        this.bottomCenter.set(cx - rx_2, cy - ry_2, cz - rz_2);

        this.r.multiply(rLen);
        this.s.multiply(sLen);
        this.t.multiply(tLen);

        this.radius = 0.5 * Math.sqrt(rLen * rLen + sLen * sLen + tLen * tLen);

        return this;
    }

    /**
     * Translates this bounding box by specified components.
     *
     * @param x the X translation component
     * @param y the Y translation component
     * @param z the Z translation component
     *
     * @return this bounding box translated by the specified components
     */
    public BoundingBox translate(double x, double y, double z) {
        this.center.x += x;
        this.center.y += y;
        this.center.z += z;

        this.bottomCenter.x += x;
        this.bottomCenter.y += y;
        this.bottomCenter.z += z;

        this.topCenter.x += x;
        this.topCenter.y += y;
        this.topCenter.z += z;

        return this;
    }

    public double distanceTo(Vec3 point) {
        return this.center.distanceTo(point); // TODO shortest distance to center and corner points
    }

    /**
     * Indicates whether this bounding box intersects a specified frustum.
     *
     * @param frustum The frustum of interest.
     *
     * @return true if the specified frustum intersects this bounding box, otherwise false.
     *
     * @throws IllegalArgumentException If the specified frustum is null or undefined.
     */
    public boolean intersectsFrustum(Frustum frustum) {
        if (frustum == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "BoundingBox", "intersectsFrustum", "missingFrustum"));
        }

        this.endPoint1.set(this.bottomCenter);
        this.endPoint2.set(this.topCenter);

        if (this.intersectsAt(frustum.near) < 0) {
            return false;
        }

        if (this.intersectsAt(frustum.far) < 0) {
            return false;
        }

        if (this.intersectsAt(frustum.left) < 0) {
            return false;
        }

        if (this.intersectsAt(frustum.right) < 0) {
            return false;
        }

        if (this.intersectsAt(frustum.top) < 0) {
            return false;
        }

        if (this.intersectsAt(frustum.bottom) < 0) {
            return false;
        }

        return true;
    }

    private double intersectsAt(Plane plane) {
        Vec3 n = plane.normal;
        double effectiveRadius = 0.5 * (Math.abs(this.s.dot(n)) + Math.abs(this.t.dot(n)));

        // Test the distance from the first end-point.
        double dq1 = plane.dot(this.endPoint1);
        boolean bq1 = dq1 <= -effectiveRadius;

        // Test the distance from the second end-point.
        double dq2 = plane.dot(this.endPoint2);
        boolean bq2 = dq2 <= -effectiveRadius;

        if (bq1 && bq2) { // endpoints more distant from plane than effective radius; box is on neg. side of plane
            return -1;
        }

        if (bq1 == bq2) { // endpoints less distant from plane than effective radius; can't draw any conclusions
            return 0;
        }

        // Compute and return the endpoints of the box on the positive side of the plane
        double dot = n.x * (this.endPoint1.x - this.endPoint2.x)
            + n.y * (this.endPoint1.y - this.endPoint2.y)
            + n.z * (this.endPoint1.z - this.endPoint2.z);
        double t = (effectiveRadius + dq1) / dot;

        // Truncate the line to only that in the positive halfspace, e.g., inside the frustum.
        double x = (this.endPoint2.x - this.endPoint1.x) * t + this.endPoint1.x;
        double y = (this.endPoint2.y - this.endPoint1.y) * t + this.endPoint1.y;
        double z = (this.endPoint2.z - this.endPoint1.z) * t + this.endPoint1.z;
        if (bq1) {
            this.endPoint1.set(x, y, z);
        } else {
            this.endPoint2.set(x, y, z);
        }

        return t;
    }

    private static void adjustExtremes(Vec3 r, double[] rExtremes, Vec3 s, double[] sExtremes, Vec3 t, double[] tExtremes, Vec3 p) {
        double pdr = p.dot(r);
        if (rExtremes[0] > pdr) {
            rExtremes[0] = pdr;
        }
        if (rExtremes[1] < pdr) {
            rExtremes[1] = pdr;
        }

        double pds = p.dot(s);
        if (sExtremes[0] > pds) {
            sExtremes[0] = pds;
        }
        if (sExtremes[1] < pds) {
            sExtremes[1] = pds;
        }

        double pdt = p.dot(t);
        if (tExtremes[0] > pdt) {
            tExtremes[0] = pdt;
        }
        if (tExtremes[1] < pdt) {
            tExtremes[1] = pdt;
        }
    }

    private static void swapAxes(Vec3 a, double[] aExtremes, Vec3 b, double[] bExtremes) {
        a.swap(b);

        double tmp = aExtremes[0];
        aExtremes[0] = bExtremes[0];
        bExtremes[0] = tmp;

        tmp = aExtremes[1];
        aExtremes[1] = bExtremes[1];
        bExtremes[1] = tmp;
    }
}
