package es.udc.fic.ri.mri_searcher;

import java.util.ArrayList;
import java.util.List;

public class QueryType {
    private int id;
    private String body;
    private String author;
    private String info;
    private List<Integer> relDocs;

    public QueryType(int id) {
        this.id = id;
        relDocs = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public List<Integer> getRelDocs() {
        return relDocs;
    }

    public void addRelDoc(int relDocId){
        this.relDocs.add(relDocId);
    }

    public void setRelDocs(List<Integer> relDocs) {
        this.relDocs = relDocs;
    }

    public Boolean isRelevant(int relDocId){
        Boolean result = false;
        for (Integer i: this.relDocs){
            if (i==relDocId) result=true;
        }
        return result;
    }
}
