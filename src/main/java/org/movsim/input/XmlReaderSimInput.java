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
package org.movsim.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.movsim.input.model.SimulationInput;
import org.movsim.input.model.VehicleInput;
import org.movsim.input.model.consumption.FuelConsumptionInput;
import org.movsim.input.model.impl.VehicleInputImpl;
import org.movsim.utilities.impl.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The Class XmlReaderSimInput.
 * 
 * @author Arne Kesting, Ralph Germ
 */
public class XmlReaderSimInput {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(XmlReaderSimInput.class);

    private boolean isValid;

    private final InputDataImpl inputData;

    private final String xmlFilename;

    private final String filenameEnding = ".xml";

    private Document doc;

    // dtd from resources. do *not* use the File.separator character
    private String dtdFilename = "/sim/multiModelTrafficSimulatorInput.dtd";

    private InputStream appletinputstream;

    private InputSource appletresource;

    private ProjectMetaData projectMetaData;

    /**
     * Instantiates a new xml reader to parse and validate the simulation input.
     * 
     * @param inputData
     *            the input data
     */
    public XmlReaderSimInput(final InputDataImpl inputData) {
        projectMetaData = inputData.getProjectMetaData(); 
        this.inputData = inputData;

        this.xmlFilename = projectMetaData.getProjectName(); // TODO Path + File
        
        // TODO Remove AccessController: Is not needed anymore
        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {

                if (!projectMetaData.isXmlFromResources() && !FileUtils.fileExists(xmlFilename)) {
                    logger.error("XML File does not exist. Exit Simulation.");
                    System.exit(1);
                }

                logger.info("Begin parsing: " + xmlFilename);

                if (projectMetaData.isXmlFromResources()) {
                    readAndValidateXmlFromResources();
                } else {
                    readAndValidateXmlFromFileName();
                }

                // write internal xml file:
                if (projectMetaData.isWriteInternalXml()) {
                    final String outFilename = xmlFilename + ".internal_xml";
                    writeInternalXmlToFile(doc, outFilename);
                    logger.info("internal xml output written to file {}. Exit.", outFilename);
                    System.exit(0);
                }

                fromDomToInternalDatastructure();
                logger.info("End XmlReaderSimInput.");

                return null;
            }
        });

    }

    /**
     * Writes the internal xml after validation to file.
     *
     * @param localDoc the local doc
     * @param outFilename the output file name
     */
    private void writeInternalXmlToFile(final Document localDoc, String outFilename) {
        PrintWriter writer = FileUtils.getWriter(outFilename);
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat());
        try {
            logger.info("  write internal xml after validation to file \"" + outFilename + "\"");
            outputter.output(localDoc, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * From dom to internal data structure.
     */
    @SuppressWarnings("unchecked")
    private void fromDomToInternalDatastructure() {

        inputData.setProjectName(xmlFilename.substring(0, xmlFilename.indexOf(filenameEnding)));

        final Element root = doc.getRootElement();

        final SimulationInput simInput = new SimulationInput(root.getChild(XmlElementNames.Simulation));
        
        inputData.setSimulationInput(simInput);
        
        // -------------------------------------------------------

        final List<VehicleInput> vehicleInputData = new ArrayList<VehicleInput>();

        final List<Element> vehicleElements = root.getChild(XmlElementNames.DriverVehicleUnits).getChildren();
        
        for (final Element vehElem : vehicleElements) {
            vehicleInputData.add(new VehicleInputImpl(vehElem));
        }
        inputData.setVehicleInputData(vehicleInputData);

        // -------------------------------------------------------

        System.out.println("parse fuelConsumptionInput");
        final FuelConsumptionInput fuelConsumptionInput = new FuelConsumptionInput(root.getChild(XmlElementNames.Consumption));
        
        inputData.setFuelConsumptionInput(fuelConsumptionInput);
        
       

    }

    /**
     * Read and validate xml.
     */
    private void readAndValidateXmlFromFileName() {
        validate(FileUtils.getInputSourceFromFilename(xmlFilename)); // TODO
                                                                     // path
        doc = getDocument(FileUtils.getInputSourceFromFilename(xmlFilename));
    }

    /**
     * Validates and reads xml from resources.
     */
    private void readAndValidateXmlFromResources() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {

                appletinputstream = XmlReaderSimInput.class.getResourceAsStream(xmlFilename);
                appletresource = new InputSource(appletinputstream);
                validate(appletresource);
                appletinputstream = XmlReaderSimInput.class.getResourceAsStream(xmlFilename);
                appletresource = new InputSource(appletinputstream);
                doc = getDocument(appletresource);

                return null;
            }
        });

    }

    /**
     * Gets the Document.
     * 
     * @param inputSource
     *            the input source
     * @return the document
     */
    private Document getDocument(final InputSource inputSource) {

        final Document doc = (Document) AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {

                try {
                    final SAXBuilder builder = new SAXBuilder();
                    builder.setIgnoringElementContentWhitespace(true);
                    builder.setEntityResolver(new EntityResolver() {

                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
                                IOException {
                            InputStream is = XmlReaderSimInput.class.getResourceAsStream(dtdFilename);
                            InputSource input = new InputSource(is);
                            return input;
                        }
                    });
                    final Document document = builder.build(inputSource);
                    return document;
                } catch (final JDOMException e) {
                    e.printStackTrace();
                    System.exit(-1);
                    return null;
                } catch (final Exception e) {
                    e.printStackTrace();
                    return null;
                }

            }
        });

        return doc;
    }

    /**
     * Validates the Inputsource.
     * 
     * @param inputSource
     *            the input source
     */
    private void validate(final InputSource inputSource) {
        // global flag !!! also used in errorHandler
        isValid = true;

        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {

                try {
                    logger.debug("validate input ... ");
                    final XMLReader myXMLReader = XMLReaderFactory.createXMLReader();
                    myXMLReader.setFeature("http://xml.org/sax/features/validation", true);
                    final DefaultHandler handler = new MyErrorHandler();
                    myXMLReader.setErrorHandler(handler);

                    // overriding dtd source from xml file with internal dtd
                    // from jar

                    myXMLReader.setEntityResolver(new EntityResolver() {

                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
                                IOException {
                            InputStream is = XmlReaderSimInput.class.getResourceAsStream(dtdFilename);
                            InputSource input = new InputSource(is);
                            return input;
                        }
                    });

                    myXMLReader.parse(inputSource);
                } catch (final SAXException e) {
                    isValid = false;
                    System.out.println(e.getMessage());
                }
                    catch (final Exception e) {
                    isValid = false;
                    System.out.println(e.getMessage());
                }

                if (!isValid) {
                    logger.error("xml input file {} is not well-formed or invalid ...Exit Simulation.", xmlFilename);
                    System.exit(0);
                } else if (projectMetaData.isOnlyValidation()) {
                    logger.info("xml input file is well-formed and valid. Exit Simulation as requested.");
                    System.exit(0);
                }

                return null;
            }
        });
    }

    /**
     * The Inner Class MyErrorHandler.
     * 
     * uses global isValid flag
     */
    class MyErrorHandler extends DefaultHandler {

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException
         * )
         */
        @Override
        public void warning(SAXParseException e) throws SAXException {
            logger.warn(getInfo(e));
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException
         * )
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            logger.error(getInfo(e));
            isValid = false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.
         * SAXParseException)
         */
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            logger.error(getInfo(e));
            isValid = false;
        }

        /**
         * Gets the info to the corresponding exception.
         * 
         * @param e
         *            the exception
         * @return the info
         */
        private String getInfo(SAXParseException e) {
            final StringBuilder stringb = new StringBuilder();
            stringb.append("   Public ID: " + e.getPublicId());
            stringb.append("   System ID: " + e.getSystemId());
            stringb.append("   Line number: " + e.getLineNumber());
            stringb.append("   Column number: " + e.getColumnNumber());
            stringb.append("   Message: " + e.getMessage());
            return stringb.toString();
        }
    }

}