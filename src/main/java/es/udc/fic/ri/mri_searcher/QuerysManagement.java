package es.udc.fic.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static es.udc.fic.ri.mri_searcher.CACMEval.fileToBuffer;

public class QuerysManagement {
    private List<List<String>> list;
    private Path docDir;

    public QuerysManagement(String docsPath) throws IOException {
        this.docDir = Paths.get(docsPath);
        try (InputStream stream = Files.newInputStream(docDir)) {
            this.list = CACMParser.parseQuerys(fileToBuffer(stream));
        }
    }

    public List<String> getParsedQuery(int n){
        return list.get(n);
    }

    public static void main(String[] args){
        String path = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\docs\\query.text";
        try {
            QuerysManagement qm = new QuerysManagement(path);
            qm.getParsedQuery(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
