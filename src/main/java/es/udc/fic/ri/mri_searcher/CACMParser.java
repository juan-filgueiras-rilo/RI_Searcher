package es.udc.fic.ri.mri_searcher;

import java.util.LinkedList;
import java.util.List;

public class CACMParser {

	/*
	 * Project testlucene 3.6.0, the CACMParser class parses the
	 * collection.
	 */
	public CACMParser(){}
	
	//boiler modificado
	private static final String END_BOILERPLATE_1 = "Reuter\n&#3;";
	private static final String END_BOILERPLATE_2 = "REUTER\n&#3;";

	public static List<List<String>> parseString(StringBuffer fileContent) {
		/* First the contents are converted to a string */
		String text = fileContent.toString();

		/*
		 * The method split of the String class splits the strings using the
		 * delimiter which was passed as argument Therefor lines is an array of
		 * strings, one string for each line
		 */
		String[] lines = text.split("\n");

		/*
		 * For each Reuters article the parser returns a list of strings where
		 * each element of the list is a field (TITLE, BODY, TOPICS, DATELINE).
		 * Each *.sgm file that is passed in fileContent can contain many
		 * Reuters articles, so finally the parser returns a list of list of
		 * strings, i.e, a list of reuters articles, that is what the object
		 * documents contains
		 */

		List<List<String>> documents = new LinkedList<List<String>>();

		/* The tag REUTERS identifies the beginning and end of each article */

		for (int i = 0; i < lines.length; ++i) {
			if (!lines[i].startsWith("<REUTERS"))
				continue;
			StringBuilder sb = new StringBuilder();
			while (!lines[i].startsWith("</REUTERS")) {
				sb.append(lines[i++]);
				sb.append("\n");
			}
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

	public static List<String> handleDocument(String text) {

		/*
		 * This method returns the Reuters article that is passed as text as a
		 * list of fields
		 */

		/* The fields TOPICS, TITLE, DATELINE and BODY are extracted */
		/* Each topic inside TOPICS is identified with a tag D */
		/* If the BODY ends with boiler plate text, this text is removed */

		//OLDID="403" NEWID="12175"
		int oldIDstart = text.indexOf("OLDID=\"") + 7;
		int oldIDfinish = text.indexOf("\"", oldIDstart);
		String oldID = text.substring(oldIDstart, oldIDfinish);
		int newIDstart = text.indexOf("NEWID=\"") + 7;
		int newIDfinish = text.indexOf("\"", newIDstart);
		String newID = text.substring(newIDstart, newIDfinish);
		//System.out.println(text.split("\n")[0] + "\t" + oldID + "\t" + newID);
		String topics = extract("TOPICS", text, true);
		String title = extract("TITLE", text, true);
		String dateline = extract("DATELINE", text, true);
		String date = extract("DATE", text, true);
		String body = extract("BODY", text, true);
		if (body.endsWith(END_BOILERPLATE_1)
				|| body.endsWith(END_BOILERPLATE_2))
			body = body
					.substring(0, body.length() - END_BOILERPLATE_1.length());
		List<String> document = new LinkedList<String>();
		document.add(title);
		document.add(body);
		document.add(topics.replaceAll("\\<D\\>", " ").replaceAll("\\<\\/D\\>",
				""));
		document.add(date);
		document.add(dateline);
		document.add(oldID);
		document.add(newID);
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

		String startElt = "<" + elt + ">";
		String endElt = "</" + elt + ">";
		int startEltIndex = text.indexOf(startElt);
		if (startEltIndex < 0) {
			if (allowEmpty)
				return "";
			throw new IllegalArgumentException("no start, elt=" + elt
					+ " text=" + text);
		}
		int start = startEltIndex + startElt.length();
		int end = text.indexOf(endElt, start);
		if (end < 0)
			throw new IllegalArgumentException("no end, elt=" + elt + " text="
					+ text);
		return text.substring(start, end);
	}

}