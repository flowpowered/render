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
package com.flowpowered.render.impl;

import java.util.Arrays;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.render.GraphNode;
import com.flowpowered.render.RenderGraph;
import com.flowpowered.render.RenderUtil;

import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.Rectangle;

/**
 *
 */
public class LightingNode extends GraphNode {
    public static Vector3f DEFAULT_LIGHT_DIRECTION = Vector3f.ONE.negate().normalize();
    private final FrameBuffer frameBuffer;
    private final Texture colorsOutput;
    private final Material material;
    private final Pipeline pipeline;
    private final Rectangle outputSize = new Rectangle();
    private final Matrix4Uniform viewMatrixUniform = new Matrix4Uniform("viewMatrix", Matrix4f.IDENTITY);
    private final FloatUniform aspectRatioUniform = new FloatUniform("aspectRatio", 1);
    private final FloatUniform tanHalfFOVUniform = new FloatUniform("tanHalfFOV", 1);
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", DEFAULT_LIGHT_DIRECTION);

    public LightingNode(RenderGraph graph, String name) {
        super(graph, name);
        final Context context = graph.getContext();
        // Create the colors texture
        colorsOutput = context.newTexture();
        colorsOutput.create();
        colorsOutput.setFormat(InternalFormat.RGBA8);
        colorsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        colorsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the frame buffer
        frameBuffer = context.newFrameBuffer();
        frameBuffer.create();
        frameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        // Create the material
        material = new Material(graph.getProgram("lighting"));
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(viewMatrixUniform);
        uniforms.add(aspectRatioUniform);
        uniforms.add(tanHalfFOVUniform);
        uniforms.add(lightDirectionUniform);
        // Create the screen model
        final Model model = new Model(graph.getScreen(), material);
        // Create the pipeline
        pipeline = new PipelineBuilder().useViewPort(outputSize).bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
    }

    @Override
    public void update() {
        updateCamera(this.<Camera>getAttribute("camera"));
        updateOutputSize(this.<Vector2i>getAttribute("outputSize"));
    }

    private void updateCamera(Camera camera) {
        tanHalfFOVUniform.set(TrigMath.tan(RenderUtil.getFieldOfView(camera) / 2));
    }

    private void updateOutputSize(Vector2i size) {
        if (size.getX() == outputSize.getWidth() && size.getY() == outputSize.getHeight()) {
            return;
        }
        outputSize.setSize(size);
        colorsOutput.setImageData(null, size.getX(), size.getY());
    }

    @Override
    protected void render() {
        final Texture depths = material.getTexture(2);
        aspectRatioUniform.set((float) depths.getWidth() / depths.getHeight());
        updateLightDirection(getAttribute("lightDirection", DEFAULT_LIGHT_DIRECTION));
        viewMatrixUniform.set(this.<Camera>getAttribute("camera").getViewMatrix());
        pipeline.run(graph.getContext());
    }

    private void updateLightDirection(Vector3f lightDirection) {
        lightDirectionUniform.set(lightDirection);
    }

    @Override
    protected void destroy() {
        frameBuffer.destroy();
        colorsOutput.destroy();
    }

    @Input("colors")
    public void setColorsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(0, texture);
    }

    @Input("normals")
    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(1, texture);
    }

    @Input("depths")
    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(2, texture);
    }

    @Input("materials")
    public void setMaterialsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(3, texture);
    }

    @Input("occlusions")
    public void setOcclusionsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(4, texture);
    }

    @Input("shadows")
    public void setShadowsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(5, texture);
    }

    @Output("colors")
    public Texture getColorsOutput() {
        return colorsOutput;
    }
}
