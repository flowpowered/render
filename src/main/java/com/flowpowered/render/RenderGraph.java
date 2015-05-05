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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.flowpowered.math.vector.Vector2f;

import com.flowpowered.caustic.api.Creatable;
import com.flowpowered.caustic.api.data.ShaderSource;
import com.flowpowered.caustic.api.gl.Context;
import com.flowpowered.caustic.api.gl.Program;
import com.flowpowered.caustic.api.gl.Shader;
import com.flowpowered.caustic.api.gl.Texture;
import com.flowpowered.caustic.api.gl.Texture.FilterMode;
import com.flowpowered.caustic.api.gl.Texture.Format;
import com.flowpowered.caustic.api.gl.Texture.InternalFormat;
import com.flowpowered.caustic.api.gl.VertexArray;
import com.flowpowered.caustic.api.util.CausticUtil;
import com.flowpowered.caustic.api.util.MeshGenerator;

/**
 *
 */
public class RenderGraph extends Creatable implements AttributeHolder {
    private final Context context;
    private final String shaderSrcDir;
    private final Map<String, Program> programs = new HashMap<>();
    private final VertexArray screen;
    private final Texture whiteDummy, blackDummy;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final SortedSet<Stage> stages = new TreeSet<>();
    private final Map<String, Object> attributes = new HashMap<>();

    public RenderGraph(Context context, String shaderSrcDir) {
        this.context = context;
        this.shaderSrcDir = shaderSrcDir;
        screen = context.newVertexArray();
        whiteDummy = context.newTexture();
        blackDummy = context.newTexture();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Render graph has already been created");
        }
        // Create the screen for deferred rendering
        screen.create();
        screen.setData(MeshGenerator.generatePlane(new Vector2f(2, 2)));
        // Create a byte buffer to store the dummy texture data
        final ByteBuffer buffer = CausticUtil.createByteBuffer(4);
        // Create the white dummy
        whiteDummy.create();
        whiteDummy.setFormat(Format.RGBA, InternalFormat.RGBA8);
        whiteDummy.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        buffer.putInt(0xFFFFFFFF);
        buffer.flip();
        whiteDummy.setImageData(buffer, 1, 1);
        // Clear the buffer for the black dummy
        buffer.clear();
        // Create the black dummy
        blackDummy.create();
        blackDummy.setFormat(Format.RGBA, InternalFormat.RGBA8);
        blackDummy.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        buffer.putInt(0x00000000);
        buffer.flip();
        blackDummy.setImageData(buffer, 1, 1);
        // Set the state to created
        super.create();
    }

    @Override
    public void destroy() {
        checkCreated();
        screen.destroy();
        for (GraphNode node : nodes.values()) {
            node.destroy();
        }
        nodes.clear();
        stages.clear();
        for (Program program : programs.values()) {
            for (Shader shader : program.getShaders()) {
                shader.destroy();
            }
            program.destroy();
        }
        programs.clear();
        attributes.clear();
        super.destroy();
    }

    public void updateAll() {
        for (GraphNode node : nodes.values()) {
            node.update();
        }
    }

    public void build() {
        stages.clear();
        final Set<GraphNode> toBuild = new HashSet<>(nodes.values());
        final Set<GraphNode> previous = new HashSet<>();
        int i = 0;
        Stage current = new Stage(i++);
        while (true) {
            for (Iterator<GraphNode> iterator = toBuild.iterator(); iterator.hasNext(); ) {
                final GraphNode node = iterator.next();
                if (previous.containsAll(node.getConnectedInputs().values())) {
                    current.addNode(node);
                    iterator.remove();
                }
            }
            if (current.getNodes().isEmpty()) {
                return;
            }
            previous.addAll(current.getNodes());
            stages.add(current);
            if (toBuild.isEmpty()) {
                break;
            }
            current = new Stage(i++);
        }
    }

    public void render() {
        for (Stage stage : stages) {
            stage.render();
        }
        context.updateDisplay();
    }

    public void addNode(GraphNode node) {
        nodes.put(node.getName(), node);
    }

    @SuppressWarnings("unchecked")
    public <N extends GraphNode> N getNode(String name) {
        try {
            return (N) nodes.get(name);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Requested node is not of requested type");
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    protected Object getAttributeRaw(String name) {
        return attributes.get(name);
    }

    public <T> T getAttribute(String name) {
        return getAttribute(name, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, T _default) {
        final Object attribute = getAttributeRaw(name);
        if (attribute == null) {
            if (_default == null) {
                throw new IllegalArgumentException("Attribute \"" + name + "\" is missing or null and no default has been provided");
            } else {
                return _default;
            }
        }
        try {
            return (T) attribute;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Requested attribute is not of requested type");
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public Context getContext() {
        return context;
    }

    public VertexArray getScreen() {
        return screen;
    }

    public Texture getWhiteDummy() {
        return whiteDummy;
    }

    public Texture getBlackDummy() {
        return blackDummy;
    }

    public Program getProgram(String name) {
        final Program program = programs.get(name);
        if (program == null) {
            return loadProgram(name);
        }
        return program;
    }

    private Program loadProgram(String name) {
        final String shaderPath = shaderSrcDir + '/' + name;
        final Shader vertex = context.newShader();
        vertex.create();
        vertex.setSource(new ShaderSource(getClass().getResourceAsStream(shaderPath + ".vert")));
        vertex.compile();
        final Shader fragment = context.newShader();
        fragment.create();
        fragment.setSource(new ShaderSource(getClass().getResourceAsStream(shaderPath + ".frag")));
        fragment.compile();
        final Program program = context.newProgram();
        program.create();
        program.attachShader(vertex);
        program.attachShader(fragment);
        program.link();
        programs.put(name, program);
        return program;
    }

    private static class Stage implements Comparable<Stage> {
        private final Set<GraphNode> nodes = new HashSet<>();
        private final int number;

        private Stage(int number) {
            this.number = number;
        }

        private void addNode(GraphNode node) {
            nodes.add(node);
        }

        public Set<GraphNode> getNodes() {
            return nodes;
        }

        private void render() {
            for (GraphNode node : nodes) {
                node.render();
            }
        }

        private int getNumber() {
            return number;
        }

        @Override
        public int compareTo(Stage o) {
            return number - o.getNumber();
        }
    }
}
