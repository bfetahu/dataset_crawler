package entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryAnnotation implements Serializable {

    private static final long serialVersionUID = 6928606637779569401L;
    public String categoryname;
    public int level = 0;
    public List<CategoryAnnotation> children;

    public CategoryAnnotation() {
        children = new ArrayList<CategoryAnnotation>();
    }

    public Set<String> getChildren() {
        Set<String> rst = new HashSet<String>();

        if (children != null && children.size() != 0) {
            for (CategoryAnnotation child : children) {
                rst.add(child.categoryname);
            }
        }
        return rst;
    }

    public CategoryAnnotation copy() {
        CategoryAnnotation cat = new CategoryAnnotation();

        cat.categoryname = categoryname;
        cat.level = level;

        for (CategoryAnnotation child : children) {
            cat.children.add(child.copy());
        }

        return cat;
    }

    public boolean containsChild(String catname) {
        for (CategoryAnnotation child : children) {
            if (child.categoryname.equals(catname)) {
                return true;
            }
        }
        return false;
    }

    public void getStringRepresentation(CategoryAnnotation cat, Set<String> catset) {
        catset.add(cat.categoryname);
        if (cat.children != null && !cat.children.isEmpty()) {
            for (CategoryAnnotation child : cat.children) {
                getStringRepresentation(child, catset);
            }
        }
    }
}
