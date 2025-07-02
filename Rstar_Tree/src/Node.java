import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Node {
    boolean isLeaf;                  
//    List<Record> records;
    List<Node> children;
    MBR nodeMBR;
    Node parent;
    List<RecordID> recordIDs; // Only if isLeaf = True

    public static final int maxRecord = 50;
    int minRecord;

    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        if (isLeaf) {
            //records = new ArrayList<>();
            recordIDs = new ArrayList<>();
        } else {
            children = new ArrayList<>();
        }
        nodeMBR = null;
        parent = null;

        this.minRecord = (int) Math.ceil(0.4*maxRecord);
    }

    public boolean isFull(int maxEntries) {
        if (isLeaf) {
            return recordIDs.size() >= maxEntries;
        } else {
            return children.size() >= maxEntries;
        }
    }

    public int size() {
        return isLeaf ? recordIDs.size() : children.size();
    }

    public void updateMBR() throws IOException {
        if (isLeaf && recordIDs != null && !recordIDs.isEmpty()) {
            RecordID recordID = recordIDs.get(0);
            Record record = getRecord(recordID);

            MBR mbr = new MBR(record.coordinates, record.coordinates);
            for (RecordID id : recordIDs) {
                Record r = getRecord(id);
                mbr = mbr.expandToInclude(new MBR(r.coordinates, r.coordinates));
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

    private Record getRecord(RecordID recordID) throws IOException {
        DataFileReader reader = new DataFileReader("datafile.bin");

        return reader.readRecord(recordID.blockID, recordID.slotID);
    }
}


