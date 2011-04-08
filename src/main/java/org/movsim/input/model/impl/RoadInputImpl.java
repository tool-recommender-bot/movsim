package org.movsim.input.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.movsim.input.impl.XmlUtils;
import org.movsim.input.model.RoadInput;
import org.movsim.input.model.simulation.FlowConservingBottleneckDataPoint;
import org.movsim.input.model.simulation.HeterogeneityInputData;
import org.movsim.input.model.simulation.ICMacroData;
import org.movsim.input.model.simulation.ICMicroData;
import org.movsim.input.model.simulation.RampData;
import org.movsim.input.model.simulation.SimpleRampData;
import org.movsim.input.model.simulation.SpeedLimitDataPoint;
import org.movsim.input.model.simulation.TrafficLightData;
import org.movsim.input.model.simulation.UpstreamBoundaryData;
import org.movsim.input.model.simulation.impl.FlowConservingBottleneckDataPointImpl;
import org.movsim.input.model.simulation.impl.HeterogeneityInputDataImpl;
import org.movsim.input.model.simulation.impl.ICMacroDataImpl;
import org.movsim.input.model.simulation.impl.ICMicroDataImpl;
import org.movsim.input.model.simulation.impl.RampDataImpl;
import org.movsim.input.model.simulation.impl.SimpleRampDataImpl;
import org.movsim.input.model.simulation.impl.SpeedLimitDataPointImpl;
import org.movsim.input.model.simulation.impl.TrafficLightDataImpl;
import org.movsim.input.model.simulation.impl.UpstreamBoundaryDataImpl;

public class RoadInputImpl implements RoadInput {
    
    private int id;
    
    private double roadLength;
    private int lanes;
    
    private boolean isWithWriteFundamentalDiagrams;
    
    private List<HeterogeneityInputData> heterogeneityInputData;
    
    private List<ICMacroData> icMacroData;
    private List<ICMicroData> icMicroData;
    
    private UpstreamBoundaryData upstreamBoundaryData;
    
    private List<FlowConservingBottleneckDataPoint> flowConsBottleneckInputData;
    private List<SpeedLimitDataPoint> speedLimitInputData;
    
    private List<SimpleRampData> simpleRamps;
    private List<RampData> ramps;
    
    
    private List<TrafficLightData> trafficLightData;
    
    
    public RoadInputImpl(Element elem){
        parseRoadElement(elem);
    }
    
    @SuppressWarnings("unchecked")
    private void parseRoadElement(Element elem) {
        
        id = Integer.parseInt(elem.getAttributeValue("id"));
        roadLength  = Double.parseDouble(elem.getAttributeValue("x_max"));
        lanes = Integer.parseInt(elem.getAttributeValue("lanes"));
        
        
        
        // -----------------------------------------------------------
        
        // heterogeneity element with vehicle types
        
        final Element heterogenElem = elem.getChild("TRAFFIC_COMPOSITION");
        isWithWriteFundamentalDiagrams = heterogenElem.getAttributeValue("write_fund_diagrams").equals("true") ? true : false;
        final List<Element> vehTypeElems = elem.getChild("TRAFFIC_COMPOSITION").getChildren("VEHICLE_TYPE");
        heterogeneityInputData = new ArrayList<HeterogeneityInputData>();
        for (Element vehTypeElem: vehTypeElems) {
            final Map<String, String> map = XmlUtils.putAttributesInHash(vehTypeElem);
            heterogeneityInputData.add(new HeterogeneityInputDataImpl(map));
        }
        

        // -----------------------------------------------------------
        
        // Initial Conditions Micro
        final List<Element> icMicroElems = elem.getChild("INITIAL_CONDITIONS").getChildren("IC_MICRO");
        icMicroData = new ArrayList<ICMicroData>();
        for (Element icMicroElem : icMicroElems) {
            final Map<String, String> map = XmlUtils.putAttributesInHash(icMicroElem);
            icMicroData.add(new ICMicroDataImpl(map));
        }

        Collections.sort(icMicroData, new Comparator<ICMicroData>() {
            public int compare(ICMicroData o1, ICMicroData o2) {
                Double pos1 = new Double((o1).getX());
                Double pos2 = new Double((o2).getX());
                return pos2.compareTo(pos1); // sort with DECREASING x because of FC veh counting 
            }
        });
        
        // -----------------------------------------------------------
        
        // Initial Conditions Macro
        final List<Element> icMacroElems  = elem.getChild("INITIAL_CONDITIONS").getChildren("IC_MACRO");
        icMacroData = new ArrayList<ICMacroData>();
        for (Element icMacroElem : icMacroElems) {
            final Map<String, String> map = XmlUtils.putAttributesInHash(icMacroElem);
            icMacroData.add(new ICMacroDataImpl(map));
        }

        Collections.sort(icMacroData, new Comparator<ICMacroData>() {
            public int compare(ICMacroData o1, ICMacroData o2) {
                Double pos1 = new Double((o1).getX());
                Double pos2 = new Double((o2).getX());
                return pos1.compareTo(pos2); // sort with increasing x 
            }
        });
        
        
        // -----------------------------------------------------------
        
        // TRAFFIC_SOURCE
        final Element upInflowElem = elem.getChild("TRAFFIC_SOURCE");
        upstreamBoundaryData = new UpstreamBoundaryDataImpl(upInflowElem);
        
        
        // -----------------------------------------------------------
        
        // TRAFFIC_SINK
        final Element downInflowElem = elem.getChild("TRAFFIC_SINK");
        // nothing to do (not yet implementend)
        
        
        // -----------------------------------------------------------
        
        // FlowConservingBottlenecks
        flowConsBottleneckInputData = new ArrayList<FlowConservingBottleneckDataPoint>();
        final List<Element> flowConsElems = elem.getChild("FLOW_CONSERVING_INHOMOGENEITIES").getChildren("INHOMOGENEITY");
        for (Element flowConsElem : flowConsElems) {
            final Map<String, String> map = XmlUtils.putAttributesInHash(flowConsElem);
            flowConsBottleneckInputData.add(new FlowConservingBottleneckDataPointImpl(map));
        }
        
        Collections.sort(flowConsBottleneckInputData, new Comparator<FlowConservingBottleneckDataPoint>() {
            public int compare(FlowConservingBottleneckDataPoint o1, FlowConservingBottleneckDataPoint o2) {
                Double pos1 = new Double((o1).getPosition());
                Double pos2 = new Double((o2).getPosition());
                return pos1.compareTo(pos2); // sort with increasing x 
            }
        });

        // -----------------------------------------------------------
        
        // speed limits 
        speedLimitInputData = new ArrayList<SpeedLimitDataPoint>();
        final List<Element> speedLimitElems = elem.getChild("SPEED_LIMITS").getChildren("LIMIT");
        for (Element speedLimitElem : speedLimitElems) {
            final Map<String, String> map = XmlUtils.putAttributesInHash(speedLimitElem);
            speedLimitInputData.add(new SpeedLimitDataPointImpl(map));
        }
        
        Collections.sort(speedLimitInputData, new Comparator<SpeedLimitDataPoint>() {
            public int compare(SpeedLimitDataPoint o1, SpeedLimitDataPoint o2) {
                Double pos1 = new Double((o1).getPosition());
                Double pos2 = new Double((o2).getPosition());
                return pos1.compareTo(pos2); // sort with increasing x 
            }
        });

        // -----------------------------------------------------------

        // non-physical ramps implementing a drop-down mechanism without lane-changing decisions   
        simpleRamps = new ArrayList<SimpleRampData>();
        final List<Element> simpleRampElems = elem.getChild("RAMPS").getChildren("SIMPLE_RAMP");
        for (Element simpleRampElem : simpleRampElems) {
            simpleRamps.add(new SimpleRampDataImpl(simpleRampElem));
        }
        
        Collections.sort(simpleRamps, new Comparator<SimpleRampData>() {
            public int compare(SimpleRampData o1, SimpleRampData o2) {
                Double pos1 = new Double((o1).getCenterPosition());
                Double pos2 = new Double((o2).getCenterPosition());
                return pos1.compareTo(pos2); // sort with increasing x 
            }
        });

     // -----------------------------------------------------------
        // physical ramps  
        ramps = new ArrayList<RampData>();
        final List<Element> rampElems = elem.getChild("RAMPS").getChildren("RAMP");
        for (Element rampElem : rampElems) {
            ramps.add(new RampDataImpl(rampElem));
        }
        
        Collections.sort(ramps, new Comparator<RampData>() {
            public int compare(RampData o1, RampData o2) {
                Double pos1 = new Double((o1).getCenterPosition());
                Double pos2 = new Double((o2).getCenterPosition());
                return pos1.compareTo(pos2); // sort with increasing x 
            }
        });
        
        // -----------------------------------------------------------
        
        // Trafficlights
        trafficLightData = new ArrayList<TrafficLightData>();
        final List<Element> trafficLigthElems = elem.getChild("TRAFFICLIGHTS").getChildren("TRAFFICLIGHT");
        for (Element trafficLightElem : trafficLigthElems) {
            final Map<String, String> map = XmlUtils.putAttributesInHash(trafficLightElem);
            trafficLightData.add(new TrafficLightDataImpl(map));
        }
        
        Collections.sort(trafficLightData, new Comparator<TrafficLightData>() {
            public int compare(TrafficLightData o1, TrafficLightData o2) {
                Double pos1 = new Double((o1).getX());
                Double pos2 = new Double((o2).getX());
                return pos1.compareTo(pos2); // sort with increasing x 
            }
        });
        
        // -----------------------------------------------------------
        
    }
    
   
    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getRoadLength()
     */
    public double getRoadLength() {
        return roadLength;
    }


  

    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getHeterogeneityInputData()
     */
    public List<HeterogeneityInputData> getHeterogeneityInputData() {
        return heterogeneityInputData;
    }


    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getIcMacroData()
     */
    public List<ICMacroData> getIcMacroData() {
        return icMacroData;
    }


    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getIcMicroData()
     */
    public List<ICMicroData> getIcMicroData() {
        return icMicroData;
    }


    public UpstreamBoundaryData getUpstreamBoundaryData(){
        return upstreamBoundaryData;
    }


    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getFlowConsBottleneckInputData()
     */
    public List<FlowConservingBottleneckDataPoint> getFlowConsBottleneckInputData() {
        return flowConsBottleneckInputData;
    }

    public List<SpeedLimitDataPoint> getSpeedLimitInputData(){
        return speedLimitInputData;
    }

    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getRamps()
     */
    public List<RampData> getRamps() {
        return ramps;
    }


    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getTrafficLightData()
     */
    public List<TrafficLightData> getTrafficLightData() {
        return trafficLightData;
    }
    
    /* (non-Javadoc)
     * @see org.movsim.input.model.impl.SimulationInput#getLanes()
     */
    public int getLanes(){
        return lanes;
    }
    
    public int getId() {
        return id;
    }


    public boolean isWithWriteFundamentalDiagrams(){
        return isWithWriteFundamentalDiagrams;
    }

    public List<SimpleRampData> getSimpleRamps() {
        return simpleRamps;
    }

    
}