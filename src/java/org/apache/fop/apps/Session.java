/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.apps;

// FOP
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FOTreeBuilder;
import org.apache.fop.fo.FOUserAgent;
import org.apache.fop.render.Renderer;
import org.apache.fop.tools.DocumentInputSource;
import org.apache.fop.tools.DocumentReader;


// Avalon
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;

// DOM
import org.w3c.dom.Document;

// SAX
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

// Java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Primary class that drives overall FOP process.
 * <P>
 * The simplest way to use this is to instantiate it with the
 * InputSource and OutputStream, then set the renderer desired, and
 * calling run();
 * <P>
 * Here is an example use of Session which outputs PDF:
 *
 * <PRE>
 * Session session = new Session(new InputSource (args[0]),
 * new FileOutputStream(args[1]));
 * session.enableLogging(myLogger); //optional
 * session.setRenderer(RENDER_PDF);
 * session.run();
 * </PRE>
 * If neccessary, calling classes can call into the lower level
 * methods to setup and
 * render. Methods can be called to set the
 * Renderer to use, the (possibly multiple) ElementMapping(s) to
 * use and the OutputStream to use to output the results of the
 * rendering (where applicable). In the case of the Renderer and
 * ElementMapping(s), the Session may be supplied either with the
 * object itself, or the name of the class, in which case Session will
 * instantiate the class itself. The advantage of the latter is it
 * enables runtime determination of Renderer and ElementMapping(s).
 * <P>
 * Once the Session is set up, the render method
 * is called. Depending on whether DOM or SAX is being used, the
 * invocation of the method is either render(Document) or
 * buildFOTree(Parser, InputSource) respectively.
 * <P>
 * A third possibility may be used to build the FO Tree, namely
 * calling getContentHandler() and firing the SAX events yourself.
 * <P>
 * Once the FO Tree is built, the format() and render() methods may be
 * called in that order.
 * <P>
 * Here is an example use of Session which outputs to AWT:
 *
 * <PRE>
 * Session session = new Session();
 * session.enableLogging(myLogger); //optional
 * session.setRenderer(new org.apache.fop.render.awt.AWTRenderer(translator));
 * session.render(parser, fileInputSource(args[0]));
 * </PRE>
 */
public class Session implements LogEnabled {

    /**
     * Render to PDF. OutputStream must be set
     */
    public static final int RENDER_PDF = 1;

    /**
     * Render to a GUI window. No OutputStream neccessary
     */
    public static final int RENDER_AWT = 2;

    /**
     * Render to MIF. OutputStream must be set
     */
    public static final int RENDER_MIF = 3;

    /**
     * Render to XML. OutputStream must be set
     */
    public static final int RENDER_XML = 4;

    /**
     * Render to PRINT. No OutputStream neccessary
     */
    public static final int RENDER_PRINT = 5;

    /**
     * Render to PCL. OutputStream must be set
     */
    public static final int RENDER_PCL = 6;

    /**
     * Render to Postscript. OutputStream must be set
     */
    public static final int RENDER_PS = 7;

    /**
     * Render to Text. OutputStream must be set
     */
    public static final int RENDER_TXT = 8;

    /**
     * Render to SVG. OutputStream must be set
     */
    public static final int RENDER_SVG = 9;

    /**
     * Render to RTF. OutputStream must be set
     */
    public static final int RENDER_RTF = 10;

    /**
     * the FO tree builder
     */
    private FOTreeBuilder treeBuilder;

    /**
     * the renderer type code given by setRenderer
     */
    private int rendererType;

    /**
     * the renderer to use to output the area tree
     */
    private Renderer renderer;

    /**
     * the structure handler
     */
    private StructureHandler structHandler;

    /**
     * the source of the FO file
     */
    private InputSource source;

    /**
     * the stream to use to output the results of the renderer
     */
    private OutputStream stream;

    /**
     * The XML parser to use when building the FO tree
     */
    private XMLReader reader;

    /**
     * the system resources that FOP will use
     */
    private Logger log = null;
    private FOUserAgent userAgent = null;

    /**
     * Returns the fully qualified classname of the standard XML parser for FOP
     * to use.
     * @return the XML parser classname
     */
    public static final String getParserClassName() {
        try {
            return javax.xml.parsers.SAXParserFactory.newInstance()
                .newSAXParser().getXMLReader().getClass().getName();
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            return null;
        } catch (org.xml.sax.SAXException e) {
            return null;
        }
    }

    /**
     * Main constructor for the Session class.
     */
    public Session() {
        stream = null;
    }

    /**
     * Convenience constructor for directly setting input and output.
     * @param source InputSource to take the XSL-FO input from
     * @param stream Target output stream
     */
    public Session(InputSource source, OutputStream stream) {
        this();
        this.source = source;
        this.stream = stream;
    }

    private boolean isInitialized() {
        return (treeBuilder != null);
    }

    /**
     * Initializes the Session object.
     */
    public void initialize() {
        if (isInitialized()) {
            throw new IllegalStateException("Session already initialized");
        }
        treeBuilder = new FOTreeBuilder();
        treeBuilder.setUserAgent(getUserAgent());
        setupDefaultMappings();
    }

    /**
     * Optionally sets the FOUserAgent instance for FOP to use. The Session
     * class sets up its own FOUserAgent if none is set through this method.
     * @param agent FOUserAgent to use
     */
    public void setUserAgent(FOUserAgent agent) {
        userAgent = agent;
    }

    private FOUserAgent getUserAgent() {
        if (userAgent == null) {
            userAgent = new FOUserAgent();
            userAgent.enableLogging(getLogger());
            userAgent.setBaseURL("");
        }
        return userAgent;
    }

    /**
     * Provide the Session instance with a logger. More information on Avalon
     * logging can be found at the
     * <a href="http://avalon.apache.org">Avalon site</a>.
     *
     * @param log the logger. Must not be <code>null</code>.
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(Logger)
     */
    public void enableLogging(Logger log) {
        if (this.log == null) {
            this.log = log;
        } else {
            getLogger().warn("Logger is already set! Won't use the new logger.");
        }
    }

    /**
     * Provide the Session instance with a logger.
     * @param log the logger. Must not be <code>null</code>.
     * @deprecated Use #enableLogging(Logger) instead.
     */
    public void setLogger(Logger log) {
        enableLogging(log);
    }


    /**
     * Returns the logger for use by FOP.
     * @return the logger
     * @see #enableLogging(Logger)
     */
    protected Logger getLogger() {
        if (this.log == null) {
            this.log = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);
            this.log.error("Logger not set. Using ConsoleLogger as default.");
        }

        return this.log;
    }

    /**
     * Resets the Session so it can be reused. Property and element
     * mappings are reset to defaults.
     * The output stream is cleared. The renderer is cleared.
     */
    public synchronized void reset() {
        source = null;
        stream = null;
        reader = null;
        treeBuilder.reset();
    }

    /**
     * Indicates whether FOP has already received input data.
     * @return true, if input data was received
     */
    public boolean hasData() {
        return (treeBuilder.hasData());
    }

    /**
     * Set the OutputStream to use to output the result of the Renderer
     * (if applicable)
     * @param stream the stream to output the result of rendering to
     */
    public void setOutputStream(OutputStream stream) {
        this.stream = stream;
    }

    private void validateOutputStream() {
        if (this.stream == null) {
            throw new IllegalStateException("OutputStream has not been set");
        }
    }

    /**
     * Set the source for the FO document. This can be a normal SAX
     * InputSource, or an DocumentInputSource containing a DOM document.
     * @see DocumentInputSource
     */
    public void setInputSource(InputSource source) {
        this.source = source;
    }

    /**
     * Sets the reader used when reading in the source. If not set,
     * this defaults to a basic SAX parser.
     * @param reader the reader to use.
     */
    public void setXMLReader(XMLReader reader) {
        this.reader = reader;
    }

    /**
     * Sets all the element and property list mappings to their default values.
     *
     */
    public void setupDefaultMappings() {
        addElementMapping("org.apache.fop.fo.FOElementMapping");
        addElementMapping("org.apache.fop.svg.SVGElementMapping");
        addElementMapping("org.apache.fop.extensions.ExtensionElementMapping");

        // add mappings from available services
        Iterator providers =
            providers(org.apache.fop.fo.ElementMapping.class);
        if (providers != null) {
            while (providers.hasNext()) {
                String str = (String)providers.next();
                try {
                    addElementMapping(str);
                } catch (IllegalArgumentException e) {
                    getLogger().warn("Error while adding element mapping", e);
                }

            }
        }
    }

    /**
     * Shortcut to set the rendering type to use. Must be one of
     * <ul>
     * <li>RENDER_PDF</li>
     * <li>RENDER_AWT</li>
     * <li>RENDER_MIF</li>
     * <li>RENDER_XML</li>
     * <li>RENDER_PCL</li>
     * <li>RENDER_PS</li>
     * <li>RENDER_TXT</li>
     * <li>RENDER_SVG</li>
     * <li>RENDER_RTF</li>
     * </ul>
     * @param renderer the type of renderer to use
     * @throws IllegalArgumentException if an unsupported renderer type was required.
     */
    public void setRenderer(int renderer) throws IllegalArgumentException {
        rendererType = renderer;
        switch (renderer) {
        case RENDER_PDF:
            setRenderer("org.apache.fop.render.pdf.PDFRenderer");
            break;
        case RENDER_AWT:
            throw new IllegalArgumentException("Use renderer form of setRenderer() for AWT");
        case RENDER_PRINT:
            throw new IllegalArgumentException("Use renderer form of setRenderer() for PRINT");
        case RENDER_PCL:
            setRenderer("org.apache.fop.render.pcl.PCLRenderer");
            break;
        case RENDER_PS:
            setRenderer("org.apache.fop.render.ps.PSRenderer");
            break;
        case RENDER_TXT:
            setRenderer("org.apache.fop.render.txt.TXTRenderer()");
            break;
        case RENDER_MIF:
            //structHandler will be set later
            break;
        case RENDER_XML:
            setRenderer("org.apache.fop.render.xml.XMLRenderer");
            break;
        case RENDER_SVG:
            setRenderer("org.apache.fop.render.svg.SVGRenderer");
            break;
        case RENDER_RTF:
            //structHandler will be set later
            break;
        default:
            throw new IllegalArgumentException("Unknown renderer type");
        }
    }

    /**
     * Set the Renderer to use.
     * @param renderer the renderer instance to use (Note: Logger must be set at this point)
     */
    public void setRenderer(Renderer renderer) {
        renderer.setUserAgent(getUserAgent());
        this.renderer = renderer;
    }

    /**
     * Returns the currently active renderer.
     * @return the renderer
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Sets the renderer.
     * @param rendererClassName the fully qualified classname of the renderer
     * class to use.
     * @param version version number
     * @deprecated use renderer.setProducer(version) + setRenderer(renderer) or
     * just setRenderer(rendererType) which will use the default producer string.
     * @see #setRenderer(int)
     * @see #setRenderer(Renderer)
     */
    public void setRenderer(String rendererClassName, String version) {
        setRenderer(rendererClassName);
    }

    /**
     * Set the class name of the Renderer to use as well as the
     * producer string for those renderers that can make use of it.
     * @param rendererClassName classname of the renderer to use such as
     * "org.apache.fop.render.pdf.PDFRenderer"
     * @exception IllegalArgumentException if the classname was invalid.
     * @see #setRenderer(int)
     */
    public void setRenderer(String rendererClassName)
                throws IllegalArgumentException {
        try {
            renderer =
                (Renderer)Class.forName(rendererClassName).newInstance();
            if (renderer instanceof LogEnabled) {
                ((LogEnabled)renderer).enableLogging(getLogger());
            }
            renderer.setProducer(Version.getVersion());
            renderer.setUserAgent(getUserAgent());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find "
                                               + rendererClassName);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                                               + rendererClassName);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access "
                                               + rendererClassName);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(rendererClassName
                                               + " is not a renderer");
        }
    }

    /**
     * Add the given element mapping.
     * An element mapping maps element names to Java classes.
     *
     * @param mapping the element mappingto add
     */
    public void addElementMapping(ElementMapping mapping) {
        mapping.addToBuilder(treeBuilder);
    }

    /**
     * Add the element mapping with the given class name.
     * @param mappingClassName the class name representing the element mapping.
     * @throws IllegalArgumentException if there was not such element mapping.
     */
    public void addElementMapping(String mappingClassName)
                throws IllegalArgumentException {
        try {
            ElementMapping mapping =
                (ElementMapping)Class.forName(mappingClassName).newInstance();
            addElementMapping(mapping);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find "
                                               + mappingClassName);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                                               + mappingClassName);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access "
                                               + mappingClassName);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(mappingClassName
                                               + " is not an ElementMapping");
        }
    }

    /**
     * Returns the tree builder (a SAX ContentHandler).
     *
     * Used in situations where SAX is used but not via a FOP-invoked
     * SAX parser. A good example is an XSLT engine that fires SAX
     * events but isn't a SAX Parser itself.
     * @return a content handler for handling the SAX events.
     */
    public ContentHandler getContentHandler() {
        if (!isInitialized()) {
            initialize();
        }
        validateOutputStream();

        // TODO: - do this stuff in a better way
        // PIJ: I guess the structure handler should be created by the renderer.
        if (rendererType == RENDER_MIF) {
            structHandler = new org.apache.fop.mif.MIFHandler(stream);
        } else if (rendererType == RENDER_RTF) {
            structHandler = new org.apache.fop.rtf.renderer.RTFHandler(stream);
        } else {
            if (renderer == null) {
                throw new IllegalStateException(
                        "Renderer not set when using standard structHandler");
            }
            structHandler = new LayoutHandler(stream, renderer, true);
        }

        structHandler.enableLogging(getLogger());

        treeBuilder.setUserAgent(getUserAgent());
        treeBuilder.setStructHandler(structHandler);

        return treeBuilder;
    }

    /**
     * Render the FO document read by a SAX Parser from an InputSource.
     * @param parser the SAX parser.
     * @param source the input source the parser reads from.
     * @throws FOPException if anything goes wrong.
     */
    public synchronized void render(XMLReader parser, InputSource source)
                throws FOPException {
        if (!isInitialized()) {
            initialize();
        }
        parser.setContentHandler(getContentHandler());
        try {
            parser.parse(source);
        } catch (SAXException e) {
            if (e.getException() instanceof FOPException) {
                // Undo exception tunneling.
                throw (FOPException)e.getException();
            } else {
                throw new FOPException(e);
            }
        } catch (IOException e) {
            throw new FOPException(e);
        }
    }

    /**
     * Render the FO ducument represented by a DOM Document.
     * @param document the DOM document to read from
     * @throws FOPException if anything goes wrong.
     */
    public synchronized void render(Document document)
                throws FOPException {
        if (!isInitialized()) {
            initialize();
        }
        try {
            DocumentInputSource source = new DocumentInputSource(document);
            DocumentReader reader = new DocumentReader();
            reader.setContentHandler(getContentHandler());
            reader.parse(source);
        } catch (SAXException e) {
            if (e.getException() instanceof FOPException) {
                // Undo exception tunneling.
                throw (FOPException)e.getException();
            } else {
                throw new FOPException(e);
            }
        } catch (IOException e) {
            throw new FOPException(e);
        }

    }

    /**
     * Runs the formatting and renderering process using the previously set
     * parser, input source, renderer and output stream.
     * If the renderer was not set, default to PDF.
     * If no parser was set, and the input source is not a dom document,
     * get a default SAX parser.
     * @throws IOException in case of IO errors.
     * @throws FOPException if anything else goes wrong.
     */
    public synchronized void run() throws IOException, FOPException {
        if (!isInitialized()) {
            initialize();
        }
        if (renderer == null) {
            setRenderer(RENDER_PDF);
        }

        if (source == null) {
            throw new FOPException("InputSource is not set.");
        }

        if (reader == null) {
            if (!(source instanceof DocumentInputSource)) {
                try {
                    SAXParserFactory spf = javax.xml.parsers.SAXParserFactory.newInstance();
                    spf.setNamespaceAware(true);
                    reader = spf.newSAXParser().getXMLReader();
                } catch (SAXException e) {
                    throw new FOPException(e);
                } catch (ParserConfigurationException e) {
                    throw new FOPException(e);
                }
            }
        }

        if (source instanceof DocumentInputSource) {
            render(((DocumentInputSource)source).getDocument());
        } else {
            render(reader, source);
        }
    }

/*  Following code was stolen from org.apache.batik.util and modified slightly.
    It does what sun.misc.Service probably does, but it cannot be relied on.
    Hopefully, it will be part of standard jdk sometime.
    This code was formerly part of a "Service" class, the contents of which have
    been extracted here so that they can be used by both Session and the now-
    deprecated Driver class.
*/

    /**
     * Map of services in the classpath
     */
    private static Map providerMap = new java.util.Hashtable();

    /**
     * Loads services present in the class path.
     */
    public static synchronized Iterator providers(Class cls) {
        ClassLoader cl = cls.getClassLoader();
        // null if loaded by bootstrap class loader
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        String serviceFile = "META-INF/services/" + cls.getName();

        // getLogger().debug("File: " + serviceFile);

        List lst = (List)providerMap.get(serviceFile);
        if (lst != null) {
            return lst.iterator();
        }

        lst = new java.util.Vector();
        providerMap.put(serviceFile, lst);

        Enumeration e;
        try {
            e = cl.getResources(serviceFile);
        } catch (IOException ioe) {
            return lst.iterator();
        }

        while (e.hasMoreElements()) {
            try {
                java.net.URL u = (java.net.URL)e.nextElement();
                //getLogger().debug("URL: " + u);

                InputStream is = u.openStream();
                Reader r = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(r);

                String line = br.readLine();
                while (line != null) {
                    try {
                        // First strip any comment...
                        int idx = line.indexOf('#');
                        if (idx != -1) {
                            line = line.substring(0, idx);
                        }

                        // Trim whitespace.
                        line = line.trim();

                        // If nothing left then loop around...
                        if (line.length() == 0) {
                            line = br.readLine();
                            continue;
                        }
                        // getLogger().debug("Line: " + line);

                        // Try and load the class
                        // Object obj = cl.loadClass(line).newInstance();
                        // stick it into our vector...
                        lst.add(line);
                    } catch (Exception ex) {
                        // Just try the next line
                    }

                    line = br.readLine();
                }
            } catch (Exception ex) {
                // Just try the next file...
            }

        }
        return lst.iterator();
    }

}
