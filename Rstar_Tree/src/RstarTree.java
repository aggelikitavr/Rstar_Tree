import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RstarTree {
    Node root;

    int height;
    int size;
    private static final double p = 0.3;

    public RstarTree(){
        this.root = new Node(true);
        
        this.height = 1;
        this.size = 0;
    }

    public void insert(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID);
        MBR mbr = new MBR(record.coordinates, record.coordinates);
        insert(root, mbr, recordID);
    }

    public void insert(Node node, MBR mbr, RecordID recordID) throws IOException {
        Node n = chooseLeaf(node, mbr);

        if (!n.isFull()) {
            n.addMBR(mbr);
            n.recordIDs.add(recordID);
        } else {
            if (node != root && !node.reInserted) {
                reInsert(node, mbr, recordID);
                node.reInserted = true;
            } else {
                //split
            }

            node.reInserted = false;
        }
    }

    private Node chooseLeaf(Node node, MBR mbr) throws IOException {
        if (!node.isLeaf) {
            // Find all the children with the least overlap
            List<Double> overlaps = new ArrayList<>();
            for (int i = 0; i < node.children.size(); i++) {
                Node f = node.children.get(i);
                double overlap = f.computeOverlap(mbr);
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
                    double expansion = f.computeExpansion(mbr);
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
                    chooseLeaf(node.children.get(indexes.get(minAreaIndex)), mbr);
                } else {
                    chooseLeaf(node.children.get(indexes.getFirst()), mbr);
                }
            } else {
                chooseLeaf(node.children.get(minOverlapIndexes.getFirst()), mbr);
            }
        }

        return node;
    }

    private void reInsert(Node node, MBR mbr, RecordID recordID) throws IOException {
        node.addMBR(mbr);
        node.recordIDs.add(recordID);

        double[] center = node.nodeMBR.getCenter();

        List<Double> distances = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < node.recordIDs.size(); i++) {
            RecordID currentRecord = node.recordIDs.get(i);
            double[] currentCoordinates = DataFileReader.getRecord(currentRecord).coordinates;

            distances.add(calculateDistance(center, currentCoordinates));
            indexes.add(i);
        }

        distances.sort(Comparator.reverseOrder());
        indexes.sort((i2, i1) -> Double.compare(distances.get(i2), distances.get(i1)));
        int p_entries = (int) Math.ceil(distances.size() * p);

        List<RecordID> recordIDs = new ArrayList<>();
        for (int i = 0; i < p_entries; i++) {
            recordIDs.add(node.removeRecord(indexes.get(i)));
        }

        for (int i = 0; i < p_entries; i++) {
            insert(recordIDs.get(i));
        }
    }

    private static double calculateDistance(double[] center, double[] point) {
        if (center.length != point.length) {
            throw new IllegalArgumentException("Points must have the same dimension");
        }

        double sum = 0.0;
        for (int i = 0; i < center.length; i++) {
            double diff = center[i] - point[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // Method that deletes a record from the R* Tree
    public void delete(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID); // Finds the record with this recordID
        Node leaf = findLeaf(root, record);

        if (leaf != null) {
            boolean removed = leaf.recordIDs.remove(recordID);

            if (removed) {
                leaf.updateMBR();
                condenseTree(leaf);
            } else {
                System.out.println("RecordID not found in leaf.");
            }
        } else {
            System.out.println("Record not found in the tree.");
        }
    }

    private Node findLeaf(Node node, Record record) throws IOException {
        if (!node.isLeaf) { // If the node is not a leaf, check for each one of the children if there is leaf
            for (Node child : node.children) {
                if (child.nodeMBR.contains(record.coordinates)) {
                    Node leaf = findLeaf(child, record);
                    if (leaf != null) return leaf;
                }
            }
        } else {
            for (RecordID id : node.recordIDs) {
                    Record r = DataFileReader.getRecord(id);
                    if (r.id == record.id) {
                        return node;
                    }
            }
        }
        return null;
    }

    private void condenseTree(Node node) throws IOException {
        List<RecordID> orphanedRecords = new ArrayList<>();
        List<Node> orphanedSubtrees = new ArrayList<>();

        while (node != root) {
            Node parent = node.parent;

            if (node.isLeaf) {
                if (node.recordIDs.size() < node.minRecord) {
                    // If underflow, remove node from parent and collect its records.
                    parent.children.remove(node);
                    orphanedRecords.addAll(node.recordIDs);
                } else {
                    node.updateMBR();
                }
            } else {
                if (node.children.size() < node.minRecord) {
                    // If underflow, remove node from parent and collect its children.
                    parent.children.remove(node);
                    orphanedSubtrees.addAll(node.children);
                } else {
                    node.updateMBR();
                }
            }

            node = parent;
        }

        // If root has only one child and is not a leaf, promote it
        if (!root.isLeaf && root.children.size() == 1) {
            root = root.children.get(0);
            root.parent = null;
        }

        // Reinsert orphaned leaf records
        for (RecordID orphaned : orphanedRecords) {
            insert(orphaned);
        }

        // Reinsert orphaned subtrees (from internal nodes)
        for (Node orphanedNode : orphanedSubtrees) {
            reInsertSubtree(orphanedNode);
        }

        root.updateMBR();
    }

    private void reInsertSubtree(Node node) throws IOException {
        if (node.isLeaf) {
            for (RecordID recordID : node.recordIDs) {
                insert(recordID);
            }
        } else {
            for (Node child : node.children) {
                reInsertSubtree(child);
            }
        }
    }

    // Queries

    public List<RecordID> rangeQuery(double[] min, double[] max) throws IOException {
        MBR queryMBR = new MBR(min, max);
        List<RecordID> result = new ArrayList<>();
        rangeQueryRecursive(root, queryMBR, result);
        return result;
    }

    private void rangeQueryRecursive(Node node, MBR queryMBR, List<RecordID> result) throws IOException {
        if (!node.nodeMBR.intersects(queryMBR)) return;

        if (node.isLeaf) {
            for (RecordID id : node.recordIDs) {
                Record record = DataFileReader.getRecord(id);

                boolean inside = true;
                for (int i = 0; i < record.coordinates.length; i++) {
                    if (record.coordinates[i] < queryMBR.min[i] || record.coordinates[i] > queryMBR.max[i]) {
                        inside = false;
                        break;
                    }
                }

                if (inside) {
                    result.add(id);
                }
            }
        } else {
            for (Node child : node.children) {
                rangeQueryRecursive(child, queryMBR, result);
            }
        }
    }

    


}
