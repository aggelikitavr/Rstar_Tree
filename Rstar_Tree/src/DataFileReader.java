import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataFileReader {
    public static final int BLOCK_SIZE = 32 * 1024;
    private final RandomAccessFile raf;

    public DataFileReader(String filename) throws IOException {
        this.raf = new RandomAccessFile(filename, "r");
    }

    // Read all the records from a block
    public List<Record> readBlock(int blockId) throws IOException {
        long offset = (long) blockId * BLOCK_SIZE;
        raf.seek(offset);

        byte[] blockData = new byte[BLOCK_SIZE];
        raf.readFully(blockData);

        ByteBuffer buffer = ByteBuffer.wrap(blockData);
        List<Record> records = new ArrayList<Record>();

        while (buffer.remaining() >= Record.RECORD_SIZE) {
            records.add(readRecordFromBuffer(buffer));
        }

        return records;
    }

    // Unpacks the record from the file
    private Record readRecordFromBuffer(ByteBuffer buffer) {
        long id = buffer.getLong();

        byte[] nameBytes = new byte[Record.NAME_SIZE];
        buffer.get(nameBytes);
        String name = new String(nameBytes, Charset.forName("ISO-8859-7")).trim();

        double[] coordinates = new double[Record.DIMENSIONS];
        for (int i = 0; i < Record.DIMENSIONS; i++) {
            coordinates[i] = buffer.getDouble();
        }

        return new Record(id, name, coordinates);
    }

    // Reads a specific record from the datafile
    public Record readRecord(int blockId, int slot) throws IOException {
        long offset = (long) blockId * BLOCK_SIZE + (long) slot * Record.RECORD_SIZE;
        raf.seek(offset);

        byte[] recordData = new byte[Record.RECORD_SIZE];
        raf.readFully(recordData);

        ByteBuffer buffer = ByteBuffer.wrap(recordData);

        return readRecordFromBuffer(buffer);
    }

    public int getTotalBlocks() throws IOException {
        long fileSize = raf.length(); // σε bytes
        return (int) Math.ceil((double) fileSize / DataFileReader.BLOCK_SIZE);
    }

    public void close() throws IOException {
        raf.close();
    }
}
