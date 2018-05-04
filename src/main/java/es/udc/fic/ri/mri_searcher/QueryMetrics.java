package es.udc.fic.ri.mri_searcher;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;

public class QueryMetrics {

	QueryType query;
	Query parsedQuery;

	private float pAt10;
	private float pAt20;
	private float recallAt10;
	private float recallAt20;
	private List<Float> precissions;
	private float averagePrecission;

	public QueryMetrics(QueryType query, Query parsedQuery) {

		this.parsedQuery = parsedQuery;
		this.query = query;
		this.pAt10 = 0;
		this.pAt20 = 0;
		this.recallAt10 = 0;
		this.recallAt20 = 0;
		this.averagePrecission = 0;
		this.precissions = new ArrayList<>();
	}

	public void computePAt10(int relsAt10) {
		this.pAt10 = (float) relsAt10 / 10;
	}

	public void computePAt20(int relsAt20) {
		this.pAt20 = (float) relsAt20 / 20;
	}

	public void computeRecallAt10(int relsAt10) {
		this.recallAt10 = (float) relsAt10 / this.query.getRelDocs().size();
	}

	public void computeRecallAt20(int relsAt20) {
		this.recallAt20 = (float) relsAt20 / this.query.getRelDocs().size();
	}

	public void addPrecission(float precission) {
		this.precissions.add(precission);
	}

	public void computeAveragePrecission() {
		for (float precission : this.precissions) {
			this.averagePrecission += precission;
		}
		this.averagePrecission = this.averagePrecission / this.query.getRelDocs().size();
	}

	public Query getParsedQuery() {
		return parsedQuery;
	}

	public void setParsedQuery(Query parsedQuery) {
		this.parsedQuery = parsedQuery;
	}

	public float getPAt10() {
		return pAt10;
	}

	public float getPAt20() {
		return pAt20;
	}

	public float getRecallAt10() {
		return recallAt10;
	}

	public float getRecallAt20() {
		return recallAt20;
	}

	public float getAveragePrecission() {
		return averagePrecission;
	}
	
	public boolean areValid() {
		return this.query.getRelDocs().size()>0;
	}
}
