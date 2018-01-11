package pmb.music.AllMusic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import pmb.music.AllMusic.XML.ImportXML;
import pmb.music.AllMusic.file.CsvFile;
import pmb.music.AllMusic.file.ImportFile;
import pmb.music.AllMusic.model.Cat;
import pmb.music.AllMusic.model.Composition;
import pmb.music.AllMusic.model.Fichier;
import pmb.music.AllMusic.model.RecordType;
import pmb.music.AllMusic.utils.CompositionUtils;
import pmb.music.AllMusic.utils.Constant;
import pmb.music.AllMusic.utils.SearchUtils;
import pmb.music.AllMusic.view.Onglet;

/**
 * Unit test for simple App.
 */
public class AppTest {

	private static final Logger LOG = Logger.getLogger(AppTest.class);
	private static final int YEAR_TOP = 2016;

	public static void main(String[] args) {
//		missingXML();
//		detectsDuplicate(RecordType.SONG.toString());
		detectsDuplicateFinal(RecordType.SONG.toString());
//		titleSlash();
//		topYear();
	}
	
	/**
	 * Tests the recover of information from txt files. 
	 */
	public static void randomLineTest() {
		List<File> files = new ArrayList<>();
		CompositionUtils.listFilesForFolder(new File(Constant.MUSIC_ABS_DIRECTORY), files, ".txt", true);
		for (File file : files) {
			LOG.error(file.getName());
			Fichier fichier = ImportFile.convertOneFile(file);
			List<String> randomLineAndLastLines = ImportFile.randomLineAndLastLines(file);
			fichier.setSorted(ImportFile.isSorted(randomLineAndLastLines.get(3)));
			fichier.setSize(ImportFile.determineSize(fichier, randomLineAndLastLines, file.getAbsolutePath()));
			LOG.error(fichier.getSize());
		}
	}
	
	/**
	 * Merge similar txt files.
	 * @param args
	 */
	public static void mergeFile(String[] args) {
		LOG.debug("Debut");
		File first = new File(Constant.MUSIC_ABS_DIRECTORY + "Rolling Stone\\Rolling Stone - 500 Albums.txt");
		File sec = new File(Constant.MUSIC_ABS_DIRECTORY + "Rolling Stone\\Rolling Stone - 500 Albums - 2012.txt");
		List<String> list = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(first), Constant.ANSI_ENCODING));) {
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		JaroWinklerDistance jaro= new JaroWinklerDistance();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sec), Constant.ANSI_ENCODING));) {
			String line;
			while ((line = br.readLine()) != null) {
				final String str = StringUtils.substringAfter(line, ". ");
				if(!list.stream().anyMatch(s->StringUtils.equalsIgnoreCase(StringUtils.substringAfter(s, ". "), str))
						&& !list.stream().anyMatch(s->CompositionUtils.artistJaroEquals(str, StringUtils.substringAfter(s, ". "),jaro, Constant.SCORE_LIMIT_ARTIST_FUSION) != null)) {
					list.add(line);
				}
			}
		} catch (IOException e1) {
		}
		list = list.stream().sorted(
				(s1, s2) -> Integer.compare(Integer.valueOf(StringUtils.substringBefore(s1, ". ")), Integer.valueOf(StringUtils.substringBefore(s2, ". "))))
				.collect(Collectors.toList());
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Constant.MUSIC_ABS_DIRECTORY + "RS.txt"), Constant.ANSI_ENCODING));) {
			for (String str : list) {
				writer.append(str).append(Constant.NEW_LINE);
			}
			writer.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		LOG.debug("Fin");
	}
	
	/**
	 * Search if there are txt files which are not convert to xml files.
	 */
	public static void missingXML() {
		LOG.debug("Debut");
		List<File> music = new ArrayList<>();
		CompositionUtils.listFilesForFolder(new File(Constant.MUSIC_ABS_DIRECTORY), music, ".txt", true);
		List<String> collectMusic = music.stream().map(File::getName).map(s -> StringUtils.substringBeforeLast(s, ".txt")).collect(Collectors.toList());
		
		List<File> xml = new ArrayList<>();
		CompositionUtils.listFilesForFolder(new File(Constant.XML_PATH), xml, Constant.XML_EXTENSION, true);
		List<String> collectXml = xml.stream().map(File::getName).map(s -> StringUtils.substringBeforeLast(s, Constant.XML_EXTENSION)).collect(Collectors.toList());
		
		LOG.debug("TXT: ");
		for (String txt : collectMusic) {
			if(!collectXml.stream().anyMatch(s -> StringUtils.equalsAnyIgnoreCase(s, txt))) {
				LOG.debug("Error: " + txt);
			}
		}
		LOG.debug("XML: ");
		for (String xmlFile : collectXml) {
			if(!collectMusic.stream().anyMatch(s -> StringUtils.equalsAnyIgnoreCase(s, xmlFile))) {
				LOG.debug("Error: " + xmlFile);
			}
		}
		LOG.debug("Fin");
	}
	
	/**
	 * Show all the duplicates for a year and a type regardless of the artist, only based on the song or album.
	 */
	public static void detectsDuplicateFinal(String type) {
		LOG.debug("Debut detectsDuplicateFinal");
		final JaroWinklerDistance jaro = new JaroWinklerDistance();
		List<Composition> importXML = ImportXML.importXML(Constant.FINAL_FILE_PATH);
		if (CollectionUtils.isNotEmpty(importXML)) {
			LOG.debug("Size: " + importXML.size());
			List<Composition[]> duplicate = new ArrayList<Composition[]>();
			double startTime = System.currentTimeMillis();
			for (int i = 0; i < importXML.size(); i++) {
				for (int j = 0; j < importXML.size(); j++) {
					if(!importXML.get(i).getRecordType().toString().equals(type) || !importXML.get(j).getRecordType().toString().equals(type)) {
						continue;
					}
					boolean isCriteria = true;
//							importXML.get(i).getFiles().stream().anyMatch(
//							f -> f.getCategorie().equals(Cat.YEAR) && f.getRangeDateBegin() == YEAR_TOP && f.getRangeDateEnd() == YEAR_TOP									
//								&& f.getPublishYear() == YEAR_TOP)
//							&& importXML.get(j).getFiles().stream().anyMatch(
//								f -> f.getCategorie().equals(Cat.YEAR) && f.getRangeDateBegin() == YEAR_TOP && f.getRangeDateEnd() == YEAR_TOP
//								&& f.getPublishYear() == YEAR_TOP);
					if (i < j && isCriteria) {
						Composition composition1 = importXML.get(i);
						Composition composition2 = importXML.get(j);
						String artist1 = composition1.getArtist();
						String artist2 = composition2.getArtist();
//						boolean result = (SearchUtils.isEqualsJaro(jaro, newTitre1, newTitre2, Constant.SCORE_LIMIT_TITLE_FUSION)
//								|| StringUtils.startsWithIgnoreCase(titre1, titre2) || StringUtils.startsWithIgnoreCase(titre2, titre1))
//								&& (StringUtils.startsWithIgnoreCase(artist1, artist2) || StringUtils.startsWithIgnoreCase(artist2, artist1))
//								&& publishYear1 == publishYear2;
						boolean similarArtist = StringUtils.startsWithIgnoreCase(artist1, artist2) || StringUtils.startsWithIgnoreCase(artist2, artist1);
						if (similarArtist) {
							String titre1 = composition1.getTitre().toLowerCase();
							String titre2 = composition2.getTitre().toLowerCase();
							String parTitre1 = SearchUtils.removePunctuation2(SearchUtils.removeParentheses(titre1));
							String parTitre2 = SearchUtils.removePunctuation2(SearchUtils.removeParentheses(titre2));
							boolean parTitreEqu = StringUtils.startsWithIgnoreCase(parTitre1, parTitre2) || StringUtils.startsWithIgnoreCase(parTitre2, parTitre1);
							if (parTitreEqu && ((parTitre1.length() <= 4 && parTitre2.length() > 4)
									|| (parTitre1.length() > 4 && parTitre2.length() <= 4))) {
								String andTitre1 = SearchUtils.removePunctuation2(StringUtils.substringBefore(SearchUtils.removeParentheses(titre1), " and "));
								String andTitre2 = SearchUtils.removePunctuation2(StringUtils.substringBefore(SearchUtils.removeParentheses(titre2), " and "));
								parTitre1 = andTitre1;
								parTitre2 = andTitre2;
								parTitreEqu = false;
							}
							boolean equalsJaroPar = SearchUtils.isEqualsJaro(jaro, parTitre1, parTitre2, Constant.SCORE_LIMIT_TITLE_FUSION);
							if(equalsJaroPar) {
								duplicate.add(new Composition[] { importXML.get(i), importXML.get(j) });
							}
						}
					}
				}
			}
			double endTime = System.currentTimeMillis();
			LOG.debug("Time: " + (endTime-startTime)/1000 + " secondes");
			LOG.debug("Size: " + duplicate.size());
			List<Composition> toRemove = new ArrayList<>();
			for (Composition[] c : duplicate) {
				LOG.debug("###########################################");
				LOG.debug(c[0]);
				LOG.debug(c[1]);
//				Composition c1 = importXML.get(SearchUtils.indexOf(importXML, c[0]));
//				List<Fichier> files = c1.getFiles();
//				toRemove.add(c1);
//				Composition c2 = importXML.get(SearchUtils.indexOf(importXML, c[1]));
//				c2.getFiles().addAll(files);
//				if(c1.getArtist().length()<c2.getArtist().length()) {
//					c2.setArtist(c1.getArtist());
//				}
//				if(c1.getTitre().length()<c2.getTitre().length()) {
//					c2.setTitre(c1.getTitre());
//				}
			}
//			importXML.removeAll(toRemove);
//			try {
//				ExportXML.exportXML(importXML, Constant.FINAL_FILE);
//			} catch (IOException e) {
//				LOG.error("Error !!", e);
//			}
		}
		LOG.debug("Fin detectsDuplicateFinal");
	}
	
	@Test
	public void removeParenthese() {
		String test1 = "hello (and truc)";
		String test2 = "(and truc) hello";
		String test3 = "hello, bonjour (and truc)";
		String test4 = "(and truc) hello, bonjour";
		String test5 = "hello, bonjour";
		String test6 = "hello, bonjour";
		String test7 = "hello, bonjour [and thed]";
		String test8 = "[and thed] hello, bonjour";
		
		Assert.assertEquals("hello ", SearchUtils.removeParentheses(test1));
		Assert.assertEquals(" hello", SearchUtils.removeParentheses(test2));
		Assert.assertEquals("hello, bonjour ", SearchUtils.removeParentheses(test3));
		Assert.assertEquals(" hello, bonjour", SearchUtils.removeParentheses(test4));
		Assert.assertEquals("hello, bonjour", SearchUtils.removeParentheses(test5));
		Assert.assertEquals("hello, bonjour", SearchUtils.removeParentheses(test6));
		Assert.assertEquals("hello, bonjour ", SearchUtils.removeParentheses(test7));
		Assert.assertEquals(" hello, bonjour", SearchUtils.removeParentheses(test8));
	}
	
	@Test
	public void removeParentheseAndPunctuation() {
		String test1 = "hello (and truc)";
		String test2 = "(and truc) hello";
		String test3 = "hello, bonjour (and truc)";
		String test4 = "(and truc) hello, bonjour";
		String test5 = "hello, bonjour";
		String test6 = "hello, bonjour";
		String test7 = "hello, bonjour [and thed]";
		String test8 = "[and thed] hello, bonjour";
		
		Assert.assertEquals("hello", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test1)));
		Assert.assertEquals("hello", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test2)));
		Assert.assertEquals("hellobonjour", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test3)));
		Assert.assertEquals("hellobonjour", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test4)));
		Assert.assertEquals("hellobonjour", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test5)));
		Assert.assertEquals("hellobonjour", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test6)));
		Assert.assertEquals("hellobonjour", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test7)));
		Assert.assertEquals("hellobonjour", SearchUtils.removePunctuation2(SearchUtils.removeParentheses(test8)));
	}
	
	/**
	 * Show all the duplicates for a year and a type regardless of the artist, only based on the song or album.
	 */
	public static void detectsDuplicate(String type) {
		LOG.debug("Debut detectsDuplicate");
		final JaroWinklerDistance jaro = new JaroWinklerDistance();
		List<Composition> importXML = ImportXML.importXML(Constant.FINAL_FILE_PATH);
		if (CollectionUtils.isNotEmpty(importXML)) {
			String year = String.valueOf(YEAR_TOP);
			Map<String, String> criteria = new HashMap<>();
			criteria.put("cat", Cat.YEAR.toString());
//			criteria.put("dateB", year);
//			criteria.put("dateE", year);
//			criteria.put("publish", year);
			criteria.put("type", type);
			List<Composition> yearList = SearchUtils.searchJaro(importXML, criteria, true);
			LOG.debug("Size: " + yearList.size());
			List<Integer[]> duplicate = new ArrayList<Integer[]>();
			for (int i = 0; i < yearList.size(); i++) {
				for (int j = 0; j < yearList.size(); j++) {
					if (i < j) {
						Composition composition1 = yearList.get(i);
						Composition composition2 = yearList.get(j);
						String titre1 = composition1.getTitre();
						String titre2 = composition2.getTitre();
						String newTitre1 = SearchUtils.removePunctuation2(titre1);
						String newTitre2 = SearchUtils.removePunctuation2(titre2);
						String artist1 = composition1.getArtist();
						String artist2 = composition2.getArtist();
						int publishYear1 = composition1.getFiles().get(0).getPublishYear();
						int publishYear2 = composition2.getFiles().get(0).getPublishYear();
						boolean result = (SearchUtils.isEqualsJaro(jaro, newTitre1, newTitre2, Constant.SCORE_LIMIT_TITLE_FUSION)
								|| StringUtils.startsWithIgnoreCase(titre1, titre2) || StringUtils.startsWithIgnoreCase(titre2, titre1))
								&& (StringUtils.startsWithIgnoreCase(artist1, artist2) || StringUtils.startsWithIgnoreCase(artist2, artist1))
								&& publishYear1 == publishYear2;
						if (result) {
							duplicate.add(new Integer[] { i, j });
						}
					}
				}
			}
			LOG.debug("Size: " + duplicate.size());
			for (Integer[] integers : duplicate) {
				LOG.debug("###########################################");
				LOG.debug(yearList.get(integers[0]));
				LOG.debug(yearList.get(integers[1]));
			}
		}
		LOG.debug("Fin detectsDuplicate");
	}

	/**
	 * Generates the top excel files of a year.
	 */
	public static void topYear() {
		List<Composition> importXML = ImportXML.importXML(Constant.FINAL_FILE_PATH);
		if (CollectionUtils.isNotEmpty(importXML)) {
			String year = String.valueOf(YEAR_TOP);
			topOccurence(importXML, year);
			topRecords(importXML, RecordType.SONG, "Top Songs", 4, year);
			topRecords(importXML, RecordType.ALBUM, "Top Albums", 10, year);
			topSongsParPublication(year);
		}
	}
	
	public static void titleSlash() {
		ImportXML.importXML(Constant.FINAL_FILE_PATH).stream().forEach(c->{
			if(StringUtils.contains(c.getTitre(), "/")) {
				LOG.debug(c.getArtist() + " - " + c.getTitre());
			}
		});
	}

	/**
	 * Generates csv file with each top 10 songs of every publication. 
	 * @param year
	 */
	private static void topSongsParPublication(String year) {
		List<String> authors = Onglet.getAuthorList();		
		List<String []> result = new ArrayList<String[]>();
		for (String author : authors) {
			List<Composition> arrayList = ImportXML.importXML(Constant.FINAL_FILE_PATH);
			Map<String, String> criteria = new HashMap<>();
			criteria.put("cat", Cat.YEAR.toString());
			criteria.put("dateB", year);
			criteria.put("dateE", year);
			criteria.put("publish", year);
			criteria.put("type", RecordType.SONG.toString());
			criteria.put("auteur", author);
			List<Composition> yearList = SearchUtils.searchJaro(arrayList, criteria, true);
			List<String[]> temp = new ArrayList<String[]>();
			String[] v = initArray(author, "-1");
			String[] w = initArray("", "-2");
			temp.add(v);
			temp.add(w);
			for (int i = 0; i < yearList.size(); i++) {
				Composition composition = yearList.get(i);
				if(composition.getFiles().get(0).getSorted() && composition.getFiles().get(0).getClassement()<=10) {
					v = new String[3];
					v[0] = composition.getArtist();
					v[1] = composition.getTitre();
					v[2] = String.valueOf(composition.getFiles().get(0).getClassement());
					temp.add(v);
				}
			}
			if(temp.size() > 2) {
				List<String[]> hello = temp.stream().sorted((e1, e2) -> Integer.valueOf(e1[2]).compareTo(Integer.valueOf(e2[2]))).collect(Collectors.toList());
				result.addAll(hello);
			}
		}
		for (String[] strings : result) {
			if(StringUtils.isBlank(strings[1])) {
				strings[2] = "";
			}
		}
		String[] header = {"Artiste","Titre", "Classement"};
		CsvFile.exportCsv("Top Songs Par Publication - " + year, result, header);
	}

	private static String[] initArray(String author, String value) {
		String[] v = new String[3];
		v[0] = author;
		v[1] = "";
		v[2] = value;
		return v;
	}

	/**
	 * Top songs or top albums.
	 * @param importXML
	 * @param type
	 * @param fileName
	 * @param limit
	 * @param year
	 */
	private static void topRecords(List<Composition> importXML, RecordType type, String fileName, int limit, String year) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put("cat", Cat.YEAR.toString());
		criteria.put("dateB", year);
		criteria.put("dateE", year);
		criteria.put("publish", year);
		criteria.put("type", type.toString());
		List<Composition> yearList = SearchUtils.searchJaro(importXML, criteria, true);
		List<Vector<Object>> occurenceListTemp = CompositionUtils.convertCompositionListToVector(yearList).stream().
				filter(c->(int) c.get(3)>=limit).collect(Collectors.toList());
		Vector<Vector<Object>> occurenceList = new Vector<Vector<Object>>();
		for (Vector<Object> vector : occurenceListTemp) {
			occurenceList.add(vector);
		}
		SortKey sortKey = new SortKey(3, SortOrder.DESCENDING);
		CsvFile.writeCsvFromSearchResult(occurenceList, fileName + " - " + YEAR_TOP, year, sortKey);
	}

	/**
	 * Generates csv file with the most occurence of an artist, songs and albums combine. 
	 * @param importXML
	 * @param year
	 */
	private static void topOccurence(List<Composition> importXML, String year) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put("cat", Cat.YEAR.toString());
		criteria.put("dateB", year);
		criteria.put("dateE", year);
		criteria.put("publish", year);
		List<Composition> yearList = SearchUtils.searchJaro(importXML, criteria, true);
		List<Vector<Object>> occurenceListTemp = CompositionUtils.convertCompositionListToArtistVector(yearList).stream().
				filter(c->(int) c.get(1)>9).collect(Collectors.toList());
		Vector<Vector<Object>> occurenceList = new Vector<Vector<Object>>();
		for (Vector<Object> vector : occurenceListTemp) {
			occurenceList.add(vector);
		}
		SortKey sortKey = new SortKey(1, SortOrder.DESCENDING);
		CsvFile.writeCsvFromArtistPanel(occurenceList, "Top Occurence - " + YEAR_TOP, year, sortKey);
	}
	
	/**
	 * Generates statistics of xml files.
	 * @param args
	 */
	public static void stat(String[] args) {
		LOG.debug("Debut");
		List<Composition> importXML = ImportXML.importXML(Constant.FINAL_FILE_PATH);
		List<Integer> size = new ArrayList<>();
		for (Composition composition : importXML) {
			String s = composition.getArtist() + composition.getTitre();
			size.add(s.length());
		}
		LOG.debug("Min: " + size.stream().mapToInt(Integer::intValue).min());
		LOG.debug("Max: " + size.stream().mapToInt(Integer::intValue).max());
		LOG.debug("Moyenne: " + size.stream().mapToInt(Integer::intValue).average());
		LOG.debug("Summary: " + size.stream().mapToInt(Integer::intValue).summaryStatistics());
		LOG.debug(size);
		LOG.debug("Fin");
	}

	@Test
	public void detectUpper() {
		String test = "05. GERRY & THE PACEMAKERS Ferry Cross the Mersey";
		boolean sorted = true;
		char[] array = test.toCharArray();
		int cut = 0;
		for (int i = 0; i < array.length; i++) {
			if (Character.isUpperCase(array[i]) || !Character.isAlphabetic(array[i])) {
				LOG.debug("upper: " + array[i]);
			} else {
				cut = i - 1;
				LOG.debug("else: " + array[i]);
				break;
			}
		}
		String artist = StringUtils.substring(test, 0, cut);
		String titre = StringUtils.substring(test, cut);
		LOG.debug("cut: " + cut);
		LOG.debug("artist: " + artist);
		LOG.debug("titre: " + titre);
		int rank = 0;
		if (sorted) {
			String res = StringUtils.substringBefore(artist, Constant.DOT);
			if (StringUtils.isNumeric(res)) {
				rank = Integer.parseInt(res);
				artist = StringUtils.substringAfter(artist, Constant.DOT);
			} else {
				res = artist.split(" ")[0];
				rank = Integer.parseInt(res);
				artist = StringUtils.substringAfterLast(artist, res);
			}
		} else {
			rank = 1;
		}
		LOG.debug("rank: " + rank);
	}

	@Test
	public void detectQuote() {
		String test = "05. GERRY & THE PACEMAKERS Ferry Cross the Mersey";
		boolean sorted = true;
		char[] array = test.toCharArray();
		int cut = 0;
		for (int i = 0; i < array.length; i++) {
			if (Character.isUpperCase(array[i]) || !Character.isAlphabetic(array[i])) {
				LOG.debug("upper: " + array[i]);
			} else {
				cut = i - 1;
				LOG.debug("else: " + array[i]);
				break;
			}
		}
		String artist = StringUtils.substring(test, 0, cut);
		String titre = StringUtils.substring(test, cut);
		LOG.debug("cut: " + cut);
		LOG.debug("artist: " + artist);
		LOG.debug("titre: " + titre);
		int rank = 0;
		if (sorted) {
			String res = StringUtils.substringBefore(artist, Constant.DOT);
			if (StringUtils.isNumeric(res)) {
				rank = Integer.parseInt(res);
				artist = StringUtils.substringAfter(artist, Constant.DOT);
			} else {
				res = artist.split(" ")[0];
				rank = Integer.parseInt(res);
				artist = StringUtils.substringAfterLast(artist, res);
			}
		} else {
			rank = 1;
		}
		LOG.debug("rank: " + rank);
	}

	@Test
	public void distanceJaro() {
		List<Composition> guardian = ImportXML.importXML(Constant.FINAL_FILE_PATH);
		String test = "beachboys";
		Map<Double, String> jaroRes = new TreeMap<>();
		JaroWinklerDistance jaro = new JaroWinklerDistance();
		for (Composition composition : guardian) {
			String titre = SearchUtils.removePunctuation2(composition.getArtist());
			Double apply = jaro.apply(titre, test);
			jaroRes.put(apply, titre);
		}
		for (Entry<Double, String> entry : jaroRes.entrySet()) {
			LOG.debug("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
	}
	
	@Test
	public void distanceJaroLine() {
		String s1 = "Fun House - The Stooges";
		String s2 = "Funhouse - The Stooges";
		JaroWinklerDistance jaro = new JaroWinklerDistance();
		Double apply = jaro.apply(s1, s2);
		LOG.debug("Key : " + s1 + " Value : " + s2 + " " + apply);
	}
}
