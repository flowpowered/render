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
import java.util.Collection;
import java.util.Collections;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.render.GraphNode;
import com.flowpowered.render.RenderGraph;

import com.flowpowered.caustic.api.Action.RenderModelsAction;
import com.flowpowered.caustic.api.Action.SetCameraAction;
import com.flowpowered.caustic.api.Camera;
import com.flowpowered.caustic.api.Material;
import com.flowpowered.caustic.api.Pipeline;
import com.flowpowered.caustic.api.Pipeline.PipelineBuilder;
import com.flowpowered.caustic.api.gl.Context;
import com.flowpowered.caustic.api.gl.Context.BlendFunction;
import com.flowpowered.caustic.api.gl.Context.Capability;
import com.flowpowered.caustic.api.gl.FrameBuffer;
import com.flowpowered.caustic.api.gl.FrameBuffer.AttachmentPoint;
import com.flowpowered.caustic.api.gl.Program;
import com.flowpowered.caustic.api.gl.Texture;
import com.flowpowered.caustic.api.gl.Texture.FilterMode;
import com.flowpowered.caustic.api.gl.Texture.InternalFormat;
import com.flowpowered.caustic.api.model.Model;
import com.flowpowered.caustic.api.util.Rectangle;

/**
 *
 */
public class RenderTransparentModelsNode extends GraphNode {
    private final Texture weightedColors;
    private final Texture layerCounts;
    private final FrameBuffer weightedSumFrameBuffer;
    private final FrameBuffer frameBuffer;
    private Texture colors;
    private final RenderModelsAction renderModels = new RenderModelsAction(null);
    private final SetCameraAction setCamera = new SetCameraAction(null);
    private final Rectangle outputSize = new Rectangle();
    private final Pipeline pipeline;

    public RenderTransparentModelsNode(RenderGraph graph, String name) {
        super(graph, name);
        final Context context = graph.getContext();
        // Create the weighted colors texture
        weightedColors = context.newTexture();
        weightedColors.create();
        weightedColors.setFormat(InternalFormat.RGBA16F);
        weightedColors.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        // Create the layer counts texture
        layerCounts = context.newTexture();
        layerCounts.create();
        layerCounts.setFormat(InternalFormat.R16F);
        layerCounts.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        // Create the weighted sum frame buffer
        weightedSumFrameBuffer = context.newFrameBuffer();
        weightedSumFrameBuffer.create();
        weightedSumFrameBuffer.attach(AttachmentPoint.COLOR0, weightedColors);
        weightedSumFrameBuffer.attach(AttachmentPoint.COLOR1, layerCounts);
        // Create the frame buffer
        frameBuffer = context.newFrameBuffer();
        frameBuffer.create();
        // Create the material
        final Material material = new Material(graph.getProgram("transparencyBlending"));
        material.addTexture(0, weightedColors);
        material.addTexture(1, layerCounts);
        // Create the screen model
        final Model model = new Model(graph.getScreen(), material);
        // Create the pipeline
        pipeline = new PipelineBuilder()
                .useViewPort(outputSize).doAction(setCamera)
                .disableDepthMask().disableCapabilities(Capability.CULL_FACE).enableCapabilities(Capability.BLEND)
                .setBlendingFunctions(BlendFunction.GL_ONE, BlendFunction.GL_ONE)
                .bindFrameBuffer(weightedSumFrameBuffer).clearBuffer().doAction(renderModels)
                .enableCapabilities(Capability.CULL_FACE).enableDepthMask()
                .setBlendingFunctions(BlendFunction.GL_ONE_MINUS_SRC_ALPHA, BlendFunction.GL_SRC_ALPHA)
                .bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer)
                .disableCapabilities(Capability.BLEND).enableDepthMask()
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update() {
        updateCamera(this.<Camera>getAttribute("camera"));
        updateModels(getAttribute("transparentModels", (Collection<Model>) Collections.EMPTY_LIST));
    }

    private void updateCamera(Camera camera) {
        setCamera.setCamera(camera);
    }

    private void updateModels(Collection<Model> models) {
        renderModels.setModels(models);
    }

    @Override
    protected void render() {
        // Update the size of the textures to match the input, if necessary
        updateAuxTextureSizes();
        // Upload the light direction uniform
        final Program weightedSumProgram = graph.getProgram("weightedSum");
        weightedSumProgram.use();
        weightedSumProgram.setUniform("lightDirection", getAttribute("lightDirection", LightingNode.DEFAULT_LIGHT_DIRECTION));
        // Render
        pipeline.run(graph.getContext());
    }

    private void updateAuxTextureSizes() {
        final Vector2i size = colors.getSize();
        if (!size.equals(outputSize.getSize())) {
            outputSize.setSize(size);
            final int width = size.getX();
            final int height = size.getY();
            weightedColors.setImageData(null, width, height);
            layerCounts.setImageData(null, width, height);
        }
    }

    @Override
    protected void destroy() {
        weightedColors.destroy();
        layerCounts.destroy();
        weightedSumFrameBuffer.destroy();
        frameBuffer.destroy();
    }

    @Input("colors")
    public void setColorsInput(Texture texture) {
        texture.checkCreated();
        colors = texture;
        frameBuffer.attach(AttachmentPoint.COLOR0, texture);
        updateAuxTextureSizes();
    }

    @Input("depths")
    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        weightedSumFrameBuffer.attach(AttachmentPoint.DEPTH, texture);
    }

    @Output("colors")
    public Texture getColorsOutput() {
        return colors;
    }
}
