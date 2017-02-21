package org.kisst.gft.odwek;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Set;

import org.kisst.gft.admin.WritesHtml;
import org.kisst.props4j.Props;

public class OnDemandDefinition implements WritesHtml {
	private static LinkedHashMap<String,OnDemandDefinition> standardDefinitions=new LinkedHashMap<String,OnDemandDefinition>();
	public static void addStandardDefinition(String name, OnDemandDefinition def) { standardDefinitions.put(name, def); }
	public static OnDemandDefinition getStandardDefinition(String application) { return standardDefinitions.get(application);}
	public static Set<String> getStandardDefinitionNames() { return standardDefinitions.keySet(); } 

	
	public final String odfolder;
	public final String odapplgroup;
	public final String odapplication;
	private final LinkedHashMap<String, OnDemandField> fields = new LinkedHashMap<String, OnDemandField>();


	public OnDemandDefinition(String odfolder, String odapplgroup, String odapplication) {
		this.odfolder=odfolder;
		this.odapplgroup=odapplgroup;
		this.odapplication=odapplication;
	}

	public OnDemandDefinition(Props props) { this(null, props); }
	public OnDemandDefinition(OnDemandDefinition def) { this(def,null); }
	public OnDemandDefinition(OnDemandDefinition def, Props props) {
		if (props==null) {
			if (def==null)
				throw new IllegalArgumentException("OnDemandDefintion should have non-null props or default definition");
			this.odfolder=def.odfolder;
			this.odapplgroup=def.odapplgroup;
			this.odapplication=def.odapplication;
		}
		else {
			String defName = props.getString("definition",null);
			if (defName!=null) {
				def=getStandardDefinition(defName);
				if (def==null)
					throw new RuntimeException("Unknown name ["+defName+"] for standard OnDemand definition "+props.getFullName());
			}
			if (def==null) {
				this.odfolder=props.getString("folder"); 
				this.odapplgroup=props.getString("applgroup"); 
				this.odapplication=props.getString("application");
			}
			else {
				this.odfolder=props.getString("folder",def.odfolder); 
				this.odapplgroup=props.getString("applgroup", def.odapplgroup); 
				this.odapplication=props.getString("application",def.odapplication);

			}
		}
		if (def!=null)
			addFields(def);
		if (props!=null)
			addFields(props);
	}
	
	public OnDemandDefinition addField(String name, OnDemandField f) { fields.put(name, f); return this; }
	public OnDemandDefinition addOptionalField(String name) { return addField(name, new OptionalField(name)); }
	public OnDemandDefinition addOptionalField(String name, String defaultValue) { return addField(name, new OptionalField(name, defaultValue)); }
	public OnDemandDefinition addMandatoryField(String name) { return addField(name, new MandatoryField(name)); }
	public OnDemandDefinition addFixedField(String name, String fixedValue) { return addField(name, new FixedField(name, fixedValue)); }
	public OnDemandDefinition addFields(OnDemandDefinition def) {
		for (String name: def.fields.keySet())
			addField(name, def.fields.get(name));
		return this;
	}
	
	@Override public String toString() { return "OnDemand("+odfolder+","+odapplgroup+","+odapplication+")"; }

	public Set<String> getFieldNames() { return fields.keySet();}
	public OnDemandField getField(String name) { 
		OnDemandField field=fields.get(name);
		if (field==null)
			throw new RuntimeException("Unknown OnDemand field "+name);
		return field;
	}
	
	public void addFields(Props props) {
		Object obj = props.get("field", null);
		if (obj instanceof Props) {
			Props fieldprops = (Props) obj;
			for (String name: fieldprops.keys())
				addFieldFromProps(name, fieldprops.getProps(name));
		}
	}
	
	protected void addFieldFromProps(String name, Props props) {
		if (props.getString("fixedValue", null)!=null)
			addFixedField(name, props.getString("fixedValue"));
		else if (props.getBoolean("optional", false))
			addField(name, new OptionalField(name, props));
		else
			addField(name, new MandatoryField(name, props));
	}


	@Override public void writeHtml(PrintWriter out) {
		out.println("<h2>Kenmerken</h2>");
		out.println("<table>");
		out.print("<tr>");
		out.print("<td><b>name</b></td>");
		out.print("<td><b>definition</b></td>");
		out.println("</tr>");
		for (String name: fields.keySet()) { 
			OnDemandField fld = fields.get(name);
			out.print("<tr>");
			out.print("<td>"+name+"</td>");
			out.print("<td>"+fld+"</td>");
			out.println("</tr>");
		}
		out.println("</table>");
	}

}