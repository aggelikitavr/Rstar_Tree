import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        OSMParser parser = new OSMParser();
        parser.parseToRecords("map.osm");

        DataFileWriter.writeToFile(parser.getRecords(), "datafile.bin");

        DataFileReader reader = new DataFileReader("datafile.bin");
        System.out.println(reader.readBlock(0).toString());
        System.out.println(reader.getTotalBlocks());

        RstarTree tree = new RstarTree();
        for (int i = 0;  i < reader.getRecordCountForBlock(0); i++) {
            tree.insert(new RecordID(0, i));
        }
        for (int i = 0;  i < reader.getRecordCountForBlock(1); i++) {
            tree.insert(new RecordID(1, i));
        }
        //tree.printTree();
    }
}