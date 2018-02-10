package client.library;



import java.util.ArrayList;

public class Batch{

    ArrayList<RequestObject> contentOfBatch;

    public Batch(ArrayList<RequestObject> contentOfBatch) {
        this.contentOfBatch = contentOfBatch;
    }

}