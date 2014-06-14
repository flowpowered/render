/*
 * This file is part of Flow Render, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2014 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.render;

import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class RenderUtilsTest {
    private static final float EPSILON = 0.001f;

    @Test
    public void testPlanesAndFOV() {
        float near, far, fov, fieldOfView;
        Matrix4f projection;
        Vector2f planes;

        near = 0.1f;
        far = 100;
        fov = 60;
        projection = Matrix4f.createPerspective(fov, 1, near, far);
        planes = RenderUtil.getPlanes(projection);
        fieldOfView = (float) Math.toDegrees(RenderUtil.getFieldOfView(projection));
        Assert.assertEquals(near, planes.getX(), EPSILON);
        Assert.assertEquals(far, planes.getY(), EPSILON);
        Assert.assertEquals(fov, fieldOfView, EPSILON);

        near = 50;
        far = 1000;
        fov = 172;
        projection = Matrix4f.createPerspective(fov, 1, near, far);
        planes = RenderUtil.getPlanes(projection);
        fieldOfView = (float) Math.toDegrees(RenderUtil.getFieldOfView(projection));
        Assert.assertEquals(near, planes.getX(), EPSILON);
        Assert.assertEquals(far, planes.getY(), EPSILON);
        Assert.assertEquals(fov, fieldOfView, EPSILON);

        near = 0.01f;
        far = 2;
        fov = 0.1f;
        projection = Matrix4f.createPerspective(fov, 1, near, far);
        planes = RenderUtil.getPlanes(projection);
        fieldOfView = (float) Math.toDegrees(RenderUtil.getFieldOfView(projection));
        Assert.assertEquals(near, planes.getX(), EPSILON);
        Assert.assertEquals(far, planes.getY(), EPSILON);
        Assert.assertEquals(fov, fieldOfView, EPSILON);

        near = 0.1f;
        far = 100;
        projection = Matrix4f.createOrthographic(1, -1, 1, -1, near, far);
        planes = RenderUtil.getPlanes(projection);
        Assert.assertEquals(planes.getX(), near, EPSILON);
        Assert.assertEquals(planes.getY(), far, EPSILON);

        near = 50;
        far = 1000;
        projection = Matrix4f.createOrthographic(1, -1, 1, -1, near, far);
        planes = RenderUtil.getPlanes(projection);
        Assert.assertEquals(planes.getX(), near, EPSILON);
        Assert.assertEquals(planes.getY(), far, EPSILON);

        near = 0.01f;
        far = 2;
        projection = Matrix4f.createOrthographic(1, -1, 1, -1, near, far);
        planes = RenderUtil.getPlanes(projection);
        Assert.assertEquals(planes.getX(), near, EPSILON);
        Assert.assertEquals(planes.getY(), far, EPSILON);
    }
}
