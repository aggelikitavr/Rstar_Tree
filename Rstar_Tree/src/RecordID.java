public class RecordID {
    public int blockID;
    public int slotID;

    public RecordID(int blockID, int slotID) {
        this.blockID = blockID;
        this.slotID = slotID;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecordID other)) return false;
        return this.blockID == other.blockID && this.slotID == other.slotID;
    }

    @Override
    public int hashCode() {
        return 31 * blockID + slotID;
    }
}
