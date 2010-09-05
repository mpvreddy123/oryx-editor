package de.hpi.AdonisSupport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.Assert;
import org.xmappr.Element;
import org.xmappr.RootElement;

import de.hpi.layouting.model.LayoutingDockers;
import de.hpi.layouting.model.LayoutingDockers.Point;

//<!ELEMENT CONNECTOR (FROM, TO, (ATTRIBUTE | RECORD | INTERREF)*)>
//<!ATTLIST CONNECTOR
//  id    ID    #IMPLIED
//  class CDATA #REQUIRED
//>

@RootElement("CONNECTOR")
public class AdonisConnector extends AdonisStencil{
	
	private static final long serialVersionUID = -3850673995571310465L;

	/**
	 * creates the special kind of "is inside" relation -> is needed by Adonis, but not by Oryx
	 * @param model - the model of father and child
	 * @param father 
	 * @param child 
	 * @return an isInside Connector
	 */
	public static AdonisConnector insideRelation(String language, AdonisModel model,AdonisInstance father, AdonisInstance child){
		AdonisConnector isInside = new AdonisConnector();
		
		isInside.setAdonisIdentifier(Unifier.getAdonisIdentifier("is inside", language));
		isInside.getResourceId();
		isInside.setModel(model);
		isInside.setId(Helper.generateId("con."));
		
		//set the target (to) to the father
		AdonisConnectionPoint point = new AdonisConnectionPoint();
		point.setInstance(father);
		isInside.setTo(point);
		//set the source (from) to the child
		point = new AdonisConnectionPoint();
		point.setInstance(child);
		isInside.setFrom(point);
		
		AdonisAttribute attribute = new AdonisAttribute();
		attribute.setAdonisName("AutoConnect");
		attribute.setType("STRING");
		
		ArrayList<AdonisAttribute> list =new ArrayList<AdonisAttribute>();
		list.add(attribute);
		isInside.setAttribute(list);
		
		return isInside;
	}
	
	@Element(name="FROM", targetType=AdonisFrom.class)
	protected AdonisConnectionPoint from;
	
	@Element(name="TO", targetType=AdonisTo.class)
	protected AdonisConnectionPoint to;
	
	@Element(name="ATTRIBUTE", targetType=AdonisAttribute.class)
	protected ArrayList<AdonisAttribute> attribute;
	
	@Element(name="RECORD")
	protected ArrayList<AdonisRecord> record;
	
	@Element(name="INTERREF", targetType=AdonisInterref.class)
	protected ArrayList<AdonisInterref> interref;
	
	public void setFrom(AdonisConnectionPoint e){
		from = e;
		e.setConnector(this);
	}
	
	public void setTo(AdonisConnectionPoint e){
		to = e;
		e.setConnector(this);
	}
	public AdonisConnectionPoint getFrom(){
		return from;
	}
	public AdonisConnectionPoint getTo(){
		return to;
	}
	
	


	public ArrayList<AdonisRecord> getRecord(){
		if (record == null){
			record = new ArrayList<AdonisRecord>();
		}
		return record;
	}
	
	public void setRecord(ArrayList<AdonisRecord> list){
		record = list;
	}
	
	


	public ArrayList<AdonisInterref> getInterref(){
		if (interref == null){
			interref = new ArrayList<AdonisInterref>();
		}
		return interref;
	}
	
	public void setInterref(ArrayList<AdonisInterref> list){
		interref = list;
	}

	public ArrayList<AdonisAttribute> getAttribute(){
		if (attribute == null){
			attribute = new ArrayList<AdonisAttribute>();
		}
		return attribute;
	}
	
	public void setAttribute(ArrayList<AdonisAttribute> list){
		attribute = list;
	}
	
	//*************************************************************************
	//* methods for computing purposes
	//**************************************************************************
	
	public boolean isConnector(){
		return true;
	}
	
	public static boolean handleStencil(String oryxName){
		Set<String> connectors = new HashSet<String>();
		connectors.add("has process");
		connectors.add("has cross-reference");
		connectors.add("has note");
		connectors.add("value flow");
		connectors.add("owns");
		return connectors.contains(oryxName);
	}
	
	public String getName(){
		return getId();
	}
	
	@Override
	public AdonisAttribute getAttribute(String identifier, String lang){
		for (AdonisAttribute anAttribute : getAttribute()){
			if (identifier.equals(Unifier.getOryxIdentifier(anAttribute.getAdonisName(),getLanguage())))
				return anAttribute;
		}
		return null;
	}
	
	public AdonisInstance getAsInstance(AdonisConnectionPoint target){
		for (AdonisInstance instance : getModel().getInstance()){
			if (instance.getName().equals(target.getInstanceName())
					&& instance.getOryxIdentifier().equals(
							Unifier.getOryxIdentifier(target.getAdonisIdentifier(),getLanguage()))){
				return instance;
			}
		}
		return null;
	}
	
	@Override
	protected Double[] getAdonisGlobalBounds() {
		// TODO Auto-generated method stub
		return new Double[]{0.0,0.0,0.0,0.0};
	}
	
	
	/**
	 * upper left x,y | lower right x,y
	 * @return
	 */
	public Double[] getOryxBounds(){
		AdonisInstance source = getAsInstance(getFrom());
		AdonisInstance target = getAsInstance(getTo());
		
		Double[] boundingRect = new Double[4];
		if (source.getCenter()[0] < target.getCenter()[0]){
			boundingRect[0] = source.getCenter()[0];
			boundingRect[2] = target.getCenter()[0];
		} else {
			boundingRect[0] = target.getCenter()[0];
			boundingRect[2] = source.getCenter()[0];
		}
		if (source.getCenter()[1] < target.getCenter()[1]){
			boundingRect[1] = source.getCenter()[1];
			boundingRect[3] = target.getCenter()[1];
		} else {
			boundingRect[1] = target.getCenter()[1];
			boundingRect[3] = source.getCenter()[1];
		}
		return boundingRect;
	}
	
	
	
	private LayoutingDockers filterDockerPoints(String list){
		LayoutingDockers dockers = new LayoutingDockers();
		//remove all unnecessary information like number of dockers, measuring unit or index
		String[] filtered = list
			.replaceAll("EDGE\\s\\d\\s", "")
			.replaceAll("[xy]\\d\\:","")
			.replaceAll("cm", "")
			.replaceAll("index\\:.+","")
			.replaceAll("\\s+", " ")
			.trim().split(" ");
		//the first number is the number of points - but we extract them according the existing points
		if (filtered.length == 1 && filtered[0].length() == 0){
			return dockers;
		}
		if (filtered.length % 2 != 0){
			Logger.e("dockers of adonis connector are missing at least a coordinate");
			return dockers;
		}
		for (int i = 0; i < filtered.length; i = i + 2){
			dockers.addPoint(
					Double.parseDouble(filtered[i]),
					Double.parseDouble(filtered[i+1]));
		}
		return dockers;
	}
	
	//*************************************************************************
	//* Java -> JSON
	//**************************************************************************
	
	@Override
	public void prepareAdonisToOryx() throws JSONException{
		super.prepareAdonisToOryx();
		// this attribute is created during export
		addUsed(getAttribute("connector number",getLanguage()));
	}
	
	@Override
	public void writeJSONchildShapes(JSONObject json) throws JSONException {
		getJSONArray(json,"childShapes");
		
	}

	@Override
	public void writeJSONproperties(JSONObject json) throws JSONException {
		JSONObject properties = getJSONObject(json, "properties");
		if (getOryxIdentifier().equals("value flow")){
			properties.put("name", getName());
		}
		
		AdonisAttribute element = null;
		
		element = getAttribute("type",getLanguage());
		if (element != null && element.getElement() != null){
			properties.put("type", Unifier.getOryxIdentifier(element.getElement(),getLanguage()));
			addUsed(element);
		}
	}

	/**
	 * to make the import easier to handle, dockers are ignored
	 */
	@Override
	public void writeJSONdockers(JSONObject json) throws JSONException {
		JSONArray dockers = getJSONArray(json, "dockers");
		JSONObject temp = new JSONObject();

		Double[] bounds = getAsInstance(getFrom()).getOryxGlobalBounds(); 
		temp.put("x",(bounds[2]-bounds[0])/2);
		temp.put("y",(bounds[3]-bounds[1])/2);
		dockers.put(temp);
		
		//<ATTRIBUTE name="Positions" type="STRING">
		//EDGE 3 x1:1.5cm y1:5.5cm x2:4cm y2:5.5cm x3:4cm y3:3cm index:7
		//EDGE 3 x1:2.5cm y1:5.5cm x2:4cm y2:5.5cm x3:4cm y3:3cm index:7
		//EDGE 3 x1:3cm   y1:5.5cm x2:4cm y2:5.5cm x3:4cm y3:3cm index:7
		//</ATTRIBUTE>
		AdonisAttribute positions = getAttribute("positions",getLanguage());
		if (positions != null){
			addUsed(positions);
			LayoutingDockers points = filterDockerPoints(positions.getElement());
			for (Point point : points.getPoints()){
				temp = new JSONObject();
				temp.put("x", point.x*CENTIMETERTOPIXEL);
				temp.put("y", point.y*CENTIMETERTOPIXEL);
				dockers.put(temp);
			}
		}
		
		bounds = getAsInstance(getTo()).getOryxGlobalBounds();
		temp = new JSONObject();
		temp.put("x",(bounds[2]-bounds[0])/2);
		temp.put("y",(bounds[3]-bounds[1])/2);
		dockers.put(temp);
	}

	@Override
	public void writeJSONbounds(JSONObject json) throws JSONException {
		//get the position and size which looks like
		//	EDGE index:5
		//	EDGE x:2.50cm y:7.00cm index:2 or
		//	NODE x:1cm y:11.5cm w:.5cm h:.6cm index:8
		Double[] boundingRect = getOryxBounds();
		
		JSONObject bounds = getJSONObject(json,"bounds");
		
		JSONObject temp = getJSONObject(bounds,"upperLeft");
		temp.put("x",boundingRect[0]);
		temp.put("y",boundingRect[1]);
		
		temp = getJSONObject(bounds,"lowerRight");
		temp.put("x",boundingRect[2]);
		temp.put("y",boundingRect[3]);	
		
	}

	@Override
	public void writeJSONoutgoing(JSONObject json) throws JSONException {
		JSONArray outgoing = getJSONArray(json,"outgoing");
		JSONObject temp = null;
		AdonisInstance instance = getAsInstance(getTo());
		
		temp = new JSONObject();
		temp.putOpt("resourceId", instance.getResourceId());
		outgoing.put(temp);
	}

	@Override
	public void writeJSONtarget(JSONObject json) throws JSONException {
		JSONObject target = getJSONObject(json,"target");		
		target.putOpt("resourceId", getAsInstance(getTo()).getResourceId());
	}
	
	public void writeJSONunused(JSONObject json) throws JSONException{
		//TODO for development - remove
		if (true)
			return;
		//JSONObject unused = getJSONObject(json, "unused");
		SerializableContainer<XMLConvertible> unused = new SerializableContainer<XMLConvertible>();
		
		try {
			for (AdonisAttribute aAttribute : getAttribute()){
				if (getUsed().indexOf(aAttribute) < 0){
					unused.getElements().add(aAttribute);
				}
			}
			for (AdonisRecord aRecord : getRecord()){
				if (getUsed().indexOf(aRecord) < 0){
					unused.getElements().add(aRecord);
				}
			}
			for (AdonisInterref aInterref : getInterref()){
				if (getUsed().indexOf(aInterref) < 0){
					unused.getElements().add(aInterref);
				}
			}
			//unused.put("attributes", makeStorable(unusedAttributes));
			json.put("unused", makeStorable(unused));
		} catch (JSONException e) {
			Logger.e("could not write unused elements and attributes",e);
		}
	}

	//*************************************************************************
	//* JSON -> Java
	//*************************************************************************
	
	public void completeOryxToAdonis(){
		
		Logger.d("Created connector class "+getOryxIdentifier()+" - "+getName()+" - "+resourceId);
		getModel().getConnector().add(this);
		super.completeOryxToAdonis();
	}
	
	public void readJSONstencil(JSONObject json) throws JSONException{
		if (getAdonisIdentifier() == null){
			JSONObject stencil = json.getJSONObject("stencil");
			setOryxIndentifier(stencil.getString("id"));
//			setAdonisIndentifier(getAdonisIdentifier());
			Logger.d("working on stencil: "+getOryxIdentifier()+" id "+resourceId);
		}
	}
	
	public void readJSONproperties(JSONObject json) throws JSONException{
		JSONObject properties = json.getJSONObject("properties");
		String type = properties.optString("type");
		if (type != null){
			getAttribute().add(AdonisAttribute.create(
					getLanguage(), 
					"type", 
					"STRING", 
					Unifier.getOryxIdentifier(type,getLanguage())));
		}
	}
	
	public void readJSONchildShapes(JSONObject json){
		//XXX currently there are no childShapes in connectors
	}
	
	public void readJSONbounds(JSONObject json){
		//XXX bounds are not recognized by Adonis - not used
	}
	
	public void readJSONdockers(JSONObject json) {
		String positions = "EDGE ";
		String points = "";
		try {
			JSONArray dockers = json.getJSONArray("dockers");
			//Adonis saves the number of points
			points += dockers.length() - 2;
			JSONObject temp = null;
			//the first and the last docker are in Adonis implicit
			for (int i = 1; i < dockers.length() - 1; i++){
				temp = dockers.getJSONObject(i);
				points += " x"+i+":"+(temp.getDouble("x")/CENTIMETERTOPIXEL)+"cm"
							+" y"+i+":"+(temp.getDouble("y")/CENTIMETERTOPIXEL)+"cm";
			}
		} catch (JSONException e){
			Logger.e("could not restore docker points",e);
			points = "0";
		}
		
		positions += points +" ";
		
		getAttribute().add(AdonisAttribute.create(
				getLanguage(),
				"positions",
				"STRING",
				positions));
	}
	
	@SuppressWarnings("unchecked")
	public void readJSONunused(JSONObject json){
		SerializableContainer<XMLConvertible> unused;
		String encodedString;
		try {
			encodedString = json.getString("unused");
			if (encodedString != null){
				unused = (SerializableContainer<XMLConvertible>) fromStorable(encodedString);
				for (XMLConvertible element : unused.getElements()){
					if (element.getClass() == AdonisAttribute.class){
						getAttribute().add((AdonisAttribute)element);
					}
					if (element.getClass() == AdonisRecord.class){
						getRecord().add((AdonisRecord)element);
					}
					if (element.getClass() == AdonisInterref.class){
						getInterref().add((AdonisInterref)element);
					}
				}
			}
		} catch (JSONException e){
			Logger.e("could not restore unused attributes",e);
		}
		
	}
	
	public void readJSONoutgoing(JSONObject json) throws JSONException{
		//JSONArray outgoing = json.getJSONArray("outgoing");
		//XXX currently not needed
	}
	
	public void readJSONtarget(JSONObject json) throws JSONException{
		JSONObject target = json.getJSONObject("target");
		AdonisConnectionPoint connectionPoint = new AdonisConnectionPoint();
		connectionPoint.setInstance(this);
		
		String instanceResourceId = null;
		AdonisInstance instance = null;
		
		instanceResourceId = target.getString("resourceId");
		
		Assert.notNull(instanceResourceId,"ResourceId during export to Adonis null");
		Assert.isTrue(getModelChildren().contains(this));
		
		instance = (AdonisInstance)getModelChildByResourceId(instanceResourceId);
		
		if (instance == null){
			instance = new AdonisInstance();
			instance.setResourceId(instanceResourceId);
			instance.setModel(getModel());
			Logger.d("created new Instance from connector - "+instance.getName());
		}
		connectionPoint = new AdonisConnectionPoint();
		connectionPoint.setInstance(instance);
		setTo(connectionPoint);
	}
}
