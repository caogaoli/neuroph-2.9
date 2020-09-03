package org.neuroph.core.input;

import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.neuroph.core.Connection;
import org.neuroph.core.Neuron;
import org.neuroph.nnet.comp.neuron.InputNeuron;

/**
 * @author Shivanth, Tijana
 */
@RunWith(value = Parameterized.class)
public class SumSqrTest {

    SumSqr instance;
    List<Connection> inputConnections;
    List<InputNeuron> inputNeurons;
    double[] inputs;
    double expected;

    public SumSqrTest(DoubleArray inputs, double expected) {
        this.inputs = inputs.getArray();
        this.expected = expected;
    }

    @Parameters
    public static Collection<Object[]> getParamters() {
        return Arrays.asList(new Object[][]{{new DoubleArray(new double[]{.1, .4, .7, .9}), 1.47},
                {new DoubleArray(new double[]{.1, -.4, .7, -.9}), 1.47}});

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new SumSqr();
        inputNeurons = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            inputNeurons.add(new InputNeuron());
        }

        Neuron toNeuron = new Neuron();

        inputConnections = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            inputConnections.add(new Connection(inputNeurons.get(i), toNeuron, 1));
            toNeuron.addInputConnection(inputConnections.get(i));
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetOutput() {
        for (int i = 0; i < inputNeurons.size(); i++) {
            inputNeurons.get(i).setInput(inputs[i]);
            inputNeurons.get(i).calculate();
        }
        double result = instance.getOutput(inputConnections);
        assertEquals(expected, result, .000001);
    }

}
