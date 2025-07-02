import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RstarTree {
    Node root;

    int height;
    int size;

    public RstarTree(){
        this.root = new Node(true);
        
        this.height = 1;
        this.size = 0;
    }

    public void insert(RecordID recordID){
        
    }

    public void insert(Node node, RecordID recordID) throws IOException {
        Node n = chooseLeaf(node, recordID);

        if (!n.isFull()) {
            Record record = DataFileReader.getRecord(recordID);
            n.addMBR(new MBR(record.coordinates, record.coordinates));
            n.recordIDs.add(recordID);
        } else {
            //split();
        }
    }

    private Node chooseLeaf(Node node, RecordID recordID) throws IOException {
        if (!node.isLeaf) {
            // Find all the children with the least overlap
            List<Double> overlaps = new ArrayList<>();
            for (int i = 0; i < node.children.size(); i++) {
                Node f = node.children.get(i);
                Record record = DataFileReader.getRecord(recordID);
                double overlap = f.computeOverlap(record.coordinates);
                overlaps.add(overlap);
            }

            double min = Collections.min(overlaps);

            List<Integer> minOverlapIndexes = new ArrayList<>();
            for (int i = 0; i < overlaps.size(); i++) {
                if (overlaps.get(i) == min) {
                    minOverlapIndexes.add(i);
                }
            }

            // If there is a tie then find the least expansion
            if (minOverlapIndexes.size() > 1) {
                List<Double> expansions = new ArrayList<>();
                for (int j = 0; j < minOverlapIndexes.size(); j++) {
                    Node f = node.children.get(minOverlapIndexes.get(j));
                    Record record = DataFileReader.getRecord(recordID);
                    double expansion = f.computeExpansion(record.coordinates);
                    expansions.add(expansion);
                }

                double minExpansion = Collections.min(expansions);

                List<Integer> indexes = new ArrayList<>();
                for (int i = 0; i < minExpansion; i++) {
                    if (expansions.get(i) == minExpansion) {
                        indexes.add(i);
                    }
                }

                // If there is a tie then get the smallest area
                if (indexes.size() > 1) {
                    double minArea = Double.MAX_VALUE;
                    int minAreaIndex = -1;
                    for (int j = 0; j < indexes.size(); j++) {
                        Node f = node.children.get(indexes.get(j));
                        double area = f.nodeMBR.area();
                        if (area < minArea) {
                            minArea = area;
                            minAreaIndex = j;
                        }
                    }
                    chooseLeaf(node.children.get(indexes.get(minAreaIndex)), recordID);
                } else {
                    chooseLeaf(node.children.get(indexes.get(0)), recordID);
                }
            } else {
                chooseLeaf(node.children.get(minOverlapIndexes.get(0)), recordID);
            }
        }

        return node;
    }

}
