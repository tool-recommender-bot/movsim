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
package org.movsim.output.impl;

import org.movsim.output.LoopDetector;
import org.movsim.simulator.Constants;
import org.movsim.simulator.vehicles.Moveable;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.VehicleContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// TODO: Auto-generated Javadoc
/**
 * The Class LoopDetectorImpl.
 */
public class LoopDetectorImpl extends ObservableImpl implements LoopDetector{

    final static Logger logger = LoggerFactory.getLogger(LoopDetectorImpl.class);

    /** The dt sample. */
    private final double dtSample;

    /** The det position. */
    private final double detPosition;


    /** The time offset. */
    private double timeOffset;

    // internal state variables
    /** The veh count. */
    private int vehCount;
    
    /** The v sum. */
    private double vSum;
    
    /** The occ time. */
    private double occTime;
    
    /** The sum inv v. */
    private double sumInvV;
    
    /** The sum inv q. */
    private double sumInvQ;

    /** The mean speed. */
    private double meanSpeed;
    
    private double densityArithmetic;
    
    /** The flow. */
    private double flow;
    
    /** The occupancy. */
    private double occupancy;
    
    /** The veh count output. */
    private int vehCountOutput;
    
    /** The mean harmonic speed. */
    private double meanSpeedHarmonic;
    
    /** The harmonic mean timegap. */
    private double meanTimegapHarmonic;

    
    /**
     * Instantiates a new loop detector impl.
     * 
     * @param projectName
     *            the project name
     * @param writeOutput
     *            the write output
     * @param detPosition
     *            the det position
     * @param dtSample
     *            the dt sample
     */
    public LoopDetectorImpl(String projectName, boolean writeOutput, double detPosition, double dtSample) {
        this.detPosition = detPosition;
        this.dtSample = dtSample;

        timeOffset = 0;
        reset();

        notifyObservers(0);
    }

    /**
     * Reset.
     */
    private void reset() {
        vehCount = 0;
        vSum = 0;
        occTime = 0;
        sumInvQ = 0;
        sumInvV = 0;
    }

   
    public void update(double time, VehicleContainer vehicleContainer) {

        // brute force search:

        for (final Vehicle veh : vehicleContainer.getVehicles()) {
            if ((veh.oldPosition() < detPosition) && (veh.position() >= detPosition)) {
                // new vehicle crossed detector
                vehCount++;
                final double speedVeh = veh.speed();
                vSum += speedVeh;
                occTime += veh.length() / speedVeh;
                sumInvV += (speedVeh > 0) ? 1. / speedVeh : 0;
                // calculate brut timegap not from local detector data:
                final Moveable vehFront = vehicleContainer.getLeader(veh);
                final double brutTimegap = (vehFront == null) ? 0 : (vehFront.position() - veh.position())
                        / vehFront.speed();
                sumInvQ += (brutTimegap > 0) ? 1. / brutTimegap : 0; // microscopic
                                                                     // flow
            }
        }

        if ((time - timeOffset + Constants.SMALL_VALUE) >= dtSample) {
            calculateAverages();
            notifyObservers(time);
            timeOffset = time;
        }
    }

    // ############################################

    /**
     * Calculate averages.
     */
    private void calculateAverages() {
        flow = vehCount / dtSample;
        meanSpeed = (vehCount == 0) ? 0 : vSum / vehCount;
        densityArithmetic = (vehCount == 0) ? 0 : flow / meanSpeed;
        occupancy = occTime / dtSample;
        vehCountOutput = vehCount;
        meanSpeedHarmonic = (vehCount == 0) ? 0 : 1. / (sumInvV / vehCount);
        meanTimegapHarmonic = (vehCount == 0) ? 0 : sumInvQ / vehCount;
        reset();
    }

    /**
     * @return the dtSample
     */
    public double getDtSample() {
        return dtSample;
    }

    /**
     * @return the detPosition
     */
    public double getDetPosition() {
        return detPosition;
    }

    /**
     * @return the meanSpeed
     */
    public double getMeanSpeed() {
        return meanSpeed;
    }

    /**
     * @return the densityArithmetic
     */
    public double getDensityArithmetic() {
        return densityArithmetic;
    }

    /**
     * @return the flow
     */
    public double getFlow() {
        return flow;
    }

    /**
     * @return the occupancy
     */
    public double getOccupancy() {
        return occupancy;
    }

    /**
     * @return the vehCountOutput
     */
    public int getVehCountOutput() {
        return vehCountOutput;
    }

    /**
     * @return the meanSpeedHarmonic
     */
    public double getMeanSpeedHarmonic() {
        return meanSpeedHarmonic;
    }

    /**
     * @return the meanTimegapHarmonic
     */
    public double getMeanTimegapHarmonic() {
        return meanTimegapHarmonic;
    }

       
    
}