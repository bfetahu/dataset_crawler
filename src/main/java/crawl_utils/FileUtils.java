package crawl_utils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtils {
    /*
     * Gets the set of file names, from any directory structure recursively.
     */

    public static void getFilesList(String path, Set<String> filelist) {
        File dir = new File(path);
        if (dir.isFile()) {
            filelist.add(path);
        } else if (dir.isDirectory()) {
            String[] list = dir.list();
            for (String item : list) {
                getFilesList(path + "/" + item, filelist);
            }
        }
    }

    /*
     * Gets the set of file names, from any directory structure recursively.
     */
    public static void getFilesList(String path, Set<String> filelist, String filter) {
        File dir = new File(path);
        if (dir.isFile()) {
            filelist.add(path);
        } else if (dir.isDirectory()) {
            String[] list = dir.list();
            for (String item : list) {
                if (!item.contains(filter)) {
                    continue;
                }

                getFilesList(path + "/" + item, filelist);
            }
        }
    }

    /*
     * Write textual content into a file.
     */
    public static void addTextualContentToFile(String content, String path) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Saves textual data content, into a file.
     */
    public static void saveText(String text, String path) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Saves textual data content, into a file.
     */
    public static void saveText(String text, String path, boolean append) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, append));
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Writes the content of an object, into a file.
     */
    public static void saveObject(Object obj, String path) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
            out.writeObject(obj);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean fileExists(String path, boolean isDebug) {
        boolean rst = new File(path).exists();

        if (!rst && isDebug) {
            System.out.println("File doesnt exist... [" + path + "]");
        }

        return rst;
    }

    /*
     * Creates a new file at a specified location.
     */
    public static boolean createFile(String path) {
        try {
            File file = new File(path);
            return file.createNewFile();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return false;
    }

    /*
     * Reads the object content from a file, and later from the called method is 
     * casted to the correct type.
     */
    public static Object readObject(String path) {
        if (fileExists(path, false)) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
                Object obj = in.readObject();

                in.close();
                return obj;
            } catch (Exception e) {
                System.out.println("Error at file: " + path);
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    /*
     * Saves in textual file the content of enriched entities.
     */
    public static void saveAnnotationsAsText(HashMap<Integer, HashMap<String, String>> enrichedtokens, HashMap<Integer, HashMap<String, Set<String>>> tokens, String path) {
        StringBuffer sb = new StringBuffer();

        for (int k : enrichedtokens.keySet()) {
            HashMap<String, String> annotatedtokens = enrichedtokens.get(k);
            HashMap<String, Set<String>> subtokens = tokens.get(k);
            if (subtokens == null) {
                continue;
            }

            sb.append("\nTokens of size k: " + k + "\n");
            for (String token : annotatedtokens.keySet()) {
                Set<String> indexes = subtokens.get(token);
                if (indexes != null && indexes.size() >= 1) {
                    sb.append(token);
                    sb.append("\t");
                    sb.append(annotatedtokens.get(token));
                    sb.append("\n");
                }
            }
        }

        saveText(sb.toString(), path);
    }

    /*
     * Reads the textual contents from a file.
     * 
     */
    public static String readText(String path) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readText(String path, String utf) {
        try {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), utf));
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Reads the content of a file into a matrix.
     *
     * @param path
     * @param delim
     * @return
     */
    public static Map<String, Map<String, Double>> readMatrix(String path, String delim) {
        Map<String, Map<String, Double>> matrix = new TreeMap<String, Map<String, Double>>();
        String[] str_lines = FileUtils.readText(path).split("\n");
        for (String line : str_lines) {
            String[] data = line.split(delim);
            Map<String, Double> sub_matrix = matrix.get(data[0]);
            sub_matrix = sub_matrix == null ? new TreeMap<String, Double>() : sub_matrix;
            matrix.put(data[0], sub_matrix);

            sub_matrix.put(data[1], Double.parseDouble(data[2]));
        }
        return matrix;
    }
    /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */

    public static Set<String> readIntoSet(String path, String delim, boolean changeCase) {
        if (!FileUtils.fileExists(path, true)) {
            return null;
        }

        Set<String> rst = new HashSet<String>();

        String content = readText(path);
        String[] tmp = content.split(delim);
        for (String s : tmp) {
            if (changeCase) {
                rst.add(s.trim().toLowerCase());
            } else {
                rst.add(s);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, String> readIntoStringMap(String path, String delim, boolean changeCase, boolean ignoreSchema) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, String> rst = new TreeMap<String, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (ignoreSchema && tmp.length != 2) {
                rst.put(tmp[0].trim(), null);
            } else if (tmp.length == 2) {
                rst.put(tmp[0].trim(), tmp[1]);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, String> readIntoStringMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, String> rst = new TreeMap<String, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (tmp.length == 2) {
                rst.put(tmp[0].trim(), tmp[1]);
            }
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Double> readIntoStringDoubleMap(String path, String delim, boolean changeCase) {
        String[] lines = readText(path).split("\n");
        Map<String, Double> rst = new HashMap<String, Double>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            rst.put(tmp[0].trim(), Double.parseDouble(tmp[1]));
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Double> readTopicData(int startcolumnindex, String path, String delim, String filter, int level) {
        String[] lines = readText(path).split("\n");
        Map<String, Double> rst = new HashMap<String, Double>();

        for (String line : lines) {
            String[] tmp = line.split(delim);

            int col_index = startcolumnindex;
            String id = tmp[col_index];
            col_index++;

            Double value = Double.parseDouble(tmp[col_index]);
            col_index++;

            Integer cat_level = Integer.parseInt(tmp[col_index]);

            if (id.equals(filter) || (level != -1 && cat_level > level)) {
                continue;
            }

            rst.put(id, value);
        }
        Map<String, Double> sortedMap = sortByComparator(rst);
        return sortedMap;
    }

    public static Map sortByComparator(Map unsortMap) {
        List list = new LinkedList(unsortMap.entrySet());

        // sort list based on comparator
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                        .compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        // put sorted list into map again
        //LinkedHashMap make sure order in which keys were inserted
        Map sortedMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */

    public static Map<String, Double> readLDATopicData(int startcolumnindex, String path, String delim, String filter, int level) {
        String[] lines = readText(path).split("\n");
        Map<String, Double> rst = new HashMap<String, Double>();

        Map<String, List<Double>> rst_tmp = new HashMap<String, List<Double>>();
        for (String line : lines) {
            String[] tmp = line.split(delim);

            int col_index = startcolumnindex;
            String id = tmp[col_index];
            col_index++;

            Double value = Double.parseDouble(tmp[col_index]);
            col_index++;

            int cat_level = Integer.parseInt(tmp[col_index]);

            if (id.equals(filter) || (level != -1 && cat_level > level)) {
                continue;
            }

            List<Double> scores = rst_tmp.get(id);
            scores = scores == null ? new ArrayList<Double>() : scores;
            rst_tmp.put(id, scores);
            scores.add(value);
        }

        for (String id : rst_tmp.keySet()) {
            double avg = 0;
            for (double score : rst_tmp.get(id)) {
                avg += score;
            }

            rst.put(id, (avg / rst_tmp.get(id).size()));
        }
        Map<String, Double> sortedMap = sortByComparator(rst);
        return sortedMap;
    }

    public static Map<Integer, Map<String, Double>> readLDATopicDataSeparated(int startcolumnindex, String path, String delim, String filter, int level) {
        String[] lines = readText(path).split("\n");
        Map<Integer, Map<String, Double>> rst = new HashMap<Integer, Map<String, Double>>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            int col_index = startcolumnindex;

            int topicid = Integer.parseInt(tmp[col_index]);
            col_index++;
            String id = tmp[col_index];
            col_index++;
            Double score = Double.parseDouble(tmp[col_index]);
            col_index++;
            int cat_level = Integer.parseInt(tmp[col_index]);

            if (id.equals(filter) || (level != -1 && cat_level > level)) {
                continue;
            }

            Map<String, Double> sub_rst = rst.get(topicid);
            sub_rst = sub_rst == null ? new HashMap<String, Double>() : sub_rst;
            rst.put(topicid, sub_rst);

            sub_rst.put(id, score);
        }

        for (int topicid : rst.keySet()) {
            Map<String, Double> sortedMap = sortByComparator(rst.get(topicid));
            rst.put(topicid, sortedMap);
        }
        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<Double, Double> readIntoDoubleMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<Double, Double> rst = new TreeMap<Double, Double>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            rst.put(Double.parseDouble(tmp[0].trim()), Double.parseDouble(tmp[1]));
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Integer> readIntoStringIntMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, Integer> rst = new TreeMap<String, Integer>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            rst.put(tmp[0].trim(), Integer.valueOf(tmp[1]));
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, String> readIntoStringMap(String path, String delim, boolean changeCase, String filter) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, String> rst = new TreeMap<String, String>();

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (!filter.contains(tmp[1])) {
                continue;
            }

            rst.put(tmp[0].trim(), tmp[1]);
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Entry<String, String>> readIntoEntryMap(String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);
        Map<String, Entry<String, String>> rst = new TreeMap<String, Map.Entry<String, String>>();

        for (String line : lines) {

            String[] tmp = line.split(delim);
            if (tmp.length < 3) {
                continue;
            }

            Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(tmp[1], tmp[2]);
            rst.put(tmp[0], entry);
        }
        return rst;
    }

    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static void readIntoEntryMap1(Map<String, Map.Entry<String, String>> rst, String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);

        for (String line : lines) {
            String[] tmp = line.split(delim);
            if (tmp.length < 3) {
                continue;
            }

            Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(tmp[1], tmp[2]);
            rst.put(tmp[0], entry);

            System.out.println(rst.size() + "\t" + line);
        }
        System.out.println(rst.size());
    }


    /*
     * Reads the textual contents from a file into  a Map<String, String> split based on a specific delimeter.
     */
    public static Map<String, Entry<String, String>> readIntoEntryMap(Map<String, Entry<String, String>> rst, String path, String delim, boolean changeCase) {
        Set<String> lines = readIntoSet(path, "\n", changeCase);

        for (String line : lines) {

            String[] tmp = line.split(delim);
            if (tmp.length < 3) {
                continue;
            }

            Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(tmp[1], tmp[2]);
            rst.put(tmp[0], entry);
        }

        return rst;
    }

    /*
     * Reads the textual contents from a file into  a set split based on a specific delimeter.
     */
    public static Set<String> readIntoSetFileNames(String path) {
        Set<String> rst = new HashSet<String>();

        Set<String> tmp = new HashSet<String>();
        getFilesList(path, tmp);

        for (String file : tmp) {
            String s = file.substring(file.lastIndexOf("/") + 1).trim();
            rst.add(s);
        }

        return rst;
    }

    /*
     * Reads an xml file, retrieved as SPARQL resultset, and saves each of the
     * individual results into separate files.
     * 
     * The result set is as a tuple of <PaperURI, PaperContent>
     */
    public static void getTextualContent(String xmlfile, String savedir) {
        try {
            // save the mapping of file to resource mappings.
            Map<String, String> data = new TreeMap<String, String>();

            //Map<String, Metadata> lst = new TreeMap<String, Metadata>();

            // Read the xml file.
            File fXmlFile = new File(xmlfile);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("result");
            // if the xml file contains any data.
            if (nList.getLength() > 0) {
                // iterate over all resources and create a metadata object for
                // each particular resource.
                for (int i = 0; i < nList.getLength(); i++) {
                    Node n = nList.item(i);

                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        Element elNode = (Element) n;

                        // get the nodes that have the binding values
                        NodeList bindList = elNode.getChildNodes();
                        String paperuri = "", papercontent = "";
                        for (int j = 0; j < bindList.getLength(); j++) {
                            Node bind = bindList.item(j);

                            if (bind.getNodeType() == Node.ELEMENT_NODE) {
                                String nodename = bind.getAttributes().getNamedItem("name").getTextContent();

                                if (nodename.equals("paper")) {
                                    paperuri = bind.getTextContent();
                                } else if (nodename.equals("text")) {
                                    papercontent = bind.getTextContent();
                                }
                            }
                        }

                        data.put(paperuri, papercontent);
                    }
                }
            }

            // Save the content into files.
            int counter = 0;
            for (String paperuri : data.keySet()) {
                String papercontent = data.get(paperuri);
                FileUtils.saveText(papercontent, savedir + "/DOC-" + counter);
                counter++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Reads an xml file, retrieved as SPARQL resultset, and saves each of the
     * individual results into separate files.
     * 
     * The result set is as a tuple of <PaperURI, PaperContent>
     */
    public static void getTextualParagraphContent(String xmlfile, String savedir) {
        try {
            // save the mapping of file to resource mappings.
            Map<String, String> data = new TreeMap<String, String>();

            // Read the xml file.
            File fXmlFile = new File(xmlfile);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("result");
            // if the xml file contains any data.
            if (nList.getLength() > 0) {
                // iterate over all resources and create a metadata object for
                // each particular resource.
                for (int i = 0; i < nList.getLength(); i++) {
                    Node n = nList.item(i);

                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        Element elNode = (Element) n;

                        // get the nodes that have the binding values
                        NodeList bindList = elNode.getChildNodes();
                        String paperuri = "", papercontent = "";
                        for (int j = 0; j < bindList.getLength(); j++) {
                            Node bind = bindList.item(j);

                            if (bind.getNodeType() == Node.ELEMENT_NODE) {
                                String nodename = bind.getAttributes().getNamedItem("name").getTextContent();

                                if (nodename.equals("paper")) {
                                    paperuri = bind.getTextContent();
                                } else if (nodename.equals("text")) {
                                    papercontent = bind.getTextContent();
                                }
                            }
                        }

                        data.put(paperuri, papercontent);
                    }
                }
            }

            // Save the content into files.
            int counter = 0;
            for (String paperuri : data.keySet()) {
                String papercontent = data.get(paperuri);
                String[] paragraphs = papercontent.split("\\.\\s{1,}\\n");
                int prgcounter = 0;
                for (String paragraph : paragraphs) {
                    FileUtils.saveText(paragraph, savedir + "/DOC-" + counter + "-" + prgcounter);
                    prgcounter++;
                }

                counter++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * For each metadata description, its content is split into tokens of size
     * k, for further processing, and entity enrichment.
     */
    public static HashMap<String, Set<String>> getTokens(Map<String, String> data, int k) {
        HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();

        // for each of the metadata description, its content is split into
        // tokens of size k, and saved into the hashmap data structure.
        for (String docid : data.keySet()) {
            String description = data.get(docid);
            String[] tokens = StringUtils.split(description);

            int tokenIterator = 0;

            // buffer object for constructing tokens.
            StringBuffer sb = new StringBuffer();

            while (tokenIterator + k < tokens.length) {
                // clear previous buffer content.
                sb.delete(0, sb.length());

                // Counts the number of terms included in the token, this number
                // has to much with the required token size.
                int termCount = 0;

                for (int i = tokenIterator; i < tokenIterator + k; i++) {
                    String term = tokens[i];
                    term = term.toLowerCase();

                    if (!term.isEmpty()) {
                        sb.append(term);
                        sb.append(" ");

                        termCount++;
                    }
                }

                // if the number of included terms in the token matches the
                // required size of the token,
                // then we add this token.
                String tmp = sb.toString().trim();
                if (termCount == k && !tmp.isEmpty() && !tmp.endsWith("IN")) {
                    // Assign the token as a key in the data structure, and the
                    // meta document containing it, as a corresponding list of
                    // meta documents having this token.
                    Set<String> metatmp = map.get(sb.toString());

                    if (metatmp == null) {
                        metatmp = new HashSet<String>();
                        map.put(tmp, metatmp);
                    }

                    metatmp.add(docid);
                }

                // increase the token iterator by one, since we want to have all
                // possible combinations of tokens of size k.
                tokenIterator++;
            }
        }

        return map;
    }

    /*
     * Filters multiple annotated tokens of different size, by keeping the tokens with the biggest size.
     * E.g. Computer (k=1), and Computer Science (k=2), the token with size k=2 is kept, while the token with size k=1 is deleted.
     * Care must be taken, that we delete tokens only for those documents/metadata being on both indexes.
     */
    public static void filterTokenEnrichments(HashMap<Integer, HashMap<String, String>> annotations, HashMap<Integer, HashMap<String, Set<String>>> tokens) {
        for (int k = 1; k < annotations.size(); k++) {
            HashMap<String, String> annotationK = annotations.get(k);

            //compare the tokens of size k, with tokens of size k + i, where i = {k+1, ... n}.
            //for the token of size k, if it exists as a subset of token with size k+i, then we remove the token
            //from the data structure of tokens with size k.
            for (int i = k + 1; i < annotations.size(); i++) {
                HashMap<String, String> annotationI = annotations.get(i);
                if (annotationI != null) {
                    //for each of the tokens of size k+1, we check whether they contain tokens of size k.
                    //this is done by iterating over the tokens of size k+1, since they contain less tokens, and thus is less expensive.
                    //after partially matching tokens are found, we take the intersection of metadata containing the token of size k+1, and k, 
                    //and remove the enrichments for those documents for the token of size k.
                    for (String tokenK1 : annotationI.keySet()) {

                        //an iteration over the tokens of size k, is needed in order to do the partial matching.
                        for (String tokenK : annotationK.keySet()) {
                            if (tokenK1.contains(tokenK)) {
                                //remove the indices of metadata from the index datastructures of tokens with length k.
                                removeTokensFromIndex(tokenK, tokenK1, k, i, tokens);
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Removes the tokens from the token-document index structure, for those tokens being enriched at multiple datastructures for tokens of different size
     * where only the token with the largest size is kept.
     */
    private static void removeTokensFromIndex(String tokenK, String tokenK1, int k, int k1, HashMap<Integer, HashMap<String, Set<String>>> tokens) {
        //Get the tokens index data structure, for k, and k+1 (k1)
        HashMap<String, Set<String>> tokenIndexK = tokens.get(k);
        HashMap<String, Set<String>> tokenIndexK1 = tokens.get(k1);

        //get the list of metadata containing the tokens tokenK, and tokenK1.
        Set<String> indexK = tokenIndexK.get(tokenK);
        Set<String> indexK1 = tokenIndexK1.get(tokenK1);

        //remove the set of indices from the index data structure for tokens of size k, based on the intersection of indexK, and indexK1.
        if (indexK == null || indexK1 == null) {
            return;
        }

        Set<String> tmp = new HashSet<String>(indexK);
        //Now we remove the indices of the metadata, from the index datastructure of size k.
        indexK.removeAll(tmp);

        System.out.println("For token: [" + tokenK + "] of size k[" + k + "] removing indexes from metadata: " + tmp);
    }

    public static Set<String> loadSet(String path) {
        String content = readText(path);
        String[] lines = content.split("\n");

        Set<String> set = new HashSet<String>();

        for (String line : lines) {
            set.add(line.trim());
        }

        return set;
    }

    /**
     * Reads a text file into a map data structures. The structure is the
     * following item_i = x1,x2...
     *
     * @param path
     * @param delim
     * @param delim_1
     * @return
     */
    public static Map<String, List<String>> readMapList(String path, String delim, String delim_1) {
        String[] lines = readText(path).split("\n");
        Map<String, List<String>> map_list = new TreeMap<String, List<String>>();

        for (String line : lines) {
            String[] data = line.split(delim);

            List<String> list = new ArrayList<String>();
            map_list.put(data[0], list);

            String[] data_items = data[1].split(delim_1);
            for (String data_item : data_items) {
                list.add(data_item);
            }
        }
        return map_list;
    }
    /*
     * Checks whether a directory exists or not. If not, then it creates the directory.
     */

    public static void checkDir(String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public static boolean fileDirExists(String path) {
        File dir = new File(path);
        return dir.exists();
    }

    public static org.w3c.dom.Document readXMLDocument(String path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            String file_content = FileUtils.readText(path);
            file_content = StringEscapeUtils.unescapeHtml(file_content);
            file_content = file_content.replaceAll("&", "");

            InputSource is = new InputSource(new StringReader(file_content));
            org.w3c.dom.Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            return doc;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Write the text content to a file using the nio package
     *
     * @param text
     * @param out_file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveTextNIO(String text, String out_file) throws FileNotFoundException, IOException {
        try {
            RandomAccessFile aFile = new RandomAccessFile(out_file, "rw");
            FileChannel inChannel = aFile.getChannel();

            ByteBuffer buf = ByteBuffer.allocate(48);
            buf.clear();
            buf.put(text.getBytes());

            buf.flip();

            while (buf.hasRemaining()) {
                inChannel.write(buf);
            }

            inChannel.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
