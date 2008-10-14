package de.hpi.xforms.rdf;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.hpi.xforms.*;

/**
 * Import XForm from Oryx RDF representation
 * main method: loadXForm()
 * 
 * @author jan-felix.schwarz@student.hpi.uni-potsdam.de
 * 
 */
public class XFormsRDFImporter {
	
	protected Document doc;
	protected Document instanceModelDoc;
	protected XFormsFactory factory;

	protected class ImportContext {
		XForm form;
		Map<String, XFormsElement> objects; // key = resource id, value = xforms element
		Map<XFormsElement, String> parentRelationships; // key = xforms element, value = parent resource id
		Map<XFormsElement, Bind> bindings; // key = xforms element, value = associated bind element
	}

	public XFormsRDFImporter(Document doc) {
		super();
		this.doc = doc;
	}
	
	public XFormsRDFImporter(Document doc, Document instanceModelDoc) {
		super();
		this.doc = doc;
		this.instanceModelDoc = instanceModelDoc;
	}
	
	public XForm loadXForm() {
		
		Node root = getRootNode(doc);
		if (root == null)
			return null;

		factory = new XFormsFactory();

		ImportContext c = new ImportContext();
		c.form = factory.createXForm();
		c.objects = new HashMap<String, XFormsElement>();
		c.parentRelationships = new HashMap<XFormsElement, String>();
		c.bindings = new HashMap<XFormsElement, Bind>();
		c.form.setModel(factory.createModel());
		c.form.setResourceId("#oryx-canvas123");
		c.objects.put("#oryx-canvas123", c.form);
		
		if(root.hasChildNodes()) {
			for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
				
				if (node instanceof Text)
					continue;

				String type = getType(node);
				if (type == null)
					continue;
				
				if (type.equals("Input")) {
					addInput(node, c);
				} else if (type.equals("Secret")) {
					addSecret(node, c);
				} else if (type.equals("Textarea")) {
					addTextarea(node, c);
				} else if (type.equals("Output")) {
					addOutput(node, c);
				} else if (type.equals("Upload")) {
					addUpload(node, c);
				} else if (type.equals("Range")) {
					addRange(node, c);
				} else if (type.equals("Trigger")) {
					addTrigger(node, c);
				} else if (type.equals("Submit")) {
					addSubmit(node, c);
				} else if (type.equals("Select")) {
					addSelect(node, c);
				} else if (type.equals("Group")) {
					addGroup(node, c);
				} else if (type.equals("Repeat")) {
					addRepeat(node, c);
				} else if (type.equals("Label")) {
					addLabel(node, c);
				} else if (type.equals("Help")) {
					addHelp(node, c);
				} else if (type.equals("Hint")) {
					addHint(node, c);
				} else if (type.equals("Alert")) {
					addAlert(node, c);
				} else if (type.equals("Item")) {
					addItem(node, c);
				} else if (type.equals("Action")) {
					addAction(node, c);
				} else if (type.equals("SetValue")) {
					addSetValue(node, c);
				} else if (type.equals("Insert")) {
					addInsert(node, c);
				} else if (type.equals("Delete")) {
					addDelete(node, c);
				} else if (type.equals("SetIndex")) {
					addSetIndex(node, c);
				} else if (type.equals("Toggle")) {
					addToggle(node, c);
				} else if (type.equals("SetFocus")) {
					addSetFocus(node, c);
				} else if (type.equals("Dispatch")) {
					addDispatch(node, c);
				} else if (type.equals("Rebuild")) {
					addRebuild(node, c);
				} else if (type.equals("Recalculate")) {
					addRecalculate(node, c);
				} else if (type.equals("Revalidate")) {
					addRevalidate(node, c);
				} else if (type.equals("Refresh")) {
					addRefresh(node, c);
				} else if (type.equals("Reset")) {
					addReset(node, c);
				} else if (type.equals("Load")) {
					addLoad(node, c);
				} else if (type.equals("Send")) {
					addSend(node, c);
				} else if (type.equals("Message")) {
					addMessage(node, c);
				}          
				
			}
		}
		
		setupParentRelationships(c);
		addModel(c);
		setupBinds(c);
		return c.form;
	}
	
	private void handleAttributes(Node node, XFormsElement element, ImportContext c) {
		if (node.hasChildNodes()) {
			Node n = node.getFirstChild();
			while ((n = n.getNextSibling()) != null) {
				if (n instanceof Text)
					continue;
				
				String attribute = n.getNodeName().substring(n.getNodeName().indexOf(':') + 1);
				
				if (n.getNamespaceURI().equals("http://oryx-editor.org/")) {
					// handle attributes of namespace oryx
					
					if (attribute.equals("bounds")) {
						if(element instanceof XFormsUIElement) {
							XFormsUIElement uiElement = (XFormsUIElement) element;
							String[] bounds = getContent(n).split(",");
							uiElement.setXPosition(Integer.parseInt(bounds[0]));
							uiElement.setYPosition(Integer.parseInt(bounds[1]));
						} else if(element instanceof ListUICommon) {
							ListUICommon listUICommon = (ListUICommon) element;
							String[] bounds = getContent(n).split(",");
							listUICommon.setYPosition(Integer.parseInt(bounds[1]));
						}
					}
					
				} else if (n.getNamespaceURI().equals("http://raziel.org/")) {
					// handle attributes of namespace raziel
					
					if (attribute.equals("parent")) {
						c.parentRelationships.put(element, getResourceId(getAttributeValue(n, "rdf:resource")));
					}
					
				} else if (n.getNamespaceURI().equals("http://xforms-editor.org/")) {
					// handle attributes of namespace xforms
					
					if(element.getAttributes().containsKey(attribute))
						element.getAttributes().put(attribute, getContent(n));
					else
						handleModelItemProperty(attribute, getContent(n), element, c);
					
				}
				
			}
		}
	}
	
	private void handleModelItemProperty(String attribute, String value, XFormsElement element, ImportContext c) {
		if(!element.getAttributes().containsKey("bind")) return;
		Bind bind = new Bind();
		if(!bind.getAttributes().containsKey(attribute)) return;
		if(c.bindings.containsKey(element))
			bind = c.bindings.get(element);
		bind.getAttributes().put(attribute, value);
		c.bindings.put(element, bind);
	}
	
	private void setupBinds(ImportContext c) {
		for(XFormsElement element : c.bindings.keySet()) {
			
			// use ref attribute of control element as nodeset attribute for bind element
			String xPath = element.getAttributes().get("ref");
			if(xPath!=null) {
				Bind bind = c.bindings.get(element);
				String bindId = "bind_";
				if(element.getAttributes().get("id")==null)
					bindId += element.getResourceId();
				else 
					bindId += element.getAttributes().get("id");
				bind.getAttributes().put("id", bindId);
				element.getAttributes().put("bind", bindId);
				
				if((element instanceof XFormsUIElement) && (!xPath.startsWith("/")))
					xPath = getNodesetContext((XFormsUIElement) element) + xPath;
				bind.getAttributes().put("nodeset", xPath);
				
				c.form.getModel().getBinds().add(bind);
			}
			
			//element.getAttributes().put("ref", null);
		}
	}
	
	private void setupParentRelationships(ImportContext c) {
		for(XFormsElement element : c.objects.values()) {
			String parentResourceId = c.parentRelationships.get(element);
			if(parentResourceId!=null) {
				XFormsElement parent = c.objects.get(parentResourceId);
				
				if(element instanceof XFormsUIElement) {
					if(parent instanceof UIElementContainer) {
						XFormsUIElement uiElement = (XFormsUIElement) element;
						uiElement.setParent((UIElementContainer) parent);
					}
				} else if(element instanceof ListUICommon) {
					if(parent instanceof ListUICommonContainer) {
						ListUICommon listUICommon = (ListUICommon) element;
						listUICommon.setParent((ListUICommonContainer) parent);
					}
				} else if(element instanceof AbstractAction) {
					if(parent instanceof ActionContainer) {
						ActionContainer actionContainer = (ActionContainer) parent;
						actionContainer.getActions().add((AbstractAction) element);
					}
				} else if(element instanceof Label) {
					if(parent instanceof LabelContainer) {
						LabelContainer labelContainer = (LabelContainer) parent;
						labelContainer.setLabel((Label) element);
					}
				} else if(element instanceof Help) {
					if(parent instanceof UICommonContainer) {
						UICommonContainer uiCommonContainer = (UICommonContainer) parent;
						uiCommonContainer.setHelp((Help) element);
					}
				} else if(element instanceof Hint) {
					if(parent instanceof UICommonContainer) {
						UICommonContainer uiCommonContainer = (UICommonContainer) parent;
						uiCommonContainer.setHint((Hint) element);
					}
				} else if(element instanceof Alert) {
					if(parent instanceof UICommonContainer) {
						UICommonContainer uiCommonContainer = (UICommonContainer) parent;
						uiCommonContainer.setAlert((Alert) element);
					}
				}  else if(element instanceof Submission) {
					// note: Submission not implemented in stencilset atm
					c.form.getModel().getSubmissions().add((Submission) element);
					if(parent instanceof Submit) {
						((Submit) parent).setSubmission((Submission) element);
					}
				}
				
			} else {
				// no parent relationship found: set form as parent
				if(element instanceof XFormsUIElement) {
					XFormsUIElement uiElement = (XFormsUIElement) element;
					uiElement.setParent(c.form);
				}
			}
		}
		
	}
	
	private void addModel(ImportContext c) {
		c.form.getModel().setInstance(factory.createInstance());
		if(instanceModelDoc==null)
			generateInstanceModelDoc(c);
		c.form.getModel().getInstance().setContent(instanceModelDoc);
	}
	
	private void generateInstanceModelDoc(ImportContext c) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			instanceModelDoc = builder.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		// instance model generation
		// this could be obsolete if XForms implementation supports lazy authoring
		
		Element root = (Element) instanceModelDoc.createElement("data");
		root.setAttribute("xmlns", "");
		instanceModelDoc.appendChild(root);
		
		for(XFormsElement element : c.objects.values()) {
			if(!(element instanceof XFormsUIElement)) continue;
			XFormsUIElement uiElement = (XFormsUIElement) element;
			
			String xPath = uiElement.getAttributes().get("ref");
			if(xPath!=null) {
				if(!xPath.startsWith("/")) xPath = getNodesetContext(uiElement) + xPath;
				
				Node instanceModelNode = (Node) root;
				
				// handle references to attributes
				if(xPath.split("@").length==2)
					xPath = xPath.split("@")[0];
				
				for(String tagName : xPath.split("/")) {
					if(tagName.length()>0) {
						Node child = getChild(instanceModelNode, tagName, null);
						if(child==null) {
							// create new element
							child = (Node) instanceModelDoc.createElement(tagName);
							instanceModelNode.appendChild(child);
						}
						instanceModelNode = child;
					}
				}
				
			}
			
		}
	}
	
	private String getNodesetContext(XFormsUIElement element) {
		String nodesetContext = "";
		while(element.getParent()!=null && !(element.getParent() instanceof XForm)) {
			element = (XFormsUIElement) element.getParent();
			String nodeset = element.getAttributes().get("nodeset");
			if(nodeset!=null) {
				nodesetContext += nodeset + "/";
			} else {
				// for children of group elements
				nodeset = element.getAttributes().get("ref");
				if(nodeset!=null) nodesetContext += nodeset + "/";
			}
		}
		return nodesetContext;
	}
	
	private void addInput(Node node, ImportContext c) {
		Input input = factory.createInput();
		input.setResourceId(getResourceId(node));
		c.objects.put(input.getResourceId(), input);
		handleAttributes(node, input, c);
	}
	
	private void addSecret(Node node, ImportContext c) {
		Secret secret = factory.createSecret();
		secret.setResourceId(getResourceId(node));
		c.objects.put(secret.getResourceId(), secret);
		handleAttributes(node, secret, c);
	}
	
	private void addTextarea(Node node, ImportContext c) {
		Textarea textarea = factory.createTextarea();
		textarea.setResourceId(getResourceId(node));
		c.objects.put(textarea.getResourceId(), textarea);
		handleAttributes(node, textarea, c);
	}
	
	private void addOutput(Node node, ImportContext c) {
		Output output = factory.createOutput();
		output.setResourceId(getResourceId(node));
		c.objects.put(output.getResourceId(), output);
		handleAttributes(node, output, c);
	}
	
	private void addUpload(Node node, ImportContext c) {
		Upload upload = factory.createUpload();
		upload.setResourceId(getResourceId(node));
		c.objects.put(upload.getResourceId(), upload);
		handleAttributes(node, upload, c);
	}
	
	private void addRange(Node node, ImportContext c) {
		Range range = factory.createRange();
		range.setResourceId(getResourceId(node));
		c.objects.put(range.getResourceId(), range);
		handleAttributes(node, range, c);
	}
	
	private void addTrigger(Node node, ImportContext c) {
		Trigger trigger = factory.createTrigger();
		trigger.setResourceId(getResourceId(node));
		c.objects.put(trigger.getResourceId(), trigger);
		handleAttributes(node, trigger, c);
	}
	
	private void addSubmit(Node node, ImportContext c) {
		Submit submit = factory.createSubmit();
		submit.setResourceId(getResourceId(node));
		c.objects.put(submit.getResourceId(), submit);
		handleAttributes(node, submit, c);
	}
	
	private void addSelect(Node node, ImportContext c) {
		Select select = factory.createSelect();
		select.setResourceId(getResourceId(node));
		c.objects.put(select.getResourceId(), select);
		handleAttributes(node, select, c);
	}
	
	private void addGroup(Node node, ImportContext c) {
		Group group = factory.createGroup();
		group.setResourceId(getResourceId(node));
		c.objects.put(group.getResourceId(), group);
		handleAttributes(node, group, c);
	}
	
	private void addRepeat(Node node, ImportContext c) {
		Repeat repeat = factory.createRepeat();
		repeat.setResourceId(getResourceId(node));
		c.objects.put(repeat.getResourceId(), repeat);
		handleAttributes(node, repeat, c);
	}
	
	private void addLabel(Node node, ImportContext c) {
		Label label = factory.createLabel();
		label.setResourceId(getResourceId(node));
		c.objects.put(label.getResourceId(), label);
		handleAttributes(node, label, c);
		label.setContent(getContent(getChild(node, "text", "http://xforms-editor.org/")));
	}
	
	private void addHelp(Node node, ImportContext c) {
		Help help = factory.createHelp();
		help.setResourceId(getResourceId(node));
		c.objects.put(help.getResourceId(), help);
		handleAttributes(node, help, c);
		help.setContent(getContent(getChild(node, "message", "http://xforms-editor.org/")));
	}
	
	private void addHint(Node node, ImportContext c) {
		Hint hint = factory.createHint();
		hint.setResourceId(getResourceId(node));
		c.objects.put(hint.getResourceId(), hint);
		handleAttributes(node, hint, c);
		hint.setContent(getContent(getChild(node, "message", "http://xforms-editor.org/")));
	}
	
	private void addAlert(Node node, ImportContext c) {
		Alert alert = factory.createAlert();
		alert.setResourceId(getResourceId(node));
		c.objects.put(alert.getResourceId(), alert);
		handleAttributes(node, alert, c);
		alert.setContent(getContent(getChild(node, "message", "http://xforms-editor.org/")));
	}
	
	private void addItem(Node node, ImportContext c) {
		Item item = factory.createItem();
		item.setResourceId(getResourceId(node));
		c.objects.put(item.getResourceId(), item);
		handleAttributes(node, item, c);
		Value value = factory.createValue();
		handleAttributes(node, value, c); 		// side effect: assigns Item's id to Value object...
		value.getAttributes().put("id", null); 	// ...so reset Value's id attribute
		item.setValue(value);
	}
	
	private void addAction(Node node, ImportContext c) {
		Action action = factory.createAction();
		action.setResourceId(getResourceId(node));
		c.objects.put(action.getResourceId(), action);
		handleAttributes(node, action, c);
	}
	
	private void addSetValue(Node node, ImportContext c) {
		SetValue setValue = factory.createSetValue();
		setValue.setResourceId(getResourceId(node));
		c.objects.put(setValue.getResourceId(), setValue);
		handleAttributes(node, setValue, c);
	}
	
	private void addInsert(Node node, ImportContext c) {
		Insert insert = factory.createInsert();
		insert.setResourceId(getResourceId(node));
		c.objects.put(insert.getResourceId(), insert);
		handleAttributes(node, insert, c);
	}
	
	private void addDelete(Node node, ImportContext c) {
		Delete delete = factory.createDelete();
		delete.setResourceId(getResourceId(node));
		c.objects.put(delete.getResourceId(), delete);
		handleAttributes(node, delete, c);
	}
	
	private void addSetIndex(Node node, ImportContext c) {
		SetIndex setIndex = factory.createSetIndex();
		setIndex.setResourceId(getResourceId(node));
		c.objects.put(setIndex.getResourceId(), setIndex);
		handleAttributes(node, setIndex, c);
	}
	
	private void addToggle(Node node, ImportContext c) {
		Toggle toggle = factory.createToggle();
		toggle.setResourceId(getResourceId(node));
		c.objects.put(toggle.getResourceId(), toggle);
		handleAttributes(node, toggle, c);
	}
	
	private void addSetFocus(Node node, ImportContext c) {
		SetFocus setFocus = factory.createSetFocus();
		setFocus.setResourceId(getResourceId(node));
		c.objects.put(setFocus.getResourceId(), setFocus);
		handleAttributes(node, setFocus, c);
	}
	
	private void addDispatch(Node node, ImportContext c) {
		Dispatch dispatch = factory.createDispatch();
		dispatch.setResourceId(getResourceId(node));
		c.objects.put(dispatch.getResourceId(), dispatch);
		handleAttributes(node, dispatch, c);
	}
	
	private void addRebuild(Node node, ImportContext c) {
		Rebuild rebuild = factory.createRebuild();
		rebuild.setResourceId(getResourceId(node));
		c.objects.put(rebuild.getResourceId(), rebuild);
		handleAttributes(node, rebuild, c);
	}
	
	private void addRecalculate(Node node, ImportContext c) {
		Recalculate recalculate = factory.createRecalculate();
		recalculate.setResourceId(getResourceId(node));
		c.objects.put(recalculate.getResourceId(), recalculate);
		handleAttributes(node, recalculate, c);
	}
	
	private void addRevalidate(Node node, ImportContext c) {
		Revalidate revalidate = factory.createRevalidate();
		revalidate.setResourceId(getResourceId(node));
		c.objects.put(revalidate.getResourceId(), revalidate);
		handleAttributes(node, revalidate, c);
	}
	
	private void addRefresh(Node node, ImportContext c) {
		Refresh refresh = factory.createRefresh();
		refresh.setResourceId(getResourceId(node));
		c.objects.put(refresh.getResourceId(), refresh);
		handleAttributes(node, refresh, c);
	}
	
	private void addReset(Node node, ImportContext c) {
		Reset reset = factory.createReset();
		reset.setResourceId(getResourceId(node));
		c.objects.put(reset.getResourceId(), reset);
		handleAttributes(node, reset, c);
	}
	
	private void addLoad(Node node, ImportContext c) {
		Load load = factory.createLoad();
		load.setResourceId(getResourceId(node));
		c.objects.put(load.getResourceId(), load);
		handleAttributes(node, load, c);
	}
	
	private void addSend(Node node, ImportContext c) {
		Send send = factory.createSend();
		send.setResourceId(getResourceId(node));
		c.objects.put(send.getResourceId(), send);
		handleAttributes(node, send, c);
	}
	
	private void addMessage(Node node, ImportContext c) {
		Message message = factory.createMessage();
		message.setResourceId(getResourceId(node));
		c.objects.put(message.getResourceId(), message);
		handleAttributes(node, message, c);
		message.setContent(getContent(getChild(node, "message", "http://xforms-editor.org/")));
	}
	
	private String getContent(Node node) {
		if (node != null && node.hasChildNodes())
			return node.getFirstChild().getNodeValue();
		return null;
	}

	private String getAttributeValue(Node node, String attribute) {
		Node item = node.getAttributes().getNamedItem(attribute);
		if (item != null)
			return item.getNodeValue();
		else
			return null;
	}

	private String getType(Node node) {
		String type = getContent(getChild(node, "type", "http://oryx-editor.org/"));
		if (type != null)
			return type.substring(type.indexOf('#') + 1);
		else
			return null;
	}

	private String getResourceId(Node node) {
		Node item = node.getAttributes().getNamedItem("rdf:about");
		if (item != null)
			return getResourceId(item.getNodeValue());
		else
			return null;
	}

	private String getResourceId(String id) {
		return id.substring(id.indexOf('#'));
	}

	private Node getChild(Node n, String name, String namespace) {
		if (n == null)
			return null;
		for (Node node=n.getFirstChild(); node != null; node=node.getNextSibling())
			if (node.getNodeName().indexOf(name) >= 0) 
				if(namespace==null||node.getNamespaceURI().equals(namespace))
					return node;
		return null;
	}

	private Node getRootNode(Document doc) {
		Node node = doc.getDocumentElement();
		if (node == null || !node.getNodeName().equals("rdf:RDF"))
			return null;
		return node;
	}

}
