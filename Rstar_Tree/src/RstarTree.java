public class RstarTree {
    Node root;
    int maxRecord; // per Node
    int minRecord; // per Node

    int height;
    int size;

    public RstarTree(int maxRecord){
        this.root = new Node(true);
        this.maxRecord = maxRecord;
        this.minRecord = (int) Math.ceil(0.4*maxRecord);
        
        this.height = 1;
        this.size = 0;
    }

    public void insert(Record record){
        
    }

}
