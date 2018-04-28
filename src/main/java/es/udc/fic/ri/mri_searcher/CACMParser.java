package es.udc.fic.ri.mri_searcher;

import javax.management.Query;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CACMParser {

	/*
	 * Project testlucene 3.6.0, the CACMParser class parses the
	 * collection.
	 */
	public CACMParser(){}
	


	public static List<List<String>> parseString(StringBuffer fileContent) {
		/* First the contents are converted to a string */
		String text = fileContent.toString();
		/*
		 * The method split of the String class splits the strings using the
		 * delimiter which was passed as argument Therefore lines is an array of
		 * strings, one string for each line
		 */
		String[] lines = text.split("\n");
		List<List<String>> documents = new LinkedList<List<String>>();

		/* The tag .I identifies the beginning and end of each article */

		for (int i = 0; i < lines.length; ++i) {
			if (!lines[i].startsWith(".I"))
				continue;
			StringBuilder sb = new StringBuilder();
			sb.append(lines[i++]);
            sb.append("\n");
			while (!lines[i].startsWith(".I")) {
				sb.append(lines[i++]);
				sb.append("\n");
                if (i == lines.length)
                    break;
			}
			i--;
			sb.append("<END>");
			/*
			 * Here the sb object of the StringBuilder class contains the
			 * Reuters article which is converted to text and passed to the
			 * handle document method that will return the document in the form
			 * of a list of fields
			 */
			documents.add(handleDocument(sb.toString()));
		}
		return documents;
	}

	public static List<String[]> parseRelevances(StringBuffer fileContent){
        String text = fileContent.toString();
        String[] lines = text.split("\n");
        List<String[]> result = new ArrayList<>();
        for (String s:lines){
            result.add(s.split(" "));
        }
        return result;
    }

    public static List<QueryType> parseQuerys(StringBuffer fileContent) {

        String text = fileContent.toString();
        String[] lines = text.split("\n");
        List<QueryType> querys = new LinkedList<>();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].startsWith(".I"))
                continue;
            StringBuilder sb = new StringBuilder();
            sb.append(lines[i++]);
            sb.append("\n");
            while (!lines[i].startsWith(".I")) {
                sb.append(lines[i++]);
                sb.append("\n");
                if (i == lines.length)
                    break;
            }
            --i;
            sb.append("<END>");
            querys.add(handleQuery(sb.toString()));
        }
        return querys;
    }

    public static QueryType handleQuery(String text) {

        String queryNo = extract("I ", text, true);
        String body = extract("W\n ", text, true);
        QueryType query = new QueryType(Integer.parseInt(queryNo));
        query.setBody(body);
        return query;
    }

	public static List<String> handleDocument(String text) {

		/*
		 * This method returns the Reuters article that is passed as text as a
		 * list of fields
		 */

		/* The fields TOPICS, TITLE, DATELINE and BODY are extracted */
		/* Each topic inside TOPICS is identified with a tag D */
		/* If the BODY ends with boiler plate text, this text is removed */

		String docNo = extract("I", text, true);
		String title = extract("T", text, true);
		if (title.startsWith("\n")){
			title = title.replaceFirst("\n", "");
		}
		String date = extract("B", text, true);
		if (date.startsWith("\n")){
			date = date.replaceFirst("\n", "");
		}
		String names = extract("A", text, true);
		if (names.startsWith("\n")){
			names = names.replaceFirst("\n", "");
		}
		String dateline = extract("N", text, true);
		if (dateline.startsWith("\n")){
			dateline = dateline.replaceFirst("\n", "");
		}
		String content = extract("X", text, true);
		if (content.startsWith("\n")){
			content = content.replaceFirst("\n", "");
		}
//		content.replaceAll("\t", "|");
//		if (body.endsWith(END_BOILERPLATE_1)
//				|| body.endsWith(END_BOILERPLATE_2))
//			body = body
//					.substring(0, body.length() - END_BOILERPLATE_1.length());
		List<String> document = new LinkedList<String>();
		document.add(docNo);
		document.add(title);
		document.add(date);
		document.add(names);
		document.add(dateline);
		document.add(content);
		return document;
	}

	private static String extract(String elt, String text, boolean allowEmpty) {

		/*
		 * This method find the tags for the field elt in the String text and
		 * extracts and returns the content
		 */
		/*
		 * If the tag does not exists and the allowEmpty argument is true, the
		 * method returns the null string, if allowEmpty is false it returns a
		 * IllegalArgumentException
		 */

		String startElt = "." + elt;
		String endElt = "\n.";
		int startEltIndex = text.indexOf(startElt);
		if (startEltIndex < 0) {
			if (allowEmpty)
				return "";
			throw new IllegalArgumentException("no start, elt=" + elt
					+ " text=" + text);
		}
		int start = startEltIndex + startElt.length();
		int end = text.indexOf(endElt, start);
		if (end < 0) {
			end = text.indexOf("<END>", start);
			if(end < 0)
				throw new IllegalArgumentException("no end, elt=" + elt + " text="
						+ text);
		}
		return text.substring(start, end);
	}

}