/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logger;

/**
 * Rectangular region in a two-dimensional coordinate system expressed as an origin and dimensions extending from the
 * origin.
 */
public class Viewport {

    /**
     * The X component of the viewport's origin.
     */
    public int x;

    /**
     * The Y component of the viewport's origin.
     */
    public int y;

    /**
     * The viewport's width.
     */
    public int width;

    /**
     * The viewport's height.
     */
    public int height;

    /**
     * Constructs an empty viewport width X, Y, width and height all zero.
     */
    public Viewport() {
    }

    /**
     * Constructs a viewport with a specified origin and dimensions.
     *
     * @param x      the X component of the viewport's lower left corner
     * @param y      the Y component of the viewport's lower left corner
     * @param width  the viewport's width
     * @param height the viewport's height
     */
    public Viewport(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Constructs a viewport with the origin and dimensions of a specified viewport.
     *
     * @param viewport the viewport specifying the values
     *
     * @throws IllegalArgumentException If the viewport is null
     */
    public Viewport(Viewport viewport) {
        if (viewport == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "Viewport", "constructor", "missingViewport"));
        }

        this.x = viewport.x;
        this.y = viewport.y;
        this.width = viewport.width;
        this.height = viewport.height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        Viewport that = (Viewport) o;
        return this.x == that.x && this.y == that.y && this.width == that.width && this.height == that.height;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(this.x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.width);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.height);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Viewport{" +
            "x=" + this.x +
            ", y=" + this.y +
            ", width=" + this.width +
            ", height=" + this.height +
            '}';
    }

    /**
     * Sets this viewport to the specified origin and dimensions.
     *
     * @param x      the new X component of the viewport's lower left corner
     * @param y      the new Y component of the viewport's lower left corner
     * @param width  the viewport's new width
     * @param height the viewport's new height
     *
     * @return this viewport set to the specified values
     */
    public Viewport set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Sets this viewport to the origin and dimensions of a specified viewport.
     *
     * @param viewport the viewport specifying the new value
     *
     * @return this viewport with its origin and dimensions set to that of the specified viewport
     *
     * @throws IllegalArgumentException If the viewport is null
     */
    public Viewport set(Viewport viewport) {
        if (viewport == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "Viewport", "set", "missingViewport"));
        }

        this.x = viewport.x;
        this.y = viewport.y;
        this.width = viewport.width;
        this.height = viewport.height;
        return this;
    }

    /**
     * Sets this viewport to an empty viewport.
     *
     * @return this viewport with its width and height both set to zero
     */
    public Viewport setEmpty() {
        this.width = 0;
        this.height = 0;
        return this;
    }

    /**
     * Indicates whether or not this viewport is empty. An viewport is empty when either its width or its height are
     * zero (or negative).
     *
     * @return true if this viewport is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.width <= 0 || this.height <= 0;
    }

    /**
     * Indicates whether this viewport intersects a specified viewport. Two viewport intersect when both overlap by a
     * non-zero amount. An empty viewport never intersects another viewport.
     *
     * @param viewport the viewport to test intersection with
     *
     * @return true if the specified viewport intersections this viewport, false otherwise
     *
     * @throws IllegalArgumentException If the viewport is null
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public boolean intersects(Viewport viewport) {
        if (viewport == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "Viewport", "intersects", "missingViewport"));
        }

        Viewport that = viewport;
        return this.x < that.x + that.width
            && that.x < this.x + this.width
            && this.y < that.y + that.height
            && that.y < this.y + this.height;
    }
}