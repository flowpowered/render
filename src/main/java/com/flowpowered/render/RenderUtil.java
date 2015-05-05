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

import com.flowpowered.commons.ViewFrustum;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;

import com.flowpowered.caustic.api.Camera;

/**
 *
 */
public final class RenderUtil {
    private RenderUtil() {
    }

    public static Vector2f computeProjection(Vector2f planes) {
        return computeProjection(planes.getX(), planes.getY());
    }

    public static Vector2f computeProjection(float near, float far) {
        return new Vector2f(far / (far - near), (-far * near) / (far - near));
    }

    public static Vector2f getPlanes(Camera camera) {
        return getPlanes(camera.getProjectionMatrix());
    }

    public static Vector2f getPlanes(Matrix4f projection) {
        // We can find the planes faster if we solve for the particular projection matrix
        if (projection.get(3, 2) == -1) {
            /*
                Perspective matrix:
                Solve the equations for near and far
                m22 = (far + near) / (near - far)
                m23 = 2 * far * near / (near - far)

                m22 * near - m22 * far = far + near
                m23 * near - m23 * far = 2 * far * near

                -m22 * far - far =  near - m22 * near
                -m23 * far = 2 * far * near - m23 * near

                m22 * far + far = (m22 - 1) * near
                m23 * far = (m23 - 2 * far) * near

                (m22 * far + far) / (m22 - 1) = near
                (m23 * far) / (m23 - 2 * far) = near

                (m23 * far) / (m23 - 2 * far) = (m22 * far + far) / (m22 - 1)
                (m23 * far) * (m22 - 1) = (m22 * far + far) * (m23 - 2 * far)

                m23 * far * m22 - m23 * far = -2 * far * far * m22 - 2 * far * far + far * m22 * m23 + far * m23
                -2 * m23 * far = -2 * far * far * m22 - 2 * far * far
                -2 * m23 = -2 * far * m22 - 2 * far
                m23 = far * m22 + far
                m23 = (m22 + 1) * far
                far = m23 / (m22 + 1)

                (m23 * far) / (m23 - 2 * far) = near
                (m23 * m23 / (m22 + 1)) / (m23 - 2 * m23 / (m22 + 1)) = near
                (m23 * m23 / (m22 + 1)) / (m23 * (m22 + 1) / (m22 + 1) - 2 * m23 / (m22 + 1)) = near
                (m23 * m23 / (m22 + 1)) / ((m23 * (m22 + 1) - 2 * m23) / (m22 + 1)) = near
                (m23 * m23 * (m22 + 1)) / ((m22 + 1) * (m23 * (m22 + 1) - 2 * m23)) = near
                (m23 * m23) / (m23 * (m22 + 1) - 2 * m23) = near
                (m23 * m23) / (m23 * (m22 + 1 - 2)) = near
                m23 / (m22 + 1 - 2) = near
                near = m23 / (m22 - 1)
            */
            final float m22 = projection.get(2, 2);
            final float m23 = projection.get(2, 3);
            return new Vector2f(m23 / (m22 - 1), m23 / (m22 + 1));
        } else if (projection.get(3, 3) == 1) {
            /*
                Orthographic matrix:
                Solve the equations for near and far
                m22 = -2 / (far - near)
                m23 = -(far + near) / (far - near)

                m22 * far - m22 * near = -2
                m23 * far - m23 * near = -far - near

                m22 * far + 2 = m22 * near
                m23 * far + far = m23 * near - near

                (m22 * far + 2) / m22 = near
                (m23 * far + far) / (m23 - 1) = near

                (m22 * far + 2) / m22 = (m23 * far + far) / (m23 - 1)
                (m22 * far + 2) * (m23 - 1) = (m23 * far + far) * m22
                m22 * far * m23 - m22 * far + 2 * m23 - 2 = m23 * far * m22 + far * m22
                2 * m23 - 2 = 2 * far * m22
                m23 - 1 = far * m22
                far = (m23 - 1) / m22

                (m22 * far + 2) / m22 = near
                (m22 * (m23 - 1) / m22 + 2) / m22 = near
                ((m23 - 1) + 2) / m22 = near
                near = (m23 + 1) / m22
            */
            final float m22 = projection.get(2, 2);
            final float m23 = projection.get(2, 3);
            return new Vector2f((m23 + 1) / m22, (m23 - 1) / m22);
        } else {
            final ViewFrustum frustum = new ViewFrustum();
            frustum.update(projection, Matrix4f.IDENTITY);
            return new Vector2f(frustum.getNearPlane(), frustum.getFarPlane());
        }
    }

    public static float getFieldOfView(Camera camera) {
        return getFieldOfView(camera.getProjectionMatrix());
    }

    public static float getFieldOfView(Matrix4f projection) {
        /*
            m11 = 1 / tan(fov * (PI / 360))
            1 / m11 = tan(fov * (PI / 360))
            atan(1 / m11) = fov * (PI / 360)

            In degrees:
            fov = 360 * atan(1 / m11) / PI

            In radians:
            fov = (PI / 180) * 360 * atan(1 / m11) / PI
            fov = 2PI * atan(1 / m11) / PI
            fov = 2 * atan(1 / m11)
        */
        return (float) (2 * TrigMath.atan(1 / projection.get(1, 1)));
    }
}
