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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CACMEval {

	private enum IndexOperation {
		NONE,
		CREATE,
		PROCESS;
	}
	private static IndexOperation OP = IndexOperation.NONE;
	private static boolean setOpIfNone(IndexOperation op) {
		if(CACMEval.OP.equals(IndexOperation.NONE)) {
			CACMEval.OP = op;
			return true;
		}
		if(CACMEval.OP.equals(op)) {
			return true;
		}
		return false;
	}
	
	public static void main(String[] args) {
		String querysPath = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\docs\\query.text";
		String relsPath = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\docs\\qrels.text";
		//String indexPath = "D:\\RI\\CACMindex";
		String indexPath = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\index";
		//String docsPath = "D:\\RI\\cacm";
		String docsPath = "D:\\UNI\\3º\\Recuperación de la Información\\2018-2\\docs\\cacm.all";
		OpenMode modo = OpenMode.CREATE;
        Similarity similarity = new BM25Similarity();
		
		for(int i=0;i<args.length;i++) {
			switch(args[i]) {
			//Creacion indice
			case("-index"):
				setOpIfNone(IndexOperation.CREATE);
				if (args.length-1 >= i+1 && isValidPath(args[i+1])) {
					indexPath = args[++i];
					break;
				} else {
					System.err.println("Wrong option -index.\n");
					System.exit(-1);
				}
			case("-coll"):
				setOpIfNone(IndexOperation.CREATE);
				if (args.length-1 >= i+1 && isValidPath(args[i+1])) {
					docsPath = args[++i];
					break;
				} else {
					System.err.println("Wrong option -coll.\n");
					System.exit(-1);
				}
			case("-openmode"):	
				setOpIfNone(IndexOperation.CREATE);
				if(args.length-1 >= i+1) {
					switch(args[++i]) {
					case "create": modo = OpenMode.CREATE;
						break;
					case "append": modo = OpenMode.APPEND;
						break;
					case "create_or_append": modo = OpenMode.CREATE_OR_APPEND;
						break;
					default: System.err.println("Wrong option -openmode.\n");
						System.exit(-1);
					}
					break;
				} else {
					System.err.println("Missing arg for -openmode.\n");
					System.exit(-1);
				}
            case("-indexingmodel"):
                setOpIfNone(IndexOperation.CREATE);
                if(args.length-1 >= i+2) {
                    String model = args[++i];
                    if (model.equals("jm")){
                        float lambda = Float.parseFloat(args[++i]);
                        similarity = new LMJelinekMercerSimilarity(lambda);
                    } else if (model.equals("dir")){
                        float mu = Float.parseFloat(args[++i]);
                        similarity = new LMDirichletSimilarity(mu);
                    } else {
                        System.err.println("Invalid arg '" + model + "' for -indexingmodel.\n");
                        System.exit(-1);
                    }
                    break;
                }else{
                    System.err.println("Missing arg for -indexingmodel.\n");
                    System.exit(-1);
                }

			}
		}
		Date start = new Date();
		Date end = null;
		try {
			if(CACMEval.OP.equals(IndexOperation.CREATE)) {
				final Path docDir = Paths.get(docsPath);
				if (!Files.isReadable(docDir)) {
					System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
					System.exit(1);
				}
				
				//Añado ruta de addIndexes para poder crear el writer principal antes de trabajar en las subcarpetas de mismo nivel.
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
			}
			if(end == null) {
				end = new Date();
			}
			System.out.println(end.getTime()/1000 - start.getTime()/1000 + " total seconds");
		} catch (IOException e) {
			System.err.println("Caught a " + e.getClass() + " with message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 * 
	 * NOTE: This method indexes one document per input file.  This is slow.  For good
	 * throughput, put multiple documents into your input file(s).  An example of this is
	 * in the benchmark module, which can create "line doc" files, one document per line,
	 * using the
	 * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 *  
	 * @param writer Writer to the index where the given file/dir info will be stored
	 * @param path The file to index, or the directory to recurse into to find files to index
	 * @throws IOException If there is a low-level I/O error
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
		
		FieldType t = new FieldType();
		t.setTokenized(true);
		t.setStored(true);
		t.setStoreTermVectors(true);
		t.setStoreTermVectorOffsets(true);
		t.setStoreTermVectorPositions(true);
		t.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		t.freeze();
		
		try (InputStream stream = Files.newInputStream(file)) {
			List<List<String>> parsedContent = CACMParser.parseString(fileToBuffer(stream));
			for (List<String> parsedDoc:parsedContent) {
				Document doc = new Document();
				
				Field pathSgm = new StringField("path", file.toString(), Field.Store.YES);
				doc.add(pathSgm);
				
				Field hostname = new StringField("hostname", System.getProperty("user.name"), Field.Store.YES);
				doc.add(hostname);
				
				Field thread = new StringField("thread", Thread.currentThread().getName(), Field.Store.YES);
				doc.add(thread);
				
				Field docid = new TextField("docid", parsedDoc.get(0), Field.Store.YES);
				doc.add(docid);
				
				Field title = new Field("title", parsedDoc.get(1), t);
				doc.add(title);
				
				Field date = new StringField("date", parsedDoc.get(2), Field.Store.YES);
				doc.add(date);
				
				Field names = new Field("names", parsedDoc.get(3), t);
				doc.add(names);
				
				Field dateline = new StringField("dateline", parsedDoc.get(4), Field.Store.YES);
				//Field oldID = new LongPoint("oldid", Long.parseLong(parsedDoc.get(5)));
				doc.add(dateline);
				
				Field content = new StringField("content", parsedDoc.get(5), Field.Store.YES);
				//Field newID = new LongPoint("newid", Long.parseLong(parsedDoc.get(6)));
				doc.add(content);
				//26-FEB-1987 15:01:01.79
//				SimpleDateFormat dateFormat = new SimpleDateFormat("d-MMMM-yyyy HH:mm:ss.SS", Locale.ENGLISH);
//				Date date = dateFormat.parse(parsedDoc.get(3));
//				String dateText = DateTools.dateToString(date, Resolution.SECOND);
//				Field dateField = new StringField("date", dateText, Field.Store.YES);
//				doc.add(dateField);
				
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
