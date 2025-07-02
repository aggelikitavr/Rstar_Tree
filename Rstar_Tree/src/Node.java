import java.util.ArrayList;
import java.util.List;

public class Node {
    boolean isLeaf;                  
    List<Record> records;          
    List<Node> children;            
    MBR nodeMBR;                    
    Node parent;                   

    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        if (isLeaf) {
            records = new ArrayList<>();
        } else {
            children = new ArrayList<>();
        }
        nodeMBR = null;
        parent = null;
    }

    public boolean isFull(int maxEntries) {
        if (isLeaf) {
            return records.size() >= maxEntries;
        } else {
            return children.size() >= maxEntries;
        }
    }

    public int size() {
        return isLeaf ? records.size() : children.size();
    }

    public void updateMBR() {
        if (isLeaf && records != null && !records.isEmpty()) {
            MBR mbr = new MBR(records.get(0).coordinates, records.get(0).coordinates);
            for (Record r : records) {
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
}


