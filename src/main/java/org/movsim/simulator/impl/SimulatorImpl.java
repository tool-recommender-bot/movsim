/**
 * Copyright (C) 2010, 2011 by Arne Kesting, Martin Treiber,
 *                             Ralph Germ, Martin Budden
 *                             <info@movsim.org>
 * ----------------------------------------------------------------------
 * 
 *  This file is part of 
 *  
 *  MovSim - the multi-model open-source vehicular-traffic simulator 
 *
 *  MovSim is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MovSim is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MovSim.  If not, see <http://www.gnu.org/licenses/> or
 *  <http://www.movsim.org>.
 *  
 * ----------------------------------------------------------------------
 */
package org.movsim.simulator.impl;

import org.movsim.input.InputData;
import org.movsim.input.commandline.SimCommandLine;
import org.movsim.input.impl.InputDataImpl;
import org.movsim.input.impl.XmlReaderSimInput;
import org.movsim.input.model.SimulationInput;
import org.movsim.output.SimObservables;
import org.movsim.output.SimOutput;
import org.movsim.simulator.Constants;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadSection.RoadSection;
import org.movsim.simulator.roadSection.impl.RoadSectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class SimulatorImpl.
 */
public class SimulatorImpl implements Simulator, Runnable {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(SimulatorImpl.class);

    /** The time. */
    private double time;

    /** The itime. */
    private int itime;

    /** The timestep. */
    private double timestep;

    /** The duration of the simulation. */
    private double tMax;

    /** The road section. */
    private RoadSection roadSection;

    /** The sim output. */
    private SimOutput simOutput;

    /** The is with gui. */
    private boolean instantaneousFileOutput = true;

    /** The sim input. */
    private InputData inputData;

    private String xmlFileName;

    private SimCommandLine cmdline;

    /**
     * Instantiates a new simulator impl.
     * 
     * @param instantaneousFileOutput
     *            Gives instantaneous file output of some simulation results
     *            (floating cars data, fundamental diagram, saptiotemporal data,
     *            trajectories) to predefined files in the "sim" folder. Also
     *            pay attention to the logging attributes in the xml files!
     *            These are independent to this flag.
     * @param cmdline
     *            commandline
     */
    public SimulatorImpl(boolean instantaneousFileOutput, SimCommandLine cmdline) {
        this.instantaneousFileOutput = instantaneousFileOutput;
        if (cmdline != null) {
            this.cmdline = cmdline;
            xmlFileName = cmdline.getSimulationFilename();
        } else {
            xmlFileName = "sim/onramp_IDM.xml";
        }
        this.inputData = new InputDataImpl();
    }

    /**
     * Restart.
     */
    public void restart() {
        time = 0;
        itime = 0;
        roadSection = new RoadSectionImpl(instantaneousFileOutput, inputData);

        // model requires specific update time depending on its category !!

        // TODO: check functionality
        if (roadSection.getTimestep() > Constants.SMALL_VALUE) {
            this.timestep = roadSection.getTimestep();
            logger.info("model sets simulation integration timestep to dt={}", timestep);
        }

        simOutput = new SimOutput(instantaneousFileOutput, inputData, roadSection);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#run()
     */
    @Override
    public void run() {
        logger.info("Simulator.run: start simulation at {} seconds", time);

        simOutput.update(itime, time, timestep);

        while (!stopThisRun(time)) {
            time += timestep;
            itime++;
            update();
        }

        logger.info("Simulator.run: stop after time = {} seconds", time);
    }

    /**
     * Stop this run.
     * 
     * @param time
     *            the time
     * @return true, if successful
     */
    private boolean stopThisRun(double time) {
        return (time > tMax);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#update()
     */
    @Override
    public void update() {
        if (itime % 100 == 0) {
            logger.info("Simulator.update: time={} seconds, dt={}", time, timestep);
        }
        roadSection.update(itime, time);
        simOutput.update(itime, time, timestep);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#iTime()
     */
    @Override
    public int iTime() {
        return itime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#time()
     */
    @Override
    public double time() {
        return time;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#timestep()
     */
    @Override
    public double timestep() {
        return timestep;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#getSiminput()
     */
    @Override
    public InputData getSimInput() {
        return inputData;
    }

    @Override
    public SimObservables getSimObservables() {
        return simOutput;
    }

    public String getXmlFileName() {
        return xmlFileName;
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
        inputData.setProjectName(xmlFileName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.Simulator#initialize()
     */
    @Override
    public void initialize() {

        logger.info("Copyright '\u00A9' by Arne Kesting, Martin Treiber, Ralph Germ and  Martin Budden (2010, 2011)");

        // parse xmlFile and set values // TODO why InputData impl?

        final XmlReaderSimInput xmlReader = new XmlReaderSimInput(xmlFileName, cmdline, (InputDataImpl) inputData);
        final SimulationInput simInput = inputData.getSimulationInput();
        this.timestep = simInput.getTimestep(); // can be modified by certain
                                                // models
        this.tMax = simInput.getMaxSimTime();

        MyRandom.initialize(simInput.isWithFixedSeed(), simInput.getRandomSeed());

        restart();
    }

}
