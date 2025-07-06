import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // Parse all the data from the file to the DataFileWriter to store them
        OSMParser parser = new OSMParser();
        parser.parseToRecords("map.osm");

        DataFileWriter.writeToFile(parser.getRecords(), "datafile.bin");
        DataFileReader reader = new DataFileReader("datafile.bin");

        ArrayList<RecordID> recordIDs = new ArrayList<>();

        // Individual insert
        long startTime = System.nanoTime();
        RstarTree tree = new RstarTree();
        for (int block = 0;  block < reader.getTotalBlocks(); block++) {
            for (int slot = 0;  slot < reader.getRecordCountForBlock(block); slot++) {
                tree.insert(new RecordID(block, slot));
                recordIDs.add(new RecordID(block, slot));

            }
        }
        tree.printTree();
        long endTime = System.nanoTime();
        System.out.println("Time to build the tree with individual inserts: " + (endTime - startTime) / 1000000.0 + "ms");

        tree.updateTreeInFile("tree.txt");


        // Bottom-up build
        startTime = System.nanoTime();
        BottomUpConstruction builder = new BottomUpConstruction(recordIDs);
        List<List<RecordID>> groups = builder.sortTileRecursive();
        List<Node> leafNodes = builder.createLeafNodes(groups);
        Node root = builder.buildRStarTree(leafNodes);
        endTime = System.nanoTime();
        System.out.println("Time to build the tree with Bottom-up: " +  (endTime - startTime) / 1000000.0 + "ms");

        List<RecordID> queryResult;

        // Tree Range Query
        double[] min = {40, 22};
        double[] max = {41.661, 22.966};
        runRangeQuery(min, max, tree, recordIDs, true);

        // Serial Range Query
        runRangeQuery(min, max, tree, recordIDs, false);

        // Tree k-nn Query with k = 5
        double[] queryPoint =  {42, 25};
        runKNNQuery(5, tree, queryPoint, recordIDs, true);

        // Tree k-nn Query with k = 10
        runKNNQuery(10, tree, queryPoint, recordIDs, true);

        // Tree k-nn Query with k = 20
        runKNNQuery(20, tree, queryPoint, recordIDs, true);

        // Serial k-nn query with k = 5
        runKNNQuery(5, tree, queryPoint, recordIDs, false);

        // Serial k-nn query with k = 10
        runKNNQuery(10, tree, queryPoint, recordIDs, false);

        // Serial k-nn query with k = 20
        runKNNQuery(20, tree, queryPoint, recordIDs, false);

        // Tree Skyline Query
        startTime = System.nanoTime();
        queryResult = tree.skylineQuery();
        System.out.println("Skyline query result: ");
        for (RecordID recordID : queryResult) {
            System.out.println(DataFileReader.getRecord(recordID).id);
        }
        endTime = System.nanoTime();
        System.out.println("Time for skyline query: " + (endTime - startTime) / 1000000.0 + "ms");

        RTreePlotter.plotTree(tree, "rstar_output.png", 800, 800);
    }

    public static void runRangeQuery(double[] min, double[] max, RstarTree tree, List<RecordID> ids,  boolean isTree) throws IOException {
        long startTime = System.nanoTime();

        System.out.println("Range query result in area: {" + min[0] + ", " + min[1] + "} " +
                "{" + max[0] + ", " + max[1] + "}");
        List<RecordID> queryResult = new ArrayList<>();
        if (isTree) {
            System.out.println("Tree Range Query result: ");
            queryResult = tree.rangeQuery(min, max);
        } else {
            System.out.println("Serial Range Query result: ");
            queryResult = serialRangeQuery(new MBR(min, max), ids);
        }

        for (RecordID recordID : queryResult) {
            System.out.println(DataFileReader.getRecord(recordID).id);
        }
        long endTime = System.nanoTime();
        System.out.println("Time for range query: " + (endTime - startTime) / 1000000.0 + "ms");
    }

    public static void runKNNQuery(int k, RstarTree tree, double[]queryPoint, List<RecordID> ids, boolean isTree) throws IOException {
        long startTime = System.nanoTime();
        List<RecordID> queryResult = new ArrayList<>();

        if (isTree) {
            System.out.println("Tree Knn query result with k = " + k);
            queryResult = tree.knnQuery(k, queryPoint);
        } else {
            System.out.println("Serial KNN query result with k = " + k);
            queryResult = serialKNearestNeighbors(queryPoint, ids, k);
        }


        for (RecordID recordID : queryResult) {
            System.out.println(DataFileReader.getRecord(recordID).id);
        }
        queryResult.clear();
        long endTime = System.nanoTime();

        if (isTree) System.out.println("Time for Tree KNN query with k = " + k + ": " + (endTime - startTime) / 1000000.0 + "ms");
        else System.out.println("Time for Serial KNN query with k = " + k + ": " + (endTime - startTime) / 1000000.0 + "ms");
    }

    public static List<RecordID> serialRangeQuery(MBR query, List<RecordID> ids) throws IOException {
        List<RecordID> results = new ArrayList<>();
        for (RecordID rid : ids) {
            Record rec = DataFileReader.getRecord(rid);
            if (query.contains(rec.coordinates)) {
                results.add(rid);
            }
        }
        return results;
    }

    public static List<RecordID> serialKNearestNeighbors(double[] query, List<RecordID> ids, int k) throws IOException {
        List<RecordID> allRecords = new ArrayList<>();

        for (RecordID rid : ids) {
            Record rec = DataFileReader.getRecord(rid);
            allRecords.add(rid);
        }

        // Sort the records by distance to the query point
        allRecords.sort(Comparator.comparingDouble(r -> {
            try {
                return euclidean(query, DataFileReader.getRecord(r).coordinates);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        // Return the first k (or fewer if not enough records)
        return allRecords.subList(0, Math.min(k, allRecords.size()));
    }

    private static double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }


}