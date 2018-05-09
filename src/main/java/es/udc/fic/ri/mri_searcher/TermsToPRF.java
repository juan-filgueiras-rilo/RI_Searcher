package es.udc.fic.ri.mri_searcher;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TermsToPRF {
    private TopDocs td;
    private int nMaxDocs;
    private int nMaxTerms;
    private String indexPath;
    private List<TermScore> termsList;
    private List<String> finalTermsList;

    public TermsToPRF(TopDocs td, int nMaxDocs, int nMaxTerms, String indexPath) {
        this.td = td;
        this.nMaxDocs = nMaxDocs;
        this.nMaxTerms = nMaxTerms;
        this.indexPath = indexPath;
        this.termsList = new ArrayList<>();
        this.finalTermsList = new ArrayList<>();
    }

    public void computeTerms() {
        int i;
        Directory dir = null;
        String[] fields = {"W", "T"};
        try {
            dir = FSDirectory.open(Paths.get(indexPath));
            DirectoryReader indexReader = DirectoryReader.open(dir);
            ScoreDoc[] scoreDocs = td.scoreDocs;
            DocList docList = new DocList();
            if (nMaxDocs > td.totalHits){
                System.out.println("Solo se han encontrado " + scoreDocs.length + " documentos, aunque el parámetro -prs tiene valor " + nMaxDocs +
                                ". Por lo que solo se usarán estos para el PRF.");
                nMaxDocs = scoreDocs.length;
            }
            for(i=0;i<nMaxDocs;i++) {
                ScoreDoc doc = scoreDocs[i];
                docList.addDoc(doc.doc,doc.score);
            }
            for (String fieldName: fields) {
	            Terms terms = MultiFields.getTerms(indexReader, fieldName);
	            TermsEnum termsEnum = terms.iterator();
	            while ((termsEnum.next() != null)) {
	                final String tt = termsEnum.term().utf8ToString();
	                final PostingsEnum postings = MultiFields.getTermPositionsEnum(indexReader, fieldName, new Term(fieldName, tt).bytes(), PostingsEnum.ALL);
	                int whereDoc;
	                Integer index = null;
	                
	                while((whereDoc = postings.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
	                    if((index = docList.findByDocId(whereDoc)) != null) {
	                    	Boolean encontrado = false;
	                    	for (TermScore t: termsList){
	                            if (t.getTermString().equals(tt) && !encontrado){
	                                t.addTf(postings.freq(), docList.getScorebyIndex(index));
	                                encontrado = true;
	                                break;
	                            }
	                        }
	                        if (!encontrado){
	                            termsList.add(new TermScore(tt, postings.freq(), docList.getScorebyIndex(index), termsEnum.docFreq(), indexReader.numDocs()));
	                        }
	                    }
	                    postings.nextDoc();
	                }
	            }
            }
            for (TermScore termScore:termsList){
                termScore.computeScore();
            }
            termsList.sort(TermScore::compareTo);
            System.out.println("Lista de términos con los que se expandirá la query:");
            for(int j=0;j<termsList.size();j++){
                if (j>=this.nMaxTerms){
                    break;
                }
                TermScore termScore = termsList.get(j);
                String termString = termScore.getTermString();
                finalTermsList.add(termString);
                System.out.println("Término nº " + (j+1) + ": " + termString + " - Score: " + termScore.getScore());
                termScore.explainScore();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTermsToExpandQuery() {
        return finalTermsList;
    }

    private class DocList{
        private List<Integer> IDList;
        private List<Float> scoreList;
        private Integer idMax;

        public DocList(){
            IDList = new ArrayList<>();
            scoreList = new ArrayList<>();
            idMax = null;
        }

        public Float getScorebyIndex(int i){
            return scoreList.get(i);
        }

        public void addDoc(Integer id, Float score){
            if (idMax == null){
                idMax = id;
            } else if (idMax<id){
                idMax = id;
            }
            IDList.add(id);
            scoreList.add(score);
        }

        public Integer findByDocId(int id){
            for(int i=0;i<IDList.size();i++){
                if (IDList.get(i).equals(id)){
                    return i;
                }
            }
            return null;
        }
    }

    private class TermScore implements Comparable<TermScore>{
        private float score;
        private String t;
        private double idf;
        private List<Integer> tf;
        private List<Float> docScore;


        public TermScore(String t, int tf, float docScore, int df, int n) {
            this.t = t;
            this.tf = new ArrayList<>();
            this.docScore = new ArrayList<>();
            this.tf.add(tf);
            this.docScore.add(docScore);
            this.idf = Math.log(n/df);

        }

        public float getScore() {
            return score;
        }

        public void computeScore(){
            score = 0;
            for(int i=0; i<tf.size();i++){
                score += tf.get(i) * idf * docScore.get(i);
            }
        }

        public void addTf(int tf, Float docScore){
            this.tf.add(tf);
            this.docScore.add(docScore);
        }

        public String getTermString(){
            return this.t;
        }

        public void explainScore(){
            for(int i=0; i<tf.size();i++){
                System.out.println( "( " +tf.get(i) + " x " + idf + " x " + docScore.get(i) + " )");
                if ((i+1)<tf.size()){
                    System.out.println(" + ");
                }
            }
            System.out.println("-------------------------------------------------------------");
        }

        @Override
        public int compareTo(TermScore o) {
            return Float.compare(o.getScore(),this.score);
        }
    }
}
