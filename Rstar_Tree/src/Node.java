import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Node {
    boolean isLeaf;                  
    List<Node> children;
    MBR nodeMBR;
    Node parent;
    List<RecordID> recordIDs; // Only if isLeaf = True

    public static final int maxRecord = 50;
    int minRecord;

    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        if (isLeaf) {
            recordIDs = new ArrayList<>();
        } else {
            children = new ArrayList<>();
        }
        nodeMBR = null;
        parent = null;

        this.minRecord = (int) Math.ceil(0.4*maxRecord);
    }

    public boolean isFull() {
        if (isLeaf) {
            return recordIDs.size() >= maxRecord;
        } else {
            return children.size() >= maxRecord;
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

    public void addMBR(MBR mbr) {
        nodeMBR = nodeMBR.expandToInclude(mbr);
    }

    public double computeOverlap(double[] point) {
        MBR mbr = new MBR(point, point);
        return nodeMBR.overlap(mbr);
    }


    public double computeExpansion(double[] point) {
        double originalArea = nodeMBR.area();
        MBR expanded = nodeMBR.expandToInclude(point);
        double expandedArea = expanded.area();
        return expandedArea - originalArea;
    }
}


