import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Node {
    boolean isLeaf;                  
    List<Node> children;
    MBR nodeMBR;
    Node parent;
    List<RecordID> recordIDs; // Only if isLeaf = True

    public static final int MAX_RECORD = 4;
    public static final int MIN_RECORD = (int) Math.ceil(0.4* MAX_RECORD);

    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        if (isLeaf) {
            recordIDs = new ArrayList<>();
        } else {
            children = new ArrayList<>();
        }
        nodeMBR = null;
        parent = null;
    }

    public boolean isFull() {
        if (isLeaf) {
            return recordIDs.size() >= MAX_RECORD;
        } else {
            return children.size() >= MAX_RECORD;
        }
    }

    public int size() {
        return isLeaf ? recordIDs.size() : children.size();
    }

    public void updateMBR() throws IOException {
        if (isLeaf && recordIDs != null && !recordIDs.isEmpty()) {
            RecordID recordID = recordIDs.get(0);
            Record record = DataFileReader.getRecord(recordID);

            MBR mbr = new MBR(record.coordinates, record.coordinates);
            for (RecordID id : recordIDs) {
                Record r = DataFileReader.getRecord(id);
                mbr = mbr.expandToInclude(r.coordinates);
            }
            nodeMBR = mbr;
        } else if (!isLeaf && children != null && !children.isEmpty()) {
            MBR mbr = children.get(0).nodeMBR;
            for (Node child : children) {
                mbr = mbr.expandToInclude(child.nodeMBR);
            }
            nodeMBR = mbr;
        }
    }

    public void addMBR(MBR mbr) throws IOException {
        if (nodeMBR == null) {
            nodeMBR = mbr;
            return;
        }
        nodeMBR = nodeMBR.expandToInclude(mbr);
        updateMBR();
    }

    public RecordID removeRecord(int index) throws IOException {
        RecordID recordID = recordIDs.get(index);
        recordIDs.remove(index);
        updateMBR();
        return recordID;
    }

    public double computeOverlap(MBR mbr) {
        return nodeMBR.overlap(mbr);
    }

    public List<MBR> getMBRs() throws IOException {
        List<MBR> mbrs = new ArrayList<>();
        for (int i = 0; i < recordIDs.size(); i++) {
            double[] coordinates = DataFileReader.getRecord(recordIDs.get(i)).coordinates;
            mbrs.add(new MBR(coordinates, coordinates));
        }

        return mbrs;
    }

    public double computeExpansion(MBR mbr) {
        double originalArea = nodeMBR.area();
        MBR expanded = nodeMBR.expandToInclude(mbr);
        double expandedArea = expanded.area();
        return expandedArea - originalArea;
    }
}


