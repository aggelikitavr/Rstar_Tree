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

    public void delete(RecordID recordID) throws IOException {
        Record record = DataFileReader.getRecord(recordID);
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
        if (!node.isLeaf) {
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

    private void condenseTree(Node leaf) throws IOException {
        Node N = leaf;
        List<Node> Q = new ArrayList<>();

        while (N != root) {
            Node P = N.parent;

            if (N.isLeaf) {
                if (N.recordIDs.size() < N.minRecord) {
                    P.children.remove(N);
                    Q.add(N);
                } else {
                    P.updateMBR();
                }
            } else {
                if (N.children.size() < N.minRecord) {
                    P.children.remove(N);
                    Q.add(N);
                } else {
                    P.updateMBR();
                }
            }
            N = P;
        }

        if (!root.isLeaf && root.children.size() == 1) {
            root = root.children.get(0);
            root.parent = null;
        }

        for (Node eliminatedNode : Q) {
            reInsertSubtree(eliminatedNode);
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

}
