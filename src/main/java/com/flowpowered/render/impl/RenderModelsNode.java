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

import java.util.Collection;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.render.GraphNode;
import com.flowpowered.render.RenderGraph;

import org.spout.renderer.api.Action.RenderModelsAction;
import org.spout.renderer.api.Action.SetCameraAction;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
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
public class RenderModelsNode extends GraphNode {
    private final FrameBuffer frameBuffer;
    private final Texture colorsOutput;
    private final Texture normalsOutput;
    private final Texture depthsOutput;
    private final Texture vertexNormalsOutput;
    private final Texture materialsOutput;
    private final RenderModelsAction renderModels = new RenderModelsAction(null);
    private final SetCameraAction setCamera = new SetCameraAction(null);
    private final Rectangle outputSize = new Rectangle();
    private final Pipeline pipeline;

    public RenderModelsNode(RenderGraph graph, String name) {
        super(graph, name);
        final Context context = graph.getContext();
        // Create the colors texture
        colorsOutput = context.newTexture();
        colorsOutput.create();
        colorsOutput.setFormat(InternalFormat.RGBA8);
        colorsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        colorsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the normals texture
        normalsOutput = context.newTexture();
        normalsOutput.create();
        normalsOutput.setFormat(InternalFormat.RGBA8);
        normalsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        // Create the depths texture
        depthsOutput = context.newTexture();
        depthsOutput.create();
        depthsOutput.setFormat(InternalFormat.DEPTH_COMPONENT32);
        depthsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        depthsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the vertex normals texture
        vertexNormalsOutput = context.newTexture();
        vertexNormalsOutput.create();
        vertexNormalsOutput.setFormat(InternalFormat.RGBA8);
        vertexNormalsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        // Create the materials texture
        materialsOutput = context.newTexture();
        materialsOutput.create();
        materialsOutput.setFormat(InternalFormat.RGBA8);
        materialsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        // Create the frame buffer
        frameBuffer = context.newFrameBuffer();
        frameBuffer.create();
        frameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR1, normalsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR2, vertexNormalsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR3, materialsOutput);
        frameBuffer.attach(AttachmentPoint.DEPTH, depthsOutput);
        // Create the pipeline
        pipeline = new PipelineBuilder().doAction(setCamera).useViewPort(outputSize).bindFrameBuffer(frameBuffer).clearBuffer().doAction(renderModels).unbindFrameBuffer(frameBuffer).build();
    }

    @Override
    public void update() {
        updateCamera(this.<Camera>getAttribute("camera"));
        updateOutputSize(this.<Vector2i>getAttribute("outputSize"));
        updateModels(this.<Collection<Model>>getAttribute("models"));
    }

    private void updateCamera(Camera camera) {
        setCamera.setCamera(camera);
    }

    private void updateOutputSize(Vector2i size) {
        if (size.getX() == outputSize.getWidth() && size.getY() == outputSize.getHeight()) {
            return;
        }
        outputSize.setSize(size);
        final int width = size.getX();
        final int height = size.getY();
        colorsOutput.setImageData(null, width, height);
        normalsOutput.setImageData(null, width, height);
        depthsOutput.setImageData(null, width, height);
        vertexNormalsOutput.setImageData(null, width, height);
        materialsOutput.setImageData(null, width, height);
    }

    private void updateModels(Collection<Model> models) {
        renderModels.setModels(models);
    }

    @Override
    protected void render() {
        pipeline.run(graph.getContext());
    }

    @Override
    protected void destroy() {
        frameBuffer.destroy();
        colorsOutput.destroy();
        normalsOutput.destroy();
        depthsOutput.destroy();
        vertexNormalsOutput.destroy();
        materialsOutput.destroy();
    }

    @Output("colors")
    public Texture getColorsOutput() {
        return colorsOutput;
    }

    @Output("normals")
    public Texture getNormalsOutput() {
        return normalsOutput;
    }

    @Output("depths")
    public Texture getDepthsOutput() {
        return depthsOutput;
    }

    @Output("vertexNormals")
    public Texture getVertexNormalsOutput() {
        return vertexNormalsOutput;
    }

    @Output("materials")
    public Texture getMaterialsOutput() {
        return materialsOutput;
    }
}
