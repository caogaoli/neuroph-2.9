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
package org.neuroph.core.learning;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.neuroph.core.Connection;
import org.neuroph.core.Layer;
import org.neuroph.core.Neuron;
import org.neuroph.core.Weight;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.learning.error.ErrorFunction;
import org.neuroph.core.learning.error.MeanSquaredError;
import org.neuroph.core.learning.stop.MaxErrorStop;

/**
 * 监督学习.
 * <p>
 * Base class for all supervised learning algorithms.
 * It extends IterativeLearning, and provides general supervised learning principles.
 * Based on Template Method Pattern with abstract method calculateWeightChanges
 * <p>
 * TODO: random pattern order
 *
 * @author Zoran Sevarac <sevarac@gmail.com>
 * @modifier caogl<caogaoli058 @ gmail.com>
 */
abstract public class SupervisedLearning extends IterativeLearning implements Serializable {

    /**
     * The class fingerprint that is set to indicate serialization
     * compatibility with a previous version of the class
     */
    private static final long serialVersionUID = 3L;

    /**
     * Total network error in previous epoch
     * 记录上一轮迭代的误差数值大小.
     */
    protected transient double previousEpochError;

    /**
     * Max allowed network error (condition to stop learning)
     */
    protected double maxError = 0.01d;

    /**
     * Stopping condition: training stops if total network error change is smaller than minErrorChange
     * for minErrorChangeIterationsLimit number of iterations
     */
    private double minErrorChange = Double.POSITIVE_INFINITY;

    /**
     * Stopping condition: training stops if total network error change is smaller than minErrorChange
     * for minErrorChangeStopIterations number of iterations
     */
    private int minErrorChangeIterationsLimit = Integer.MAX_VALUE;

    /**
     * Count iterations where error change is smaller then minErrorChange.
     */
    private transient int minErrorChangeIterationsCount;

    /**
     * Setting to determine if learning (weights update) is in batch mode.
     * False by default.
     */
    private boolean batchMode = false;

    // 误差计算函数.
    private ErrorFunction errorFunction;

    /**
     * Creates new supervised learning rule
     */
    public SupervisedLearning() {
        super();
        errorFunction = new MeanSquaredError();
        stopConditions.add(new MaxErrorStop(this));
    }

    /**
     * This method should implement the weights update procedure for the whole network for the given output error vector.
     *
     * @param outputError output error vector for some network input (aka. patternError, network error)
     *                    usually the difference between desired and actual output
     */
    abstract protected void calculateWeightChanges(double[] outputError);

    /**
     * <keep-note>这里使用多个参数的learn(...)重载方法的方式值得商榷？really is the best ways?</keep-note>
     * or you can have try learn(DataSet trainingSet, Properties basedSupervisedLearningProperties);
     * or learn(baseLearningContext) and baseLearningContext is an instance like ApplicationContext in Spring.
     */

    /**
     * Trains network for the specified training set and maxError
     *
     * @param trainingSet training set to learn
     * @param maxError    learning stop condition. If maxError is reached learning stops
     */
    public final void learn(DataSet trainingSet, double maxError) {
        this.maxError = maxError;
        learn(trainingSet);
    }

    /**
     * Trains network for the specified training set, maxError and number of iterations
     *
     * @param trainingSet   training set to learn
     * @param maxError      learning stop condition. if maxError is reached learning stops
     * @param maxIterations maximum number of learning iterations
     */
    public final void learn(DataSet trainingSet, double maxError, int maxIterations) {
        this.trainingSet = trainingSet;
        this.maxError = maxError;
        setMaxIterations(maxIterations);
        learn(trainingSet);
    }

    @Override
    protected void onStart() {
        // reset iteration counter
        super.onStart();
        minErrorChangeIterationsCount = 0;
        previousEpochError = 0d;
    }

    @Override
    protected void beforeEpoch() {
        previousEpochError = errorFunction.getTotalError();
        errorFunction.reset();
    }

    @Override
    protected void afterEpoch() {
        // calculate abs error change and count iterations if its below specified min error change (used for stop condition)
        double absErrorChange = Math.abs(previousEpochError - errorFunction.getTotalError());
        if (absErrorChange <= this.minErrorChange) {
            minErrorChangeIterationsCount++;
        } else {
            minErrorChangeIterationsCount = 0;
        }

        // if learning is performed in batch mode, apply accumulated weight changes from this epoch        
        if (batchMode == true) {
            doBatchWeightsUpdate();
        }
    }

    /**
     * This method implements basic logic for one learning epoch for the
     * supervised learning algorithms. Epoch is the one pass through the
     * training set. This method iterates through the training set
     * and trains network for each element. It also sets flag if conditions
     * to stop learning has been reached: network error below some allowed
     * value, or maximum iteration count
     *
     * @param trainingSet training set for training network
     */
    @Override
    public void doLearningEpoch(DataSet trainingSet) {
        Iterator<DataSetRow> iterator = trainingSet.iterator();
        // 遍历全部的训练集数据，进行模型训练.
        // iterate all elements from training set - maybe remove isStopped from here
        while (iterator.hasNext() && !isStopped()) {
            DataSetRow dataSetRow = iterator.next();
            // learn current input/output pattern defined by SupervisedTrainingElement
            // 一个一个样本进行学习.
            learnPattern(dataSetRow);
        }
    }

    /**
     * Trains network with the input and desired output pattern from the specified training element
     *
     * @param trainingElement supervised training element which contains input and desired output
     */
    protected final void learnPattern(DataSetRow trainingElement) {
        // 为神经网络设置输入.
        neuralNetwork.setInput(trainingElement.getInput());
        // 网络神经输入信号向前传播，feedforward.
        neuralNetwork.calculate();
        // 获取神经网络的计算输出.
        double[] output = neuralNetwork.getOutput();
        // 累计每个样本的训练误差，其中errorFunction的实例对象会去记录totalError和patternCount两个关键指标.
        double[] patternError = errorFunction.addPatternError(output, trainingElement.getDesiredOutput());
        // 实现神经元之间权值的更新，不同的神经网络其权值更新及其传播的方式存在差异，需要分别实现.
        calculateWeightChanges(patternError);
        /**
         * todo
         * 在监督学习下:
         * 注意，doBatchWeightsUpdate()和applyWeightChanges()处理的场景和方法触发的位置完全不同.
         * 原作者在applyWeightChanges()、doBatchWeightsUpdate()的设计有一定的问题，导致读者哟有点乱.
         * 改进设计如下：
         * step1：设计一个WeightUpdate相关接口；
         * step2：其具体实现者粗略划分两个实现类，通过事件监听，触发不同阶段的权值更新.
         * step3：类似于 afterLearnPattern，在父类或者定级类中定义模板方法.
         */
        if (!batchMode) {
            // batch mode updates are done i doBatchWeightsUpdate
            applyWeightChanges();
        }
    }

    /**
     * this method updates network weight not in batch mode - update the weight change by each input data Patten.
     * and it will be trigger off the weight update operation after each patten finish learning operation。
     */
    private void applyWeightChanges() {
        List<Layer> layers = neuralNetwork.getLayers();
        for (int i = neuralNetwork.getLayersCount() - 1; i > 0; i--) {
            // iterate neurons at each layer
            for (Neuron neuron : layers.get(i)) {
                // iterate connections/weights for each neuron
                for (Connection connection : neuron.getInputConnections()) {
                    // for each connection weight apply accumulated weight change
                    Weight weight = connection.getWeight();
                    if (!isBatchMode()) {
                        weight.value += weight.weightChange;
                    } else {
                        weight.value += (weight.weightChange / getTrainingSet().size());
                    }
                    // reset deltaWeight
                    weight.weightChange = 0;
                }
            }
        }
    }

    /**
     * This method updates network weights in batch mode - use accumulated weights change stored in Weight.deltaWeight
     * It is executed after each learning epoch, only if learning is done in batch mode.
     *
     * @see SupervisedLearning#doLearningEpoch(org.neuroph.core.data.DataSet)
     */
    protected void doBatchWeightsUpdate() {
        // iterate layers from output to input
        List<Layer> layers = neuralNetwork.getLayers();
        for (int i = neuralNetwork.getLayersCount() - 1; i > 0; i--) {
            // iterate neurons at each layer
            for (Neuron neuron : layers.get(i).getNeurons()) {
                // iterate connections/weights for each neuron
                for (Connection connection : neuron.getInputConnections()) {
                    // for each connection weight apply accumulated weight change
                    Weight weight = connection.getWeight();
                    // apply delta weight which is the sum of delta weights in batch mode    - TODO: add mini batch
                    weight.value += weight.weightChange / getTrainingSet().size();
                    weight.weightChange = 0; // reset deltaWeight
                }
            }
        }
    }

    /**
     * Returns true if learning is performed in batch mode, false otherwise
     *
     * @return true if learning is performed in batch mode, false otherwise
     */
    public boolean isBatchMode() {
        return batchMode;
    }

    /**
     * Sets batch mode on/off (true/false)
     *
     * @param batchMode batch mode setting
     */
    public void setBatchMode(boolean batchMode) {
        this.batchMode = batchMode;
    }

    /**
     * Sets allowed network error, which indicates when to stopLearning training
     *
     * @param maxError network error
     */
    public void setMaxError(double maxError) {
        this.maxError = maxError;
    }

    /**
     * Returns learning error tolerance - the value of total network error to stop learning.
     *
     * @return learning error tolerance
     */
    public double getMaxError() {
        return maxError;
    }

    /**
     * Returns total network error in previous learning epoch
     *
     * @return total network error in previous learning epoch
     */
    public double getPreviousEpochError() {
        return previousEpochError;
    }

    /**
     * Returns min error change stopping criteria
     *
     * @return min error change stopping criteria
     */
    public double getMinErrorChange() {
        return minErrorChange;
    }

    /**
     * Sets min error change stopping criteria
     *
     * @param minErrorChange value for min error change stopping criteria
     */
    public void setMinErrorChange(double minErrorChange) {
        this.minErrorChange = minErrorChange;
    }

    /**
     * Returns number of iterations for min error change stopping criteria
     *
     * @return number of iterations for min error change stopping criteria
     */
    public int getMinErrorChangeIterationsLimit() {
        return minErrorChangeIterationsLimit;
    }

    /**
     * Sets number of iterations for min error change stopping criteria
     *
     * @param minErrorChangeIterationsLimit number of iterations for min error change stopping criteria
     */
    public void setMinErrorChangeIterationsLimit(int minErrorChangeIterationsLimit) {
        this.minErrorChangeIterationsLimit = minErrorChangeIterationsLimit;
    }

    /**
     * Returns number of iterations count for for min error change stopping criteria
     *
     * @return number of iterations count for for min error change stopping criteria
     */
    public int getMinErrorChangeIterationsCount() {
        return minErrorChangeIterationsCount;
    }

    public ErrorFunction getErrorFunction() {
        return errorFunction;
    }

    public void setErrorFunction(ErrorFunction errorFunction) {
        this.errorFunction = errorFunction;
    }


    public double getTotalNetworkError() {
        return errorFunction.getTotalError();
    }

}
