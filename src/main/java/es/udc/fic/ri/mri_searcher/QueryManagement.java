package es.udc.fic.ri.mri_searcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static es.udc.fic.ri.mri_searcher.CACMEval.fileToBuffer;

public class QueryManagement {
	private List<QueryType> list;
	private Path queryDir;
	private Path relsDir;

	public QueryManagement(String queryPath, String relsPath) {
		this.queryDir = Paths.get(queryPath);
		this.relsDir = Paths.get(relsPath);
		try (InputStream querysStream = Files.newInputStream(queryDir)) {
			this.list = CACMParser.parseQuerys(fileToBuffer(querysStream));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (InputStream relsStream = Files.newInputStream(relsDir)) {
			setRelDocs(fileToBuffer(relsStream));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setRelDocs(StringBuffer relsBuffer) {
		int id;
		QueryType q;
		int relDocId;
		List<String[]> relevances = CACMParser.parseRelevances(relsBuffer);
		for (String[] line : relevances) {
			id = Integer.parseInt(line[0]);
			relDocId = Integer.parseInt(line[1]);
			q = this.getQuery(id);
			q.addRelDoc(relDocId);
		}
	}

	public QueryType getQuery(int n) {
		return list.get(n - 1);
	}

//	public static void main(String[] args) {
//		String querysPath = "D:\\RI\\CACM\\query.text";
//		String relsPath = "D:\\RI\\CACM\\qrels.text";
//
//		QueryManagement qm = new QueryManagement(querysPath, relsPath);
//	}
}
