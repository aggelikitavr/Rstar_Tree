import java.util.ArrayList;

abstract class Queries {
    // Returns the IDs of the query records
    abstract ArrayList<Long> getQueryRecordIds(Node node);
}