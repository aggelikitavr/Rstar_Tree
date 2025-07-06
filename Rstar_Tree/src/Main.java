import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        timers();
    }

    public static void realData() throws IOException {
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
        tree.printTree();
        tree.updateTreeInFile("tree.txt");

        RstarTree newTree = new RstarTree("tree.txt");
        newTree.printTree();
    }

    public static void citiesData() throws IOException {
        List<Record> records = Arrays.asList(
                new Record(1, "Αθήνα", new double[]{10, 20}),
                new Record(2, "Θεσσαλονίκη", new double[]{15, 25}),
                new Record(3, "Πάτρα", new double[]{30, 10}),
                new Record(4, "Ηράκλειο", new double[]{40, 12}),
                new Record(5, "Λάρισα", new double[]{12, 32}),
                new Record(6, "Βόλος", new double[]{19, 24}),
                new Record(7, "Ιωάννινα", new double[]{28, 18}),
                new Record(8, "Χανιά", new double[]{35, 30}),
                new Record(9, "Καλαμάτα", new double[]{22, 14}),
                new Record(10, "Ξάνθη", new double[]{13, 37}),
                new Record(11, "Κομοτηνή", new double[]{25, 40}),
                new Record(12, "Σέρρες", new double[]{16, 33}),
                new Record(13, "Ρέθυμνο", new double[]{42, 28}),
                new Record(14, "Αλεξανδρούπολη", new double[]{31, 23}),
                new Record(15, "Κέρκυρα", new double[]{11, 17}),
                new Record(16, "Χαλκίδα", new double[]{26, 21}),
                new Record(17, "Καβάλα", new double[]{17, 27}),
                new Record(18, "Άρτα", new double[]{18, 19}),
                new Record(19, "Τρίκαλα", new double[]{29, 36}),
                new Record(20, "Σπάρτη", new double[]{33, 13}),
                new Record(21, "Λαμία", new double[]{20, 20}),
                new Record(22, "Κατερίνη", new double[]{24, 26}),
                new Record(23, "Πτολεμαΐδα", new double[]{27, 29}),
                new Record(24, "Γιάννενα", new double[]{30, 18}),
                new Record(25, "Ναύπλιο", new double[]{21, 15}),
                new Record(26, "Ζάκυνθος", new double[]{32, 16}),
                new Record(27, "Καλαμπάκα", new double[]{34, 11}),
                new Record(28, "Ρόδος", new double[]{45, 35}),
                new Record(29, "Κως", new double[]{46, 38}),
                new Record(30, "Λέσβος", new double[]{41, 34}),
                new Record(31, "Μυτιλήνη", new double[]{38, 39}),
                new Record(32, "Χίος", new double[]{43, 31}),
                new Record(33, "Σάμος", new double[]{39, 20}),
                new Record(34, "Λήμνος", new double[]{44, 25}),
                new Record(35, "Άργος", new double[]{36, 22}),
                new Record(36, "Κόρινθος", new double[]{37, 19}),
                new Record(37, "Θήβα", new double[]{23, 34}),
                new Record(38, "Άμφισσα", new double[]{28, 27}),
                new Record(39, "Έδεσσα", new double[]{14, 16}),
                new Record(40, "Γιαννιτσά", new double[]{13, 29}),
                new Record(41, "Νάουσα", new double[]{22, 30}),
                new Record(42, "Αίγιο", new double[]{31, 26}),
                new Record(43, "Μεσολόγγι", new double[]{20, 31}),
                new Record(44, "Λευκάδα", new double[]{26, 17}),
                new Record(45, "Πρέβεζα", new double[]{19, 22}),
                new Record(46, "Αγρίνιο", new double[]{25, 28}),
                new Record(47, "Κατερίνη", new double[]{27, 32}),
                new Record(48, "Πύργος", new double[]{33, 15}),
                new Record(49, "Καρδίτσα", new double[]{29, 24}),
                new Record(50, "Καστοριά", new double[]{18, 21})
        );

        DataFileWriter.writeToFile(records, "datafile.bin");

        DataFileReader reader = new DataFileReader("datafile.bin");
        System.out.println(reader.readBlock(0).toString());
        System.out.println(reader.getTotalBlocks());

        RstarTree tree = new RstarTree();
        for (int blockNum = 0; blockNum < reader.getTotalBlocks(); blockNum++) {
            for (int i = 0; i < reader.getRecordCountForBlock(blockNum); i++) {
                tree.insert(new RecordID(blockNum, i));
            }
        }

        System.out.println("----------------------------------------");
        tree.printTree();
        RTreePlotter.plotTree(tree, "rstar_output.png", 800, 800);

        //RecordID cityToDelete = new RecordID(0, 2);
        //System.out.println("Deleting city with ID = " + DataFileReader.getRecord(cityToDelete).id + " and name = " + DataFileReader.getRecord(cityToDelete).name);
        //tree.delete(cityToDelete);

        tree.printTree();
        tree.updateTreeInFile("tree.txt");

        RstarTree tree2 = new RstarTree("tree.txt");
        tree2.printTree();

        // double[] min = {20, 20};
        // double[] max = {30, 30};
        // System.out.println("Range query result in area: {" + min[0] + ", " + min[1] + "} " +
        //         "{" + max[0] + ", " + max[1] + "}");
        // for (RecordID recordID : tree.rangeQuery(min, max)) {
        //     System.out.println(DataFileReader.getRecord(recordID).id);
        // }

        System.out.println(" ");

        List<RecordID> queryResult;

        // int k = 5;
        // queryResult = tree.knnQuery(k, new double[]{11, 20});
        // System.out.println("Knn query result with k = " + k);
        // for (RecordID recordID : queryResult) {
        //     System.out.println(DataFileReader.getRecord(recordID).id);
        // }
        // queryResult.clear();

        System.out.println(" ");

        queryResult = tree.skylineQuery();
        System.out.println("Skyline query result: ");
        for (RecordID recordID : queryResult) {
            System.out.println(DataFileReader.getRecord(recordID).id);
        }

        ArrayList<RecordID> recordIDs = new ArrayList<>();
        for (int blockNum = 0; blockNum < reader.getTotalBlocks(); blockNum++) {
            for (int i = 0; i < reader.getRecordCountForBlock(blockNum); i++) {
                recordIDs.add(new RecordID(blockNum, i));
            }
        }

        // 2. Φτιάξε τον constructor
        BottomUpConstruction builder = new BottomUpConstruction(recordIDs);

        // 3. Εφάρμοσε Sort-Tile-Recursive
        List<List<RecordID>> groups = builder.sortTileRecursive();

        // 4. Δημιούργησε φύλλα
        List<Node> leafNodes = builder.createLeafNodes(groups);

        // 5. Χτίσε το R*-Tree
        Node root = builder.buildRStarTree(leafNodes);

        // 6. (Προαιρετικό) Εκτύπωσε τη ρίζα ή πληροφορίες
        System.out.println("R*-Tree root MBR: " + root.nodeMBR);
        System.out.println("------------------------------------------");
        tree.printTree();
    }

    public static void timers() throws IOException {
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
        startTime = System.nanoTime();
        double[] min = {20, 20};
        double[] max = {70, 70};
//        System.out.println("Range query result in area: {" + min[0] + ", " + min[1] + "} " +
//                "{" + max[0] + ", " + max[1] + "}");
//        queryResult = tree.rangeQuery(min, max);
//        for (RecordID recordID : queryResult) {
//            System.out.println(DataFileReader.getRecord(recordID).id);
//        }
        endTime = System.nanoTime();
        System.out.println("Time for range query: " + (endTime - startTime) / 1000000.0 + "ms");

        // Serial Range Query
        startTime  = System.nanoTime();
        MBR mbr = new MBR(min, max);
        queryResult = serialRangeQuery(mbr, recordIDs);
//        for (RecordID recordID : queryResult) {
//            System.out.println(DataFileReader.getRecord(recordID).id);
//        }
        endTime = System.nanoTime();
        System.out.println("Time for serial range query: " + (endTime - startTime) / 1000000.0 + "ms");


        // Tree k-nn Query with k = 5
        double[] queryPoint =  {42, 25};
        startTime = System.nanoTime();
        int k = 5;
        queryResult = tree.knnQuery(k, queryPoint);
//        System.out.println("Knn query result with k = " + k);
//        for (RecordID recordID : queryResult) {
//            System.out.println(DataFileReader.getRecord(recordID).id);
//        }
        queryResult.clear();
        endTime = System.nanoTime();
        System.out.println("Time for k-nn query with k =  " + k + ": " + (endTime - startTime) / 1000000.0 + "ms");

        // Tree k-nn Query with k = 40
        startTime = System.nanoTime();
        k = 40;
        queryResult = tree.knnQuery(k, queryPoint);
        queryResult.clear();
        endTime = System.nanoTime();
        System.out.println("Time for k-nn query with k = " + k + ": " + (endTime - startTime) / 1000000.0 + "ms");

        // Serial k-nn query with k = 40
        startTime  = System.nanoTime();
        queryResult = serialKNearestNeighbors(queryPoint, recordIDs, k);
//        for (RecordID recordID : queryResult) {
//            System.out.println(DataFileReader.getRecord(recordID).id);
//        }
        endTime = System.nanoTime();
        System.out.println("Time for serial k-nn query with k = " +  k + ": " + (endTime - startTime) / 1000000.0 + "ms");

        // Tree Skyleine Query
        startTime = System.nanoTime();
        queryResult = tree.skylineQuery();
//        System.out.println("Skyline query result: ");
//        for (RecordID recordID : queryResult) {
//            System.out.println(DataFileReader.getRecord(recordID).id);
//        }
        endTime = System.nanoTime();
        System.out.println("Time for skyline query: " + (endTime - startTime) / 1000000.0 + "ms");
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