import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        OSMParser parser = new OSMParser();
        parser.parseToRecords("map.osm");

        DataFileWriter.writeToFile(parser.getRecords(), "datafile.bin");

        DataFileReader reader = new DataFileReader("datafile.bin");
        System.out.println(reader.readBlock(0).toString());
        System.out.println(reader.getTotalBlocks());
    }
}