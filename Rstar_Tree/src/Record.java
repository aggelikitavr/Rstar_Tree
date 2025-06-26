public class Record {
    public long id;
    public String name;
    public double[] coordinates;

    // Bytes
    public static final int NAME_SIZE = 64;
    public static final int DIMENSIONS = 2;
    public static final int RECORD_SIZE = Long.BYTES + NAME_SIZE + Double.BYTES * DIMENSIONS;

    public Record(long id, String name, double[] coordinates) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        return "Record{id=" + id + ", name='" + name + "', coordinates=(" + coordinates[0] + ", " + coordinates[1] + ")}\n";
    }
}
