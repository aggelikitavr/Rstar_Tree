import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataFileWriter {
    public static final int BLOCK_SIZE = 32 * 1024;

    public static void writeToFile(List<Record> records, String filename) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(filename);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            ByteBuffer bb = ByteBuffer.allocate(BLOCK_SIZE);
            int count = 0;

            for (Record record : records) {
                if (bb.remaining() < Record.RECORD_SIZE) {
                    bos.write(bb.array());
                    //bb.clear();
                    bb = ByteBuffer.allocate(BLOCK_SIZE); // Δημιουργία νέου, καθαρού buffer
                }

                writeRecordToBuffer(record, bb);
                count++;
            }

            if (bb.position() > 0) {
                bos.write(bb.array());
            }

            System.out.println("Wrote " + count + " records.");
        }
    }

    // Packs a record into the Buffer
    private static void writeRecordToBuffer(Record record, ByteBuffer bb) {
        System.out.println("Writing ID: " + record.id);
        bb.putLong(record.id);

        byte[] nameBytes = new byte[Record.NAME_SIZE];
        //byte[] inputBytes = record.name.getBytes(StandardCharsets.UTF_8);
        byte[] inputBytes = record.name.getBytes(Charset.forName("ISO-8859-7"));


        System.arraycopy(inputBytes, 0, nameBytes, 0, Math.min(inputBytes.length, Record.NAME_SIZE));

        bb.put(nameBytes);

        for (int i = 0; i < Record.DIMENSIONS; i++) {
            bb.putDouble(record.coordinates[i]);
        }
    }
}
