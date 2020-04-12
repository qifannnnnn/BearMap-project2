package bearmaps.proj2c;

import bearmaps.hw4.streetmap.Node;
import bearmaps.hw4.streetmap.StreetMapGraph;
import bearmaps.proj2ab.MyTrieSet;
import bearmaps.proj2ab.Point;
import bearmaps.hw4.WeirdSolver;
import bearmaps.proj2ab.PointSet;
import bearmaps.proj2ab.WeirdPointSet;


import java.util.*;

public class AugmentedStreetMapGraph extends StreetMapGraph {
    // from uncleaned to cleaned
    Map<String,String> map_name=new HashMap<>();
    Map<Point,Node> map=new HashMap<>();
    WeirdPointSet kdTree;
    MyTrieSet trie=new MyTrieSet();

    public AugmentedStreetMapGraph(String dbPath) {
        super(dbPath);
        // You might find it helpful to uncomment the line below:
        List<Node> nodes = this.getNodes();
        List<Point> points=new ArrayList<>();
        // mapping between points and nodes
        for (Node i:nodes){
                String uncleaned = i.name();
            if (!(uncleaned==null)){

                String cleaned = cleanString(uncleaned);

                if (!(cleaned.length()==0)){
                    map_name.put(cleaned, uncleaned);
                    // System.out.println(cleaned);
                    trie.add(cleaned);
                }
            }

            if (neighbors(i.id()).size()>0)
            {
                map.put(new Point(i.lon(), i.lat()), i);
                points.add(new Point(i.lon(), i.lat()));
            }
        }

        // construct a kdtree
        kdTree=new WeirdPointSet(points);
    }


    /**
    return the id
     */
    public long closest(double lon, double lat) {
        Point ans=kdTree.nearest(lon,lat);
        Node result=map.get(ans);
        return result.id();
    }


    /**
     * For Project Part III (gold points)
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        List<String> result=new ArrayList<>();
        prefix=cleanString(prefix);
        for (String i:trie.keysWithPrefix(prefix)){
            result.add(map_name.get(i));
        }
        return result;
    }

    /**
     * For Project Part III (gold points)
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public List<Map<String, Object>> getLocations(String locationName) {
        return new LinkedList<>();
    }


    /**
     * Useful for Part III. Do not modify.
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }



}
