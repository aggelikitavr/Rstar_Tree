import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BottomUp {
    List<RecordID> recordIDs;
    List<Record> records;
    

    public BottomUp(List<RecordID> recordIDs) throws IOException {
        this.recordIDs = recordIDs;
        this.records = new ArrayList<>();
        for (RecordID id : recordIDs) {
            records.add(DataFileReader.getRecord(id));
        }
    }

    public void sortTileRecursive() throws IOException {
        List<List<RecordID>> groups = new ArrayList<>();
        int leaf_size = (int) Math.ceil((double) recordIDs.size() / Node.MAX_RECORD); // Number of records that can fit in a leaf
        int num_strips = (int) Math.ceil(Math.sqrt(leaf_size)); // Number of vertical strips
        int records_per_strip = (int) Math.ceil((double) recordIDs.size() / num_strips);
        
        // Step 1: Ταξινομημένα records ως προς X
        List<Record> sortedByX = new ArrayList<>(records);
        sortedByX.sort(Comparator.comparingDouble(r -> r.coordinates[0]));

        // Step 2: Κόψιμο σε κάθετες λωρίδες (strips)
        for (int i = 0; i < num_strips; i++) {
            int startStrip = i * records_per_strip;
            int endStrip = Math.min(startStrip + records_per_strip, sortedByX.size());

            List<Record> strip = sortedByX.subList(startStrip, endStrip);

            // Step 3: Ταξινόμηση της λωρίδας ως προς Y
            strip.sort(Comparator.comparingDouble(r -> r.coordinates[1]));

            // Step 4: Κόψιμο σε ομάδες μεγέθους MAX_RECORD
            for (int j = 0; j < strip.size(); j += Node.MAX_RECORD) {
                int endGroup = Math.min(j + Node.MAX_RECORD, strip.size());
                List<Record> group = strip.subList(j, endGroup);

                // Μετατροπή Record -> RecordID
                List<RecordID> groupIDs = group.stream()
                    .map(r -> new RecordID(r.blockID, r.slotID)) // Αν έτσι δημιουργείς RecordID
                    .collect(Collectors.toList());

                groups.add(groupIDs); // Προσθήκη στο σύνολο των ομάδων
            }
        }
    }

    public void 


}


