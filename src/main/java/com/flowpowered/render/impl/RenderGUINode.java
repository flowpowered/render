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

import com.flowpowered.render.GraphNode;
import com.flowpowered.render.RenderGraph;

import org.spout.renderer.api.Action.RenderModelsAction;
import org.spout.renderer.api.Action.SetCameraAction;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.Rectangle;

/**
 *
 */
public class RenderGUINode extends GraphNode {
    private final Material material;
    private final SetCameraAction setCamera = new SetCameraAction(null);
    private final RenderModelsAction renderModels = new RenderModelsAction(null);
    private final Pipeline pipeline;
    private final Rectangle inputSize = new Rectangle();

    public RenderGUINode(RenderGraph graph, String name) {
        super(graph, name);
        material = new Material(graph.getProgram("screen"));
        final Model model = new Model(graph.getScreen(), material);
        pipeline = new PipelineBuilder().doAction(setCamera).useViewPort(inputSize).clearBuffer().renderModels(Arrays.asList(model)).doAction(renderModels).build();
    }

    @Override
    public void update() {
        updateModels(this.<Collection<Model>>getAttribute("guiModels"));
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
    }

    @Input("colors")
    public void setColorsInput(Texture colorsInput) {
        material.addTexture(0, colorsInput);
        inputSize.setSize(colorsInput.getSize());
        setCamera.setCamera(Camera.createOrthographic(1, 0, (float) colorsInput.getHeight() / colorsInput.getWidth(), 0, 0, 1));
    }

    public Camera getCamera() {
        return setCamera.getCamera();
    }
}
