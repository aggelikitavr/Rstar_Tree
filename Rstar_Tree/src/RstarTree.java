import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RstarTree {
    Node root;

    int height;
    private static final double p = 0.3; // How many records are going to be removed in the reinsert (30%)
    private List<Boolean> overflowTreatmentCalled;
    private int level;

    // Constructs a tree from scratch
    public RstarTree(){
        this.root = new Node(true);

        overflowTreatmentCalled = new ArrayList<>();
        overflowTreatmentCalled.add(false);
        this.height = 1;
        this.level = 0;
    }

    // Constructs the tree from the index file
    public RstarTree(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        Stack<NodeWrapper> stack = new Stack<>();

        // Get the height
        line = br.readLine();
        if (line != null && line.startsWith("Height:")) {
            height = Integer.parseInt(line.substring("Height:".length()).trim());
        } else {
            throw new IOException("Expected Height line at top of file.");
        }

        // Check every line with 4 possible results:
        // - Root
        // - Node[isLeaf=
        // - Record
        // - Child --> In this situation we just want to continue reading
        while ((line = br.readLine()) != null) {
            int indent = countIndentation(line);
            line = line.trim();

            if (line.equals("Root")) {
                Node root = new Node(false);
                this.root = root;

                stack.push(new NodeWrapper(root, indent));

            } else if (line.startsWith("Node[isLeaf=")) {
                boolean isLeaf = line.contains("true");
                Node node = new Node(isLeaf);

                while (!stack.isEmpty() && stack.peek().indent >= indent) {
                    stack.pop();
                }

                if (!stack.isEmpty()) {
                    Node parent = stack.peek().node;
                    parent.children.add(node);
                }

                stack.push(new NodeWrapper(node, indent));
            } else if (line.startsWith("Record")) {
                Matcher matcher = Pattern.compile("blockID=(\\d+), slotID=(\\d+)").matcher(line);

                if (matcher.find()) {
                    int blockID = Integer.parseInt(matcher.group(1));
                    int slotID = Integer.parseInt(matcher.group(2));

                    RecordID recordID = new RecordID(blockID, slotID);
                    double[] coordinates = DataFileReader.getRecord(recordID).coordinates;
                    MBR mbr = new MBR(coordinates, coordinates);

                    Node node = stack.peek().node;
                    if (node.recordIDs == null) node.recordIDs = new ArrayList<>();
                    node.recordIDs.add(recordID);
                    node.addMBR(mbr);
                    node.isLeaf = true;
                }
            }
        }

        updateAllMBRs();
        br.close();
    }

    private int countIndentation(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }

        return count;
    }

    public void updateAllMBRs() throws IOException {
        updateMBRsRecursive(this.root);
    }

    private void updateMBRsRecursive(Node node) throws IOException {
        if (!node.isLeaf) {
            for (Node child : node.children) {
                updateMBRsRecursive(child); // update child first
            }
        }
        node.updateMBR(); // then update current node
    }

    public void insert(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID);

        // Check for empty records
        if (record == null || record.id == 0) {
            return;
        }
        MBR mbr = new MBR(record.coordinates, record.coordinates);
        level = 0;

        for (int i = 0; i < overflowTreatmentCalled.size(); i++) {
            overflowTreatmentCalled.set(i, false);
        }
        insert(root, mbr, recordID);
    }

    // This re-insert method is being used to re-insert an individual record and not called when OverflowTreament is necessary
    private void reInsert(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID);
        
        if (record == null || record.id == 0) {
            return;
        }
        
        MBR mbr = new MBR(record.coordinates, record.coordinates);
        level = 0;
        insert(root, mbr, recordID);
    }

    private void insert(Node node, MBR mbr, RecordID recordID) throws IOException {
        // Find a suitable leaf to assign this record
        Node n = chooseLeaf(node, mbr);

        // Check for errors
        if (!n.isLeaf) {
            System.out.println("Node is not a leaf");
        }

        // If the leaf is full then call reInsert before calling split
        if (!n.isFull()) {
            n.addMBR(mbr);
            n.recordIDs.add(recordID);
        } else {
            if (n != root && overflowTreatmentCalled.get(level) == Boolean.FALSE) {
                overflowTreatmentCalled.set(level, true);
                reInsert(n, mbr, recordID);
            } else {
                n.addMBR(mbr);
                n.recordIDs.add(recordID);
                split(n);
            }
        }
    }

    // Recursive function that will return a leaf based on minimum overlap first, minimum expansion second and third minimum area
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

    // This reInsert method is being called before we split a node to see if a better adjustment of the records can be made among
    // the leaves
    private void reInsert(Node node, MBR mbr, RecordID recordID) throws IOException {
        // Add the new record to the node first
        node.addMBR(mbr);
        node.recordIDs.add(recordID);
        node.updateMBR();

        // Get the center of the mbr and calculate the distances of each record in the node from the center
        double[] center = node.nodeMBR.getCenter();

        List<Double> distances = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < node.recordIDs.size(); i++) {
            RecordID currentRecord = node.recordIDs.get(i);
            double[] currentCoordinates = DataFileReader.getRecord(currentRecord).coordinates;

            distances.add(calculateDistance(center, currentCoordinates));
            indexes.add(i);
        }

        // Sort the distances in reverse order
        distances.sort(Comparator.reverseOrder());
        indexes.sort((i2, i1) -> Double.compare(distances.get(i2), distances.get(i1)));


        // Get the p first records and remove them from the node
        int p_entries = (int) Math.ceil(distances.size() * p);
        p_entries = Math.min(p_entries, node.recordIDs.size() - Node.MIN_RECORD);

        List<RecordID> recordIDs = new ArrayList<>();
        for (int i = 0; i < p_entries; i++) {
            recordIDs.add(node.removeRecord(indexes.get(i)));
        }

        // Insert each of the remove records in the tree
        for (int i = 0; i < p_entries; i++) {
            reInsert(recordIDs.get(i));
        }
    }

    // Split a node in two (left and right nodes) and assigning the initial records to them based on the best axis and index
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
        double sum = 0;
        for (int i = 0; i < r1.length; i++) {
            double d = r1[i] - r2[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    // Method that deletes a record from the R* Tree
    public void delete(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID); // Finds the record with this recordID
        Node leaf = findLeaf(root, record);

        System.out.println("Node: " + leaf.nodeMBR);
        //if (leaf != null) {
            boolean removed = leaf.recordIDs.remove(recordID);

            if (removed) {
                leaf.updateMBR();
                condenseTree(leaf);
                System.out.println("RecordID was deleted.");
            } else {
                System.out.println("RecordID not found in leaf.");
            }
        //} else {
        //    System.out.println("Record not found in the tree.");
        //}
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
                for (int i = 0; i < Record.DIMENSIONS; i++) {
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

    public List<RecordID> knnQuery(int k, double[] queryPoint) throws IOException {
        if (k <= 0)
            throw new IllegalArgumentException("Parameter 'k' must be a positive integer.");

        PriorityQueue<DistanceRecord> knn = new PriorityQueue<>(k, (a, b) -> Double.compare(b.distance, a.distance));

        knnSearchRecursive(root, queryPoint, k, knn);

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
    public List<RecordID> skylineQuery() throws IOException {
        List<RecordID> skyline = new ArrayList<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(n -> n.nodeMBR.minCoordSum()));
        queue.add(root);

        while (!queue.isEmpty()) {
            Node node = queue.poll();

            if (node.isLeaf) {
                for (int i = 0; i < node.recordIDs.size(); i++) {
                    RecordID rid = node.recordIDs.get(i);
                    Record rec = DataFileReader.getRecord(rid);
                    if (!isDominated(rid, skyline)) {
                        skyline.removeIf(other -> {
                            try {
                                return dominates(rec.coordinates, DataFileReader.getRecord(other).coordinates);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        skyline.add(rid);
                    }
                }
            } else {
                for (Node child : node.children) {
                    if (!isCompletelyDominated(child.nodeMBR, skyline)) {
                        queue.add(child);
                    }
                }
            }
        }

        return skyline;
    }

    private boolean dominates(double[] a, double[] b) {
        boolean dominates = false;
        for (int i = 0; i < Record.DIMENSIONS; i++) {
            if (a[i] > b[i]) return false;
            if (a[i] < b[i]) dominates = true;
        }

        return dominates;
    }

    private boolean isDominated(RecordID rec, List<RecordID> skyline) throws IOException {
        for (RecordID other : skyline) {
            if (dominates(DataFileReader.getRecord(other).coordinates, DataFileReader.getRecord(rec).coordinates)) return true;
        }
        return false;
    }

    private boolean isCompletelyDominated(MBR mbr, List<RecordID> skyline) throws IOException {
        // Use MBR.min as a representative corner; optionally test more corners
        for (RecordID rid : skyline) {
            Record rec = DataFileReader.getRecord(rid);
            boolean dominates = true;
            for (int i = 0; i < rec.coordinates.length; i++) {
                if (rec.coordinates[i] > mbr.min[i]) {
                    dominates = false;
                    break;
                }
            }
            if (dominates) return true;
        }
        return false;
    }

    // Write tree to an index file
    public void updateTreeInFile(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Height: " + this.height + "\n");
            writeNodeRecursive(writer, root, 0);
            writeNodeRecursive(writer, root, 0);
        }
    }

    private void writeNodeRecursive(FileWriter writer, Node node, int indent) throws IOException {
        String prefix = "  ".repeat(indent);
        if (node == root) writer.write(prefix + "Root\n");
        else writer.write(prefix + "Node[isLeaf=" + node.isLeaf + "]\n");

        if (node.isLeaf) {
            for (int i = 0; i < node.recordIDs.size(); i++) {
                RecordID rid = node.recordIDs.get(i);
                writer.write(prefix + "  Record: blockID=" + rid.blockID + ", slotID=" + rid.slotID + "\n");
            }
        } else {
            for (int i = 0; i < node.children.size(); i++) {
                writer.write(prefix + "  Child:\n");
                writeNodeRecursive(writer, node.children.get(i), indent + 2);
            }
        }
    }
}

