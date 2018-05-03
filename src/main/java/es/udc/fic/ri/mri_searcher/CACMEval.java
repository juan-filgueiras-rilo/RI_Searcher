package es.udc.fic.ri.mri_searcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CACMEval {

	private enum IndexOperation {
		NONE, CREATE, SEARCH, TRAINING_TEST, PRF;
	}

	private static IndexOperation OP = IndexOperation.NONE;
	private static String PROPERTIES_PATH = System.getProperty("user.dir") + "\\paths.properties";
	private static boolean setOpIfNone(IndexOperation op) {
		if (CACMEval.OP.equals(IndexOperation.NONE)) {
			CACMEval.OP = op;
			return true;
		}
		if (CACMEval.OP.equals(op)) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		// String indexPath = "D:\\RI\\CACMindex";
		String indexPath = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\index";
		// String docsPath = "D:\\RI\\cacm";
		String docsPath = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\docs\\cacm.all";
		OpenMode modo = OpenMode.CREATE;
		int cut = 0;
		int top = 0;
		int n_rel_docs = 0;
		int n_terms_to_expand = 0;
		List<Integer> queryList = new ArrayList<>();
		Similarity similarity = new BM25Similarity();

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			// Creacion indice
			case ("-index"):
				setOpIfNone(IndexOperation.CREATE);
				if (args.length - 1 >= i + 1 && isValidPath(args[i + 1])) {
					indexPath = args[++i];
					System.out.println("Path donde se creará / actualizará el Índice: " + indexPath);
					break;
				} else {
					System.err.println("Wrong option -index.\n");
					System.exit(-1);
				}
			case ("-coll"):
				setOpIfNone(IndexOperation.CREATE);
				if (args.length - 1 >= i + 1 && isValidPath(args[i + 1])) {
					docsPath = args[++i];
					System.out.println("Directorio de los documentos: " + docsPath);
					break;
				} else {
					System.err.println("Wrong option -coll.\n");
					System.exit(-1);
				}
			case ("-openmode"):
				setOpIfNone(IndexOperation.CREATE);
				if (args.length - 1 >= i + 1) {
					System.out.println("Modo de apertura del índice: " + args[i + 1]);
					switch (args[++i]) {
					case "create":
						modo = OpenMode.CREATE;
						break;
					case "append":
						modo = OpenMode.APPEND;
						break;
					case "create_or_append":
						modo = OpenMode.CREATE_OR_APPEND;
						break;
					default:
						System.err.println("Wrong option -openmode.\n");
						System.exit(-1);
					}
					break;
				} else {
					System.err.println("Missing arg for -openmode.\n");
					System.exit(-1);
				}
			case ("-indexingmodel"):
				setOpIfNone(IndexOperation.CREATE);
				if (args.length - 1 >= i + 2) {
					String model = args[++i];
					if (model.equals("jm")) {
						float lambda = Float.parseFloat(args[++i]);
						similarity = new LMJelinekMercerSimilarity(lambda);
						System.out
								.println("Usando model de similitud: LMJelinekMercerSimilarity con lambda: " + lambda);
					} else if (model.equals("dir")) {
						float mu = Float.parseFloat(args[++i]);
						similarity = new LMDirichletSimilarity(mu);
						System.out.println("Usando model de similitud: LMDirichletSimilarity con mu: " + mu);
					} else {
						System.err.println("Invalid arg '" + model + "' for -indexingmodel.\n");
						System.exit(-1);
					}
					break;
				} else {
					System.err.println("Missing arg for -indexingmodel.\n");
					System.exit(-1);
				}
			case ("-search"):
				setOpIfNone(IndexOperation.SEARCH);
				if (args.length - 1 >= i + 2) {
					String model = args[++i];
					if (model.equals("jm")) {
						float lambda = Float.parseFloat(args[++i]);
						similarity = new LMJelinekMercerSimilarity(lambda);
						System.out
								.println("Usando model de similitud: LMJelinekMercerSimilarity con lambda: " + lambda);
					} else if (model.equals("dir")) {
						float mu = Float.parseFloat(args[++i]);
						similarity = new LMDirichletSimilarity(mu);
						System.out.println("Usando model de similitud: LMDirichletSimilarity con mu: " + mu);
					} else {
						System.err.println("Invalid arg '" + model + "' for -search.\n");
						System.exit(-1);
					}
					break;
				} else {
					System.err.println("Missing arg for -search.\n");
					System.exit(-1);
				}
			case ("-indexin"):
				if (args.length - 1 >= i + 1 && isValidPath(args[i + 1])) {
					indexPath = args[++i];
					System.out.println("Path del Índice a utilizar: " + indexPath);
					break;
				} else {
					System.err.println("Wrong option -indexin.\n");
					System.exit(-1);
				}
			case ("-cut"):
				setOpIfNone(IndexOperation.SEARCH);
				if (args.length - 1 >= i + 1) {
					cut = Integer.parseInt(args[++i]);
					System.out.println("Usando " + cut + " documentos del ranking para el cómputo del MAP.");
					break;
				} else {
					System.err.println("Wrong option -cut.\n");
					System.exit(-1);
				}
			case ("-top"):
				setOpIfNone(IndexOperation.SEARCH);
				if (args.length - 1 >= i + 1) {
					top = Integer.parseInt(args[++i]);
					System.out.println("Mostrando los " + top + " primeros documentos del ranking...");
					break;
				} else {
					System.err.println("Wrong option -top.\n");
					System.exit(-1);
				}
			case ("-queries"):
				setOpIfNone(IndexOperation.SEARCH);
				if (args.length - 1 >= i + 1) {
					String arg = args[++i];
					String[] queries = arg.split("-");
					if (!arg.equals("all")) {
						if (queries.length > 1) {
							queryList.add(Integer.parseInt(queries[0]));
							queryList.add(Integer.parseInt(queries[1]));
							System.out.println("Using queries between " + arg);
						} else {
							queryList.add(Integer.parseInt(arg));
							System.out.println("Using query with ID: " + arg);
						}
					} else {
						queryList.add(1);
						queryList.add(64);
						System.out.println("Using all queries.");
					}
					break;
				} else {
					System.err.println("Wrong option -top.\n");
					System.exit(-1);
				}
			case ("-evaljm"):
				setOpIfNone(IndexOperation.TRAINING_TEST);
				break;
			case("-prf"):
				setOpIfNone(IndexOperation.PRF);
				if (args.length - 1 >= i + 2) {
					String model = args[++i];
					if (model.equals("jm")) {
						float lambda = Float.parseFloat(args[++i]);
						similarity = new LMJelinekMercerSimilarity(lambda);
						System.out
								.println("Usando model de similitud: LMJelinekMercerSimilarity con lambda: " + lambda);
					} else if (model.equals("dir")) {
						float mu = Float.parseFloat(args[++i]);
						similarity = new LMDirichletSimilarity(mu);
						System.out.println("Usando model de similitud: LMDirichletSimilarity con mu: " + mu);
					} else {
						System.err.println("Invalid arg '" + model + "' for -prf.\n");
						System.exit(-1);
					}
					break;
				} else {
					System.err.println("Missing arg for -prf.\n");
					System.exit(-1);
				}
            case("-prs"):
                setOpIfNone(IndexOperation.PRF);
                if (args.length - 1 >= i + 1) {
                    n_rel_docs = Integer.parseInt(args[++i]);
                    System.out.println("El Pseudo Relevance Set se construirá con los primeros " + n_rel_docs + " documentos.");
                    break;
                } else {
                    System.err.println("Missing arg for -prs.\n");
                    System.exit(-1);
                }
            case("-exp"):
                setOpIfNone(IndexOperation.PRF);
                if (args.length - 1 >= i + 1) {
                    n_terms_to_expand = Integer.parseInt(args[++i]);
                    System.out.println("Expande la query original con los mejores " + n_rel_docs + " términos.");
                    break;
                } else {
                    System.err.println("Missing arg for -exp.\n");
                    System.exit(-1);
                }
            case("-query"):
                setOpIfNone(IndexOperation.PRF);
                if (args.length - 1 >= i + 1) {
                    queryList.add(Integer.parseInt(args[++i]));
                    System.out.println("Seleccionada query con ID: " + args[i]);
                    break;
                } else {
                    System.err.println("Missing arg for -query.\n");
                    System.exit(-1);
                }
			}

		}
		Date start = new Date();
		Date end = null;
		try {
			if (CACMEval.OP.equals(IndexOperation.CREATE)) {
				final Path docDir = Paths.get(docsPath);
				if (!Files.isReadable(docDir)) {
					System.out.println("Document directory '" + docDir.toAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
					System.exit(1);
				}

				// Añado ruta de addIndexes para poder crear el writer principal antes de
				// trabajar en las subcarpetas de mismo nivel.
				System.out.println("Indexing to directory '" + indexPath + "'...");

				Directory dir = FSDirectory.open(Paths.get(indexPath));
				Analyzer analyzer = new StandardAnalyzer();
				IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
				iwc.setSimilarity(similarity);
				iwc.setOpenMode(modo);
				iwc.setRAMBufferSizeMB(512.0);

				IndexWriter indexWriter = new IndexWriter(dir, iwc);

				indexDocs(indexWriter, docDir);
				indexWriter.close();
			} else if (CACMEval.OP.equals(IndexOperation.SEARCH)) {
				doSearch(indexPath, similarity, queryList, top, cut);
			} else if (CACMEval.OP.equals(IndexOperation.TRAINING_TEST)) {
				
			} else if (CACMEval.OP.equals(IndexOperation.PRF)) {
                if (queryList.size() == 1){
                    Properties propsFile = new Properties();
                    propsFile.load(new InputStreamReader(Files.newInputStream(Paths.get(CACMEval.PROPERTIES_PATH))));
                    String queryPath = propsFile.getProperty("queryPath");
                    String qrelsPath = propsFile.getProperty("qrelsPath");
                    QueryManagement queryManagement = new QueryManagement(queryPath, qrelsPath);
                    List<String> terms_to_expand_query = new ArrayList<>();
                    String terms_expand_query = "";
                    int new_id = 0;
                    doSearch(indexPath,similarity,queryList,top,cut);
                    /*Extraer mejores términos para expandir la query y meterlos en terms_to_expand_query*/
                    for (String term:terms_to_expand_query){
                        terms_expand_query += " " + term;
                    }
                    new_id = queryManagement.expandQuery(queryList.get(0),terms_expand_query);
                    queryList.clear();
                    queryList.add(new_id);
                    doSearch(indexPath,similarity,queryList,top,cut);
                    /*Extraer resultados*/
                    /*Comparar resultados*/
                }else{
                    System.out.println("Falta argumento -query");
                }
            }
			if (end == null) {
				end = new Date();
			}
			System.out.println(end.getTime() / 1000 - start.getTime() / 1000 + " total seconds");
		} catch (IOException | org.apache.lucene.queryparser.classic.ParseException e) {
			System.err.println("Caught a " + e.getClass() + " with message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	static void doSearch(String indexPath, Similarity similarity, List<Integer> queryList, int top, int cut) throws IOException, org.apache.lucene.queryparser.classic.ParseException {
		
		//Usamos indexpath obtenido en indexin
		Directory dir = FSDirectory.open(Paths.get(indexPath));
		DirectoryReader indexReader = DirectoryReader.open(dir);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		//Seteamos la similaridad con la suavización para la búsqueda
		indexSearcher.setSimilarity(similarity);
		
		//Creamos la lista para el Query Management
		Properties propsFile = new Properties();
		propsFile.load(new InputStreamReader(Files.newInputStream(Paths.get(CACMEval.PROPERTIES_PATH))));
		String queryPath = propsFile.getProperty("queryPath");
		String qrelsPath = propsFile.getProperty("qrelsPath");
		QueryManagement queryManagement = new QueryManagement(queryPath, qrelsPath);
		Analyzer analyzer = new StandardAnalyzer();
		
		//Preparamos las queries pedidas y los campos
		String[] fields = {"T","W"};
		
		//Creamos variables para las métricas promediadas.
		float meanPAt10 = 0;
		float meanPAt20 = 0;
		float meanRecallAt10 = 0;
		float meanRecallAt20 = 0;
		float meanAveragePrecission = 0;
		int i;
		//Por cada query, mostramos query, documentos con info, y métricas
		for (i=queryList.get(0); i<=queryList.get(queryList.size()-1); i++) {
			
			int rels10Count = 0;
			int rels20Count = 0;
			int relsCount = 0;
			float avgPrecission = 0;
			QueryType query = queryManagement.getQuery(i);
			System.out.println("\nQuery: " + query.getBody());
			String escapedQuery = MultiFieldQueryParser.escape(query.getBody());
			String[] queries = {escapedQuery, escapedQuery};
			
			Query q =  MultiFieldQueryParser.parse(queries, fields, analyzer);
			
			TopDocs topDocs = indexSearcher.search(q, top);
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			System.out.println("Number of Top Docs: " + topDocs.scoreDocs.length);
			
			for (int j = 0; j < scoreDocs.length; j++) {
				
				ScoreDoc scoredDoc = scoreDocs[j];
				System.out.println("\nDoc nº: " + scoredDoc.doc + ", score: " + scoredDoc.score);
				Document doc = indexReader.document(scoredDoc.doc);
				List<IndexableField> docFields = doc.getFields();
				for (IndexableField docField : docFields) {
					System.out.println(docField.name() + ": " +docField.stringValue());
				}
				query.isRelevant(Integer.parseInt(doc.getField("I").stringValue().trim()));
				System.out.println("\nIs relevant: " + query.isRelevant(scoredDoc.doc) + "\n");
				System.out.println("---------------------------------------------------------");
				
				if (query.isRelevant(scoredDoc.doc)) {
					if(j < 20) {
						if (j < 10) {
							rels10Count++;
						}
						rels20Count++;
					}
					relsCount++;
					avgPrecission += (float)relsCount/(j+1);;
				}
			}
			int relSize = query.getRelDocs().size();
			if (relSize != 0 && relsCount != 0) {
				//Metrica P@N para N = 10 y 20.
				System.out.println("P@10: " + (float)rels10Count/10);
				meanPAt10 += (float)rels10Count/10;
				System.out.println("P@20: " + (float)rels20Count/20);
				meanPAt20 += (float)rels20Count/20;
				System.out.println("---------------------------------------------------------");
				//Metrica Recall@N para N = 10 y 20.
				meanRecallAt10 += (float)rels10Count/relSize;
				meanRecallAt20 += (float)rels20Count/relSize;
				System.out.println("Recall@10: " + (float)rels10Count/relSize);
				System.out.println("Recall@20: " + (float)rels20Count/relSize);
				System.out.println("---------------------------------------------------------");
				//Métrica para AP TODO
				System.out.println("AP: " + avgPrecission/relsCount);
				if (cut <= avgPrecission/relsCount)
					meanAveragePrecission += avgPrecission/relsCount;
				System.out.println("---------------------------------------------------------\n");
			}
			System.out.println("*********************************************************");
		}
		//Medias para las métricas para todas las queries
		int queryNo = i - queryList.get(0);
		System.out.println("Mean P@10 for '" + queryNo + "' queries: " + meanPAt10/queryNo);
		System.out.println("Mean P@20 for '" + queryNo + "' queries: " + meanPAt20/queryNo);
		System.out.println("Mean Recall@10 for '" + queryNo + "' queries: " + meanRecallAt10/queryNo);
		System.out.println("Mean Recall@20 for '" + queryNo + "' queries: " + meanRecallAt20/queryNo);
		System.out.println("MAP for '" + queryNo + "' queries: " + meanAveragePrecission/queryNo);
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For good
	 * throughput, put multiple documents into your input file(s). An example of
	 * this is in the benchmark module, which can create "line doc" files, one
	 * document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be stored
	 * @param path
	 *            The file to index, or the directory to recurse into to find files
	 *            to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 */
	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						if (file.toString().endsWith(".all"))
							indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					} catch (IOException ignore) {
						// don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}

	/** Indexes a single document */
	static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {

		FieldType type = new FieldType();
		type.setTokenized(true);
		type.setStored(true);
		type.setStoreTermVectors(true);
		type.setStoreTermVectorOffsets(true);
		type.setStoreTermVectorPositions(true);
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		type.freeze();

		try (InputStream stream = Files.newInputStream(file)) {
			List<List<String>> parsedContent = CACMParser.parseString(fileToBuffer(stream));
			for (List<String> parsedDoc : parsedContent) {
				Document doc = new Document();

				Field pathSgm = new StringField("path", file.toString(), Field.Store.YES);
				doc.add(pathSgm);

				Field hostname = new StringField("hostname", System.getProperty("user.name"), Field.Store.YES);
				doc.add(hostname);

				Field thread = new StringField("thread", Thread.currentThread().getName(), Field.Store.YES);
				doc.add(thread);
				// docid?
				Field i = new TextField("I", parsedDoc.get(0).trim(), Field.Store.YES);
				doc.add(i);
				// title?
				Field t = new Field("T", parsedDoc.get(1).trim(), type);
				doc.add(t);
				// date
				Field b = new StringField("B", parsedDoc.get(2).trim(), Field.Store.YES);
				doc.add(b);
				// names
				Field a = new Field("A", parsedDoc.get(3).trim(), type);
				doc.add(a);
				// dateline
				Field n = new StringField("N", parsedDoc.get(4).trim(), Field.Store.YES);
				// Field oldID = new LongPoint("oldid", Long.parseLong(parsedDoc.get(5)));
				doc.add(n);
				Field w = new Field("W", parsedDoc.get(5).trim(), type);
				doc.add(w);
				// Field content = new StringField("content", parsedDoc.get(5),
				// Field.Store.YES);
				// Field newID = new LongPoint("newid", Long.parseLong(parsedDoc.get(6)));
				// doc.add(content);
				// 26-FEB-1987 15:01:01.79
				// SimpleDateFormat dateFormat = new SimpleDateFormat("d-MMMM-yyyy HH:mm:ss.SS",
				// Locale.ENGLISH);
				// Date date = dateFormat.parse(parsedDoc.get(3));
				// String dateText = DateTools.dateToString(date, Resolution.SECOND);
				// Field dateField = new StringField("date", dateText, Field.Store.YES);
				// doc.add(dateField);

				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					// New index, so we just add the document (no old document can be there):
					writer.addDocument(doc);
				} else {
					// Existing index (an old copy of this document may have been indexed) so
					// we use updateDocument instead to replace the old one matching the exact
					// path, if present:
					writer.updateDocument(new Term("path", file.toString()), doc);
				}
			}
			if (writer.getConfig().getOpenMode() == OpenMode.CREATE)
				System.out.println("adding " + file);
			else
				System.out.println("updating " + file);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static StringBuffer fileToBuffer(InputStream is) throws IOException {
		StringBuffer buffer = new StringBuffer();
		InputStreamReader isr = null;

		try {
			isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			while ((line = br.readLine()) != null) {
				buffer.append(line + "\n");
			}
		} finally {
			if (is != null) {
				is.close();
			}
			if (isr != null) {
				isr.close();
			}
		}

		return buffer;
	}

	public static boolean isValidPath(String path) {

		try {
			Paths.get(path);
		} catch (InvalidPathException | NullPointerException ex) {
			return false;
		}
		return true;
	}
}
