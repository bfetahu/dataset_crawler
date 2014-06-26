package entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class DBPediaAnnotation implements Serializable {

    private static final long serialVersionUID = 1L;
    public String uri;
    public int support;
    public Set<OntologyTypeImpl> ontologytypes;
    //stores from where the annotation was generated
    public String surfacefrom;
    //using standard IR similarity measures, we compute how likely is the annotation to be correct given the context of the resource.	
    public double annotationconfidence;
    //each annotation has also an additional description and label extracted from DBpedia
    public Entry<String, String> extraannotation;
    //category associations
    public CategoryAnnotation category;

    public DBPediaAnnotation(String uri) {
        ontologytypes = new HashSet<OntologyTypeImpl>();
        this.uri = uri;
        // TODO Auto-generated constructor stub
        category = new CategoryAnnotation();
    }

    public DBPediaAnnotation(String uri, int support) {
        ontologytypes = new HashSet<OntologyTypeImpl>();
        this.uri = uri;
        this.support = support;
        category = new CategoryAnnotation();
    }

    public String getCategoryRepresentation() {
        Set<String> catset = new HashSet<String>();

        for (CategoryAnnotation cat : category.children) {
            category.getStringRepresentation(cat, catset);
        }

        return catset.toString();
    }
    /*
     * Set the annotation types from the enrichment output from DBpedia Spotlight.
     */

    public void setAnnotationTypes(String typestr) {
        //extract the type of the annotation.
        String[] types = typestr.split(",");

        for (String type : types) {
            OntologyTypeImpl ontimpl = new OntologyTypeImpl();
            ontimpl.id = type;
            ontologytypes.add(ontimpl);
        }
    }

    /*
     * Get the string representation of the annotation object. 
     * (non-Javadoc)
     * @see org.dbpedia.spotlight.model.DBpediaResource#toString()
     */
    public String toString(boolean printTypes) {
        StringBuffer sb = new StringBuffer();

        sb.append(surfacefrom);
        sb.append(":\t");
        sb.append(uri);
        sb.append(":\t");
        sb.append(annotationconfidence);
        sb.append("\n");

       if (printTypes) {
            int counter = 0;
            for (OntologyTypeImpl ot : ontologytypes) {
                if (counter != 0) {
                    sb.append(", ");
                }
                sb.append(ot.typeID());
                counter++;
            }
        }

        return sb.toString();
    }

    public String getAnnotationURI() {
        return uri;
    }

    public Set<String> getSubCategoryRepresentation(Set<String> sb, CategoryAnnotation category) {
        sb.add(category.categoryname);

        if (category.children != null && !category.children.isEmpty()) {
            for (CategoryAnnotation child : category.children) {
                getSubCategoryRepresentation(sb, child);
            }
        }
        return sb;
    }
}
