import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class RstarTree {
    Node root;

    int height;
    int size;
    private static final double p = 0.3;
    private List<Boolean> overflowTreatmentCalled;
    private int level;

    public RstarTree(){
        this.root = new Node(true);

        overflowTreatmentCalled = new ArrayList<>();
        overflowTreatmentCalled.add(false);
        this.height = 1;
        this.size = 0;
        this.level = 0;
    }

    public void insert(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID);
        MBR mbr = new MBR(record.coordinates, record.coordinates);
        level = 0;

        for (int i = 0; i < overflowTreatmentCalled.size(); i++) {
            overflowTreatmentCalled.set(i, false);
        }
        insert(root, mbr, recordID);
    }

    private void reInsert(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID);
        MBR mbr = new MBR(record.coordinates, record.coordinates);
        level = 0;
        insert(root, mbr, recordID);
    }

    private void insert(Node node, MBR mbr, RecordID recordID) throws IOException {
        Node n = chooseLeaf(node, mbr);

        if (!n.isLeaf) {
            System.out.println("Node is not a leaf");
        }

        if (!n.isFull()) {
            n.addMBR(mbr);
            n.recordIDs.add(recordID);
        } else {
            if (n != root && overflowTreatmentCalled.get(level) == Boolean.FALSE) {
                overflowTreatmentCalled.set(level, true);
                reInsert(n, mbr, recordID);
            } else {
                split(n);
            }

        }
    }

    private Node chooseLeaf(Node node, MBR mbr) {
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
                for (int i = 0; i < expansions.size(); i++) {
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
                    level++;
                    return chooseLeaf(node.children.get(indexes.get(minAreaIndex)), mbr);
                } else {
                    level++;
                    return chooseLeaf(node.children.get(indexes.get(0)), mbr);
                }
            } else {
                level++;
                return chooseLeaf(node.children.get(minOverlapIndexes.get(0)), mbr);
            }
        }

        return node;
    }

    private void reInsert(Node node, MBR mbr, RecordID recordID) throws IOException {
        node.addMBR(mbr);
        node.recordIDs.add(recordID);
        node.updateMBR();

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
        p_entries = Math.min(p_entries, node.recordIDs.size() - Node.MIN_RECORD);


        List<RecordID> recordIDs = new ArrayList<>();
        for (int i = 0; i < p_entries; i++) {
            recordIDs.add(node.removeRecord(indexes.get(i)));
        }

        if (node.nodeMBR.area() == 0.0) {
            System.out.println("Warning: Node is empty after split or reinsert!");
        }

        for (int i = 0; i < p_entries; i++) {
            reInsert(recordIDs.get(i));
        }
    }

    private void split(Node node) throws IOException {
        List<MBR> mbrs;
        List<RecordID> records = null;
        List<Node> children = null;

        if (node.isLeaf) {
            records = new ArrayList<>(node.recordIDs);
            mbrs = new ArrayList<>(node.getMBRs());  // MBRs corresponding to each record
        } else {
            children = new ArrayList<>(node.children);
            mbrs = new ArrayList<>();
            for (Node child : children) {
                mbrs.add(child.nodeMBR);
            }
        }


        int bestAxis = chooseSplitAxis(mbrs);

        if (node.isLeaf) {
            sortByAxis(mbrs, records, bestAxis, true);
        } else {
            sortByAxis(mbrs, children, bestAxis);
        }

        int splitIndex = chooseSplitIndex(mbrs, Node.MIN_RECORD);


        Node left = new Node(node.isLeaf);
        Node right = new Node(node.isLeaf);

        if (node.isLeaf) {
            for (int i = 0; i < splitIndex; i++) {
                left.addMBR(mbrs.get(i));
                left.recordIDs.add(records.get(i));
            }

            for (int i = splitIndex; i < mbrs.size(); i++) {
                right.addMBR(mbrs.get(i));
                right.recordIDs.add(records.get(i));
            }
        } else {
            for (int i = 0; i < splitIndex; i++) {
                Node child = children.get(i);
                left.children.add(child);
                child.parent = left;
            }
            for (int i = splitIndex; i < children.size(); i++) {
                Node child = children.get(i);
                right.children.add(child);
                child.parent = right;
            }
        }

        left.updateMBR();
        right.updateMBR();

        if (node == root) {
            root = new Node(false);
            root.children.add(left);
            root.children.add(right);

            root.updateMBR();
            height++;
            overflowTreatmentCalled.add(false);

            left.parent = root;
            right.parent = root;
        } else {
            Node parent = node.parent;

            parent.children.removeIf(child -> child == node);

            parent.children.add(left);
            parent.children.add(right);

            left.parent = parent;
            right.parent = parent;

            parent.updateMBR();

            if (parent.isFull()) {
                split(parent); // recursively handle overflow
            }
        }

        if (node.nodeMBR.area() == 0.0) {
            System.out.println("Warning: Node is empty after split!");
        }

        if (node.isLeaf) {
            node.recordIDs.clear();
        } else {
            node.children.clear();
        }
    }

    private int chooseSplitAxis(List<MBR> mbrs) {
        int bestAxis = -1;
        double minSumMargin = Double.MAX_VALUE;

        for (int axis = 0; axis < Record.DIMENSIONS; axis++) {
            int finalAxis = axis;

            List<MBR> byLower = new ArrayList<>(mbrs);
            byLower.sort(Comparator.comparingDouble(m -> m.min[finalAxis]));

            List<MBR> byUpper = new ArrayList<>(mbrs);
            byUpper.sort(Comparator.comparingDouble(m -> m.max[finalAxis]));

            double marginSumLower = computeTotalMargin(byLower);
            double marginSumUpper = computeTotalMargin(byUpper);

            double totalMargin = marginSumLower + marginSumUpper;

            if (totalMargin < minSumMargin) {
                minSumMargin = totalMargin;
                bestAxis = axis;
            }
        }

        return bestAxis;
    }

    private double computeTotalMargin(List<MBR> mbrs) {
        int M = mbrs.size();
        int m = Node.MIN_RECORD;
        double total = 0.0;

        for (int k = 1; k <= M - 2 * m + 1; k++) {
            int split = m - 1 + k;
            List<MBR> group1 = mbrs.subList(0, split);
            List<MBR> group2 = mbrs.subList(split, M);

            MBR bbox1 = MBR.computeBoundingBox(group1);
            MBR bbox2 = MBR.computeBoundingBox(group2);

            total += bbox1.margin() + bbox2.margin();
        }

        return total;
    }

    private int chooseSplitIndex(List<MBR> sorted, int m) {
        int M = sorted.size();

        double minOverlap = Double.MAX_VALUE;
        double minArea =  Double.MAX_VALUE;
        int bestSplitIndex = -1;

        for (int k = 1; k <= M - 2 * m + 1; k++) {
            int split = m - 1 + k;

            List<MBR> group1 = sorted.subList(0, split);
            List<MBR> group2 = sorted.subList(split, M);

            MBR bbox1 = MBR.computeBoundingBox(group1);
            MBR bbox2 = MBR.computeBoundingBox(group2);

            double overlap = bbox1.overlap(bbox2);
            double area = bbox1.area() +  bbox2.area();

            if (overlap < minOverlap || (overlap == minOverlap && area < minArea)) {
                minOverlap = overlap;
                minArea = area;
                bestSplitIndex = split;
            }
        }

        return bestSplitIndex;
    }

    private void sortByAxis(List<MBR> mbrs, List<RecordID> recordIDs, int axis, boolean irrelevant) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < mbrs.size(); i++) indices.add(i);

        indices.sort(Comparator.comparingDouble(i -> mbrs.get(i).min[axis]));

        List<MBR> sortedMBRs = new ArrayList<>();
        List<RecordID> sortedIDs = new ArrayList<>();

        for (int i : indices) {
            sortedMBRs.add(mbrs.get(i));
            sortedIDs.add(recordIDs.get(i));
        }

        mbrs.clear();
        mbrs.addAll(sortedMBRs);
        recordIDs.clear();
        recordIDs.addAll(sortedIDs);
    }

    private void sortByAxis(List<MBR> mbrs, List<Node> children, int axis) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < mbrs.size(); i++) indices.add(i);

        indices.sort(Comparator.comparingDouble(i -> mbrs.get(i).min[axis]));

        List<MBR> sortedMBRs = new ArrayList<>();
        List<Node> sortedChildren = new ArrayList<>();

        for (int i : indices) {
            sortedMBRs.add(mbrs.get(i));
            sortedChildren.add(children.get(i));
        }

        mbrs.clear();
        mbrs.addAll(sortedMBRs);
        children.clear();
        children.addAll(sortedChildren);
    }

    public void printTree() throws IOException {
        System.out.println("Tree details: ");
        System.out.println("Height = " + height);
        printNode(root, 0);
    }

    private void printNode(Node node, int depth) throws IOException {
        String indent = "  ".repeat(depth);
        if (node == root) System.out.println(indent + "Root" + " Node");
        else System.out.println(indent + (node.isLeaf ? "Leaf" : "Internal") + " Node");

        System.out.println(indent + "  MBR: " + node.nodeMBR); // Optional

        if (node.isLeaf) {
            for (RecordID rid : node.recordIDs) {
                Record record = DataFileReader.getRecord(rid);
                System.out.println(indent + "    RecordID: " + record.id);
            }
        } else {
            for (Node child : node.children) {
                printNode(child, depth + 1);
            }
        }
    }

    private double calculateDistance(double[] r1, double[] r2) {
        double dx = r1[0] - r2[0];
        double dy = r1[1] - r2[1];
        return Math.sqrt(dx * dx + dy * dy);
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
                if (node.recordIDs.size() < Node.MIN_RECORD) {
                    // If underflow, remove node from parent and collect its records.
                    parent.children.remove(node);
                    orphanedRecords.addAll(node.recordIDs);
                } else {
                    node.updateMBR();
                }
            } else {
                if (node.children.size() < Node.MIN_RECORD) {
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

    // Range Query
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

    // K-NN Query

    public List<RecordID> knnQuery(int k, Node node, double[] queryPoint) throws IOException {
        if (k <= 0)
            throw new IllegalArgumentException("Parameter 'k' must be a positive integer.");

        PriorityQueue<DistanceRecord> knn = new PriorityQueue<>(k, (a, b) -> Double.compare(b.distance, a.distance));

        knnSearchRecursive(node, queryPoint, k, knn);

        List<RecordID> result = new ArrayList<>();
        for (DistanceRecord entry : knn) {
            result.add(entry.recordID);
        }

        // Sort from nearest to furthest points
        result.sort(Comparator.comparingDouble(
        rid -> {
            try {
                return calculateDistance(queryPoint, DataFileReader.getRecord(rid).coordinates);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    ));

        return result;
    }

    private void knnSearchRecursive(Node node, double[] queryPoint, int k, PriorityQueue<DistanceRecord> knn) throws IOException {
        if (node.isLeaf) {
            for (int i = 0; i < node.recordIDs.size(); i++) {
                RecordID rid = node.recordIDs.get(i);
                Record record = DataFileReader.getRecord(rid);
                double distance = calculateDistance(queryPoint, record.coordinates);

                if (knn.size() < k) {
                    knn.add(new DistanceRecord(rid, distance));
                } else if (distance < knn.peek().distance) {
                    knn.poll(); // remove worst
                    knn.add(new DistanceRecord(rid, distance));
                }
            }
        } else {
            List<Node> sortedChildren = new ArrayList<>(node.children);
            sortedChildren.sort(Comparator.comparingDouble(child -> child.nodeMBR.minDistance(queryPoint)));

            for (Node child : sortedChildren) {
                double minDist = child.nodeMBR.minDistance(queryPoint);
                if (knn.size() == k && minDist > knn.peek().distance) {
                    continue;
                }
                knnSearchRecursive(child, queryPoint, k, knn);
            }
        }
    }

    // Skyline Query

}