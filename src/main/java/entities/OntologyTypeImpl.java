package entities;

import java.io.Serializable;

public class OntologyTypeImpl implements Serializable{
	private static final long serialVersionUID = -4430068940030143326L;
	public String uri;
	public String id;
	
	public String getFullUri() {
		// TODO Auto-generated method stub
		return uri;
	}

	public String typeID() {
		// TODO Auto-generated method stub
		return id;
	}
	
        @Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(id);
		sb.append("]-");
		sb.append(uri);
		
		return sb.toString();
	}
}