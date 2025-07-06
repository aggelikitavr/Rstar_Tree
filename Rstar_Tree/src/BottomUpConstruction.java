import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BottomUpConstruction {
    List<RecordID> recordIDs;
    List<Record> records;
    

    public BottomUpConstruction(List<RecordID> recordIDs) throws IOException {
        this.recordIDs = recordIDs;
        this.records = new ArrayList<>();
        for (RecordID id : recordIDs) {
            records.add(DataFileReader.getRecord(id));
        }
    }

    public List<Node> createLeafNodes(List<List<RecordID>> groups) throws IOException{
        List<Node> leafNodes = new ArrayList<>();

        for(List<RecordID> group : groups){
            Node leaf = new Node(false);
            leaf.isLeaf = true;
            leaf.recordIDs = group;

            // Υπολογισμός MBR από τα records
            List<Record> records = new ArrayList<>();
            for (RecordID rid : group) {
                records.add(DataFileReader.getRecord(rid));
        }

        leaf.nodeMBR = MBR.fromRecords(records);
        leafNodes.add(leaf);
        }
        return leafNodes;
    }

    public Node buildRStarTree(List<Node> currentLevelNodes) {
        if (currentLevelNodes.size() == 1) {
            return currentLevelNodes.get(0); // Root node
        }

        List<Node> nextLevelNodes = new ArrayList<>();

        // STR-like grouping: sort by X center
        currentLevelNodes.sort(Comparator.comparingDouble(n -> n.nodeMBR.getCenter()[0]));

        int maxChildren = Node.MAX_RECORD;
        for (int i = 0; i < currentLevelNodes.size(); i += maxChildren) {
            int end = Math.min(i + maxChildren, currentLevelNodes.size());
            List<Node> group = currentLevelNodes.subList(i, end);

            Node parent = new Node(false);
            parent.children = new ArrayList<>(group);
            parent.nodeMBR = MBR.fromNodes(group);

            nextLevelNodes.add(parent);
        }

        return buildRStarTree(nextLevelNodes); // Αναδρομικά μέχρι να φτιάξεις root
    }

    public List<List<RecordID>> sortTileRecursive() throws IOException {
        List<List<RecordID>> groups = new ArrayList<>();

        int leaf_size = (int) Math.ceil((double) recordIDs.size() / Node.MAX_RECORD);
        int num_strips = (int) Math.ceil(Math.sqrt(leaf_size));
        int records_per_strip = (int) Math.ceil((double) recordIDs.size() / num_strips);

        // Βήμα 1: Συνδυασμός Record με RecordID
        List<SimpleEntry<Record, RecordID>> entries = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            entries.add(new SimpleEntry<>(records.get(i), recordIDs.get(i)));
        }

        // Ταξινόμηση κατά X
        entries.sort(Comparator.comparingDouble(e -> e.getKey().coordinates[0]));

        // Βήμα 2: Διαίρεση σε strips
        for (int i = 0; i < num_strips; i++) {
            int startStrip = i * records_per_strip;
            int endStrip = Math.min(startStrip + records_per_strip, entries.size());

            List<SimpleEntry<Record, RecordID>> strip = entries.subList(startStrip, endStrip);

            // Ταξινόμηση κατά Y
            strip.sort(Comparator.comparingDouble(e -> e.getKey().coordinates[1]));

            // Βήμα 3: Ομαδοποίηση MAX_RECORD
            for (int j = 0; j < strip.size(); j += Node.MAX_RECORD) {
                int endGroup = Math.min(j + Node.MAX_RECORD, strip.size());
                List<SimpleEntry<Record, RecordID>> group = strip.subList(j, endGroup);

                // Πάρε μόνο τα RecordID
                List<RecordID> groupIDs = group.stream()
                    .map(SimpleEntry::getValue)
                    .collect(Collectors.toList());

                groups.add(groupIDs);
            }
        }

        return groups;
    }



}


