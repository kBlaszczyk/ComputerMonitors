/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.computer.monitor.system.server;

import com.google.common.base.Predicate;
import org.terasology.computer.monitor.component.ComputerMonitorComponent;
import org.terasology.computer.monitor.component.ComputerMonitorDataHolderComponent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.Region3i;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock2.MultiBlockRegistry;
import org.terasology.multiBlock2.component.MultiBlockCandidateComponent;
import org.terasology.multiBlock2.event.BeforeMultiBlockUnloaded;
import org.terasology.multiBlock2.event.MultiBlockFormed;
import org.terasology.multiBlock2.event.MultiBlockLoaded;
import org.terasology.multiBlock2.recipe.UniformMultiBlockRecipe;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ComputerMonitorServerSystem extends BaseComponentSystem {
    public static final String MONITOR_MULTI_BLOCK_CANDIDATE_KEY = "ComputerMonitors:monitor";
    public static final String MONITOR_MULTI_BLOCK_TYPE = "ComputerMonitors:monitor";
    public static final int MAX_MONITOR_DIMENSION = 5;
    @In
    private MultiBlockRegistry multiBlockRegistry;
    @In
    private BlockEntityRegistry blockEntityRegistry;

    @Override
    public void preBegin() {
        multiBlockRegistry.registerMultiBlockType(MONITOR_MULTI_BLOCK_CANDIDATE_KEY,
                new UniformMultiBlockRecipe<Region3iMultiBlockDefinition>(blockEntityRegistry, new Predicate<EntityRef>() {
                    @Override
                    public boolean apply(@Nullable EntityRef input) {
                        MultiBlockCandidateComponent multiBlockCandidateComponent = input.getComponent(MultiBlockCandidateComponent.class);
                        return multiBlockCandidateComponent != null
                                && multiBlockCandidateComponent.getType().contains(MONITOR_MULTI_BLOCK_CANDIDATE_KEY);
                    }
                }, new Predicate<Vector3i>() {
                    @Override
                    public boolean apply(@Nullable Vector3i input) {
                        int maxValue = Math.max(Math.max(input.x, input.z), input.y);
                        int minValue = Math.min(input.x, input.z);
                        return minValue == 1 && maxValue <= MAX_MONITOR_DIMENSION;
                    }
                }) {
                    @Override
                    protected Region3iMultiBlockDefinition createMultiBlockDefinition(Region3i multiBlockRegion) {
                        Vector3i mainBlock = multiBlockRegion.min();

                        List<Vector3i> memberLocations = new LinkedList<Vector3i>();
                        for (Vector3i vector3i : multiBlockRegion) {
                            if (!vector3i.equals(mainBlock)) {
                                memberLocations.add(vector3i);
                            }
                        }

                        return new Region3iMultiBlockDefinition(MONITOR_MULTI_BLOCK_TYPE, mainBlock, memberLocations, multiBlockRegion);
                    }
                });
    }

    @ReceiveEvent
    public void computerMonitorMultiBlockCreated(MultiBlockFormed<Region3iMultiBlockDefinition> event, EntityRef multiBlockEntity) {
        if (event.getType().equals(MONITOR_MULTI_BLOCK_TYPE)) {
            Region3i region = event.getMultiBlockDefinition().getRegion();

            int height = region.sizeY();
            int width = Math.max(region.sizeX(), region.sizeZ());

            int lineCount = height * 5;
            int charsInLine = width * 8;

            int x = region.sizeX();
            int z = region.sizeZ();

            Vector3i size = new Vector3i(x, height, z);

            Side front;
            if (x>z) {
                front = Side.FRONT;
            } else {
                front = Side.LEFT;
            }

            List<String> lines = new ArrayList<>(lineCount);
            for (int i=0; i<lineCount; i++) {
                lines.add("");
            }

            ComputerMonitorComponent component = new ComputerMonitorComponent(size, front, charsInLine, lineCount, new ArrayList<>(lineCount));
            multiBlockEntity.addComponent(component);
        }
    }

    @ReceiveEvent
    public void computerMonitorMultiBlockUnloaded(BeforeMultiBlockUnloaded event, EntityRef multiBlockEntity) {
        if (event.getType().equals(MONITOR_MULTI_BLOCK_TYPE)) {
            EntityRef mainBlockEntity = event.getMainBlockEntity();

            ComputerMonitorComponent monitor = multiBlockEntity.getComponent(ComputerMonitorComponent.class);

            ComputerMonitorDataHolderComponent component = new ComputerMonitorDataHolderComponent(
                    monitor.getMonitorSize(), monitor.getFront(), monitor.getCharactersInLineCount(), monitor.getLineCount());
            mainBlockEntity.addComponent(component);
        }
    }

    @ReceiveEvent
    public void computerMonitorMultiBlockLoaded(MultiBlockLoaded event, EntityRef multiBlockEntity) {
        if (event.getType().equals(MONITOR_MULTI_BLOCK_TYPE)) {
            EntityRef mainBlockEntity = event.getMainBlockEntity();
            ComputerMonitorDataHolderComponent component = mainBlockEntity.getComponent(ComputerMonitorDataHolderComponent.class);

            int lineCount = component.getLineCount();

            List<String> lines = new ArrayList<>(lineCount);
            for (int i=0; i<lineCount; i++) {
                lines.add("");
            }

            ComputerMonitorComponent monitor = new ComputerMonitorComponent(
                    component.getMonitorSize(), component.getFront(), component.getCharactersInLineCount(), lineCount, lines);
            multiBlockEntity.addComponent(monitor);

            mainBlockEntity.removeComponent(ComputerMonitorDataHolderComponent.class);
        }
    }
}
