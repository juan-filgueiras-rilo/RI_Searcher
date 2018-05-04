package es.udc.fic.ri.mri_searcher;

import java.util.List;
import java.util.ArrayList;

public class MetricsManagement {

	private List<QueryMetrics> queryMetrics;
	
	private float meanPAt10;
	private float meanPAt20;
	private float meanRecallAt10;
	private float meanRecallAt20;
	private float meanAveragePrecission;
	
	public MetricsManagement() {
		
		this.queryMetrics = new ArrayList<>();
		this.meanPAt10 = 0;
		this.meanPAt20 = 0;
		this.meanRecallAt10 = 0;
		this.meanRecallAt20 = 0;
		this.meanAveragePrecission = 0;
	}
	
	public int getQueriesSize() {
		return this.queryMetrics.size();
	}
	
	public void computeAllMetrics() {
		this.computeMeanPAt10();
		this.computeMeanPAt20();
		this.computeMeanRecallAt10();
		this.computeMeanRecallAt20();
		this.computeMeanAveragePrecission();
	}
	
	public void computeMeanPAt10() {
		float mean = 0;
		for(QueryMetrics metrics : queryMetrics) {
			mean+=metrics.getPAt10(); 
		}
		this.meanPAt10 = (float) mean/this.queryMetrics.size();
	}
	
	public void computeMeanPAt20() {
		float mean = 0;
		for(QueryMetrics metrics : queryMetrics) {
			mean+=metrics.getPAt20(); 
		}
		this.meanPAt20 = (float) mean/this.queryMetrics.size();
	}
	
	public void computeMeanRecallAt10() {
		float mean = 0;
		for(QueryMetrics metrics : queryMetrics) {
			mean+=metrics.getRecallAt10(); 
		}
		this.meanRecallAt10 = (float) mean/this.queryMetrics.size();
	}
	
	public void computeMeanRecallAt20() {
		float mean = 0;
		for(QueryMetrics metrics : queryMetrics) {
			mean+=metrics.getRecallAt20(); 
		}
		this.meanRecallAt20 = (float) mean/this.queryMetrics.size();
	}
	
	public void computeMeanAveragePrecission() {
		float map = 0;
		for(QueryMetrics metrics : queryMetrics) {
			map+=metrics.getAveragePrecission(); 
		}
		this.meanAveragePrecission = (float) map/this.queryMetrics.size();
	}
	
	public void addQueryMetrics(QueryMetrics metrics) {
		this.queryMetrics.add(metrics);
	}
	
	public float getMeanPAt10() {
		return meanPAt10;
	}

	public float getMeanPAt20() {
		return meanPAt20;
	}

	public float getMeanRecallAt10() {
		return meanRecallAt10;
	}

	public float getMeanRecallAt20() {
		return meanRecallAt20;
	}

	public float getMeanAveragePrecission() {
		return meanAveragePrecission;
	}
}
