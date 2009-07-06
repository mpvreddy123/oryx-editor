package org.oryxeditor.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;



import de.hpi.xforms.XForm;
import de.hpi.xforms.rdf.XFormsRDFImporter;
import de.hpi.xforms.serialization.XFormsXHTMLExporter;

/**
 * 
 * @author jan-felix.schwarz@student.hpi.uni-potsdam.de
 *
 */
public class XFormsExportServlet extends HttpServlet {
	
	private static final long serialVersionUID = 6084194342174761093L;

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		//TODO: examination of the HTTP Accept header (see http://www.w3.org/TR/xhtml-media-types/#media-types)
		res.setContentType("application/xhtml+xml");
		
		String rdf = req.getParameter("data");
		String cssUrl = req.getParameter("css");
		System.out.println(rdf);		
		if (cssUrl == null) {
			cssUrl = req.getContextPath() + "/css/xforms_default.css";
		}
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(rdf.getBytes()));
			res.getWriter().write(exportForm(document, cssUrl));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		
		//TODO: examination of the HTTP Accept header (see http://www.w3.org/TR/xhtml-media-types/#media-types)
		res.setContentType("application/xhtml+xml");
		
		String path = req.getParameter("path");
		String cssUrl = req.getParameter("css");
		
		if(path==null)
			return;
		if(cssUrl==null)
			cssUrl = req.getContextPath() + "/css/xforms_default.css";
		
		if(path.endsWith("/self")) path = path.substring(0, path.indexOf("/self"));
		
		Repository repo = new Repository(Repository.getBaseUrl(req));
		String rdf = repo.getModel(path, "rdf");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(rdf.getBytes()));
			res.getWriter().write(exportForm(document, cssUrl));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String exportForm(Document document, String cssUrl) {
		
		try {
			
			XFormsRDFImporter importer = new XFormsRDFImporter(document);
			XForm form = importer.loadXForm();
			
			XFormsXHTMLExporter exporter = new XFormsXHTMLExporter(form);
			Document xhtmlDoc = exporter.getXHTMLDocument(cssUrl);
			
			/*//System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMImplementationSourceImpl");
			
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
			LSOutput out = impl.createLSOutput();
			out.setByteStream(res.getOutputStream());
			LSSerializer writer = impl.createLSSerializer();
			writer.write(xhtmlDoc, out);
			
			DOMImplementation implementation = DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0");
			DOMImplementationLS feature = (DOMImplementationLS) implementation.getFeature("LS", "3.0");
			LSSerializer serializer = feature.createLSSerializer();
			LSOutput output = feature.createLSOutput();
			output.setByteStream(res.getOutputStream());
			serializer.write(xhtmlDoc, output);*/
			
			OutputFormat format = new OutputFormat(xhtmlDoc);
			format.setLineWidth(65);
			format.setIndenting(true);
			format.setLineSeparator("\n");

			// TODO: newlines in output (weird it doesn't work)
		    
		    StringWriter output = new StringWriter();
		    XMLSerializer serial = new XMLSerializer(output, format);
			serial.serialize(xhtmlDoc);
			
			return output.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "error";
	}

}
