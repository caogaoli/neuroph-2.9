/**
 * Copyright 2010 Neuroph Project http://neuroph.sourceforge.net
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.neuroph.core.events;

/**
 * This interface is implemented by classes who are listening to neural network events events (to be defined)
 * NeuralNetworkEvent class holds the information about event.
 *
 * @author Zoran Sevarac <sevarac@gmail.com>
 */
@FunctionalInterface
public interface NeuralNetworkEventListener extends java.util.EventListener {

    public void handleNeuralNetworkEvent(NeuralNetworkEvent event);
}
