package pmb.music.AllMusic.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.text.WordUtils;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;

import com.opencsv.bean.CsvBindByName;

import pmb.music.AllMusic.XML.ExportXML;
import pmb.music.AllMusic.XML.ImportXML;
import pmb.music.AllMusic.file.CleanFile;
import pmb.music.AllMusic.file.CsvFile;
import pmb.music.AllMusic.file.CustomColumnPositionMappingStrategy;
import pmb.music.AllMusic.model.Cat;
import pmb.music.AllMusic.model.Composition;
import pmb.music.AllMusic.model.CsvComposition;
import pmb.music.AllMusic.model.Fichier;
import pmb.music.AllMusic.model.RecordType;
import pmb.music.AllMusic.model.SearchMethod;
import pmb.music.AllMusic.model.SearchRange;
import pmb.music.AllMusic.view.dialog.DeleteCompoDialog;
import pmb.music.AllMusic.view.panel.ArtistPanel;
import pmb.music.AllMusic.view.panel.BatchPanel;
import pmb.music.AllMusic.view.panel.OngletPanel;

public class BatchUtils {
	private static final Logger LOG = Logger.getLogger(BatchUtils.class);

	public static final Comparator<CsvComposition> compareByTrackNumber = (CsvComposition c1, CsvComposition c2) -> {
		String s1 = c1.getTrackNumber();
		if (StringUtils.contains(s1, Constant.TRACK_NUMBER_SEPARATOR)) {
			s1 = StringUtils.substringBefore(s1, Constant.TRACK_NUMBER_SEPARATOR);
		}
		String s2 = c2.getTrackNumber();
		if (StringUtils.contains(s2, Constant.TRACK_NUMBER_SEPARATOR)) {
			s2 = StringUtils.substringBefore(s2, Constant.TRACK_NUMBER_SEPARATOR);
		}
		Integer int1 = 0;
		if (StringUtils.isNotBlank(s1)) {
			int1 = Integer.valueOf(s1);
		}
		Integer int2 = 0;
		if (StringUtils.isNotBlank(s2)) {
			int2 = Integer.valueOf(s2);
		}
		return int1.compareTo(int2);
	};

	/**
	 * @param song
	 * @param album
	 * @param ignoreUnmergeableFiles if ignore file with merge equals to false
	 * @param byYear
	 * @param batchPanel for logging purpose
	 * @return
	 */
	public static String detectsDuplicateFinal(boolean song, boolean album, boolean ignoreUnmergeableFiles,
			boolean byYear, BatchPanel batchPanel) {
		LOG.debug("Start detectsDuplicateFinal");
		StringBuilder result = new StringBuilder();
		addLine(result, "DetectsDuplicateFinal: ", true);
		addLine(result, "Song: " + song, true);
		addLine(result, "Album: " + album, true);
		addLine(result, "Ignore Unmergeable Files: " + ignoreUnmergeableFiles, true);

		if (song) {
			detectsDuplicateFinal(RecordType.SONG.toString(), ignoreUnmergeableFiles, byYear, result, batchPanel);
		}
		if (album) {
			detectsDuplicateFinal(RecordType.ALBUM.toString(), ignoreUnmergeableFiles, byYear, result, batchPanel);
		}
		try {
			ImportXML.synchroDeletedWithFinal();
		} catch (MyException e) {
			LOG.error("Erreur lors de la détection de composition supprimées", e);
			addLine(result, e.toString(), true);
		}

		LOG.debug("End detectsDuplicateFinal");
		return writeInFile(result, Constant.BATCH_FILE);
	}

	/**
	 * Generates statistics of xml files.
	 * 
	 * @return
	 */
	public static String stat() {
		LOG.debug("Start stat");
		StringBuilder result = new StringBuilder();
		addLine(result, "Statistiques sur la longueur artiste et titre: ", true);
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		statsLength(result, importXML.stream().map(Composition::getArtist).distinct().map(String::length)
				.collect(Collectors.toList()), "Artiste: ");
		statsLength(result, importXML.stream().filter(c -> c.getRecordType().equals(RecordType.ALBUM))
				.map(composition -> composition.getTitre().length()).collect(Collectors.toList()), "Album: ");
		statsLength(result, importXML.stream().filter(c -> c.getRecordType().equals(RecordType.SONG))
				.map(composition -> composition.getTitre().length()).collect(Collectors.toList()), "Chanson: ");

		addLine(result, "Statistiques sur les tops annuels: ", true);
		Map<Integer, Integer> songs = importXML.stream().filter(c -> c.getRecordType().equals(RecordType.SONG))
				.map(Composition::getFiles).flatMap(List::stream).filter(f -> f.getCategorie().equals(Cat.YEAR))
				.collect(Collectors.groupingBy(Fichier::getRangeDateBegin, Collectors
						.collectingAndThen(Collectors.mapping(Fichier::getFileName, Collectors.toSet()), Set::size)));
		Map<Integer, Integer> albums = importXML.stream().filter(c -> c.getRecordType().equals(RecordType.ALBUM))
				.map(Composition::getFiles).flatMap(List::stream).filter(f -> f.getCategorie().equals(Cat.YEAR))
				.collect(Collectors.groupingBy(Fichier::getRangeDateBegin, Collectors
						.collectingAndThen(Collectors.mapping(Fichier::getFileName, Collectors.toSet()), Set::size)));

		List<Composition> songYear = importXML.stream()
				.filter(c -> c.getRecordType().equals(RecordType.SONG)
						&& c.getFiles().stream().anyMatch(f -> f.getCategorie().equals(Cat.YEAR)))
				.collect(Collectors.toList());
		List<Composition> albumYear = importXML.stream()
				.filter(c -> c.getRecordType().equals(RecordType.ALBUM)
						&& c.getFiles().stream().anyMatch(f -> f.getCategorie().equals(Cat.YEAR)))
				.collect(Collectors.toList());

		int min = Stream.concat(songs.keySet().stream(), albums.keySet().stream()).mapToInt(Integer::intValue).min()
				.getAsInt();
		int max = Stream.concat(songs.keySet().stream(), albums.keySet().stream()).mapToInt(Integer::intValue).max()
				.getAsInt();
		addLine(result, "Year;Songs Files;Songs Count;Albums Files;Albums Count;Total Files;Total Count", false);
		IntStream.rangeClosed(min, max).forEach(i -> {
			Integer song = !songs.containsKey(i) ? 0 : songs.get(i);
			Integer album = !albums.containsKey(i) ? 0 : albums.get(i);
			long songCount = songYear.stream()
					.filter(c -> c.getFiles().stream()
							.anyMatch(f -> f.getCategorie().equals(Cat.YEAR) && f.getRangeDateBegin().equals(i)))
					.count();
			long albumCount = albumYear.stream()
					.filter(c -> c.getFiles().stream()
							.anyMatch(f -> f.getCategorie().equals(Cat.YEAR) && f.getRangeDateBegin().equals(i)))
					.count();
			addLine(result, i + ";" + song.toString() + ";" + songCount + ";" + album.toString() + ";" + albumCount
					+ ";" + (song + album) + ";" + (songCount + albumCount), false);
		});

		LOG.debug("End stat");
		return writeInFile(result, "stats.csv");
	}

	private static void statsLength(StringBuilder result, List<Integer> size, String title) {
		addLine(result, title, true);
		IntSummaryStatistics summaryStatistics = size.stream().mapToInt(Integer::intValue).summaryStatistics();
		addLine(result, "Min: " + summaryStatistics.getMin(), false);
		addLine(result, "Max: " + summaryStatistics.getMax(), false);
		addLine(result, "Moyenne: " + summaryStatistics.getAverage(), false);
		addLine(result, "Mediane: " + MiscUtils.median(size), false);
		addLine(result,
				"Ecart-Type: "
						+ MiscUtils.calculateSD(size, summaryStatistics.getAverage(), summaryStatistics.getCount()),
				false);
		addLine(result, "Summary: " + summaryStatistics, false);
		addLine(result, "", false);
	}

	public static String findUnknown() {
		LOG.debug("Start findUnknown");
		Comparator<String> byFileName = (String o1, String o2) -> {
			String s1 = StringUtils.substringBeforeLast(o1, ";");
			String s2 = StringUtils.substringBeforeLast(o2, ";");
			return s1.compareToIgnoreCase(s2);
		};
		Comparator<String> byCount = (String o1, String o2) -> {
			Long r1 = Long.valueOf(StringUtils.substringAfterLast(o1, ";"));
			Long r2 = Long.valueOf(StringUtils.substringAfterLast(o2, ";"));
			return Long.compare(r1, r2);
		};
		Map<String, List<String>> result = new TreeMap<>(byFileName.thenComparing(byCount.reversed()));
		StringBuilder sb = new StringBuilder();
		addLine(sb, "Find Unknown: ", true);
		Map<String, String> criteria = new HashMap<>();
		criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, RecordType.UNKNOWN.toString());
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		List<Composition> unknown = SearchUtils.search(importXML, criteria, true, SearchMethod.WHOLE_WORD, true, true);
		AtomicInteger notFound = new AtomicInteger(0);
		unknown.parallelStream().forEach(u -> {
			Map<String, String> c = new HashMap<>();
			c.put(SearchUtils.CRITERIA_ARTIST, u.getArtist());
			c.put(SearchUtils.CRITERIA_TITRE, u.getTitre());
			List<Composition> search = SearchUtils.search(importXML, c, true, SearchMethod.CONTAINS, true, false);
			String fileName = u.getFiles().get(0).getFileName();
			String artistTitre = u.getArtist() + " - " + u.getTitre();
			String item;
			List<RecordType> types = search.stream().map(Composition::getRecordType)
					.filter(t -> !t.equals(RecordType.UNKNOWN)).collect(Collectors.toList());
			if (!types.isEmpty()) {
				long songCount = search.stream().filter(s -> s.getRecordType().equals(RecordType.SONG))
						.mapToInt(s -> s.getFiles().size()).sum();
				long albumCount = search.stream().filter(s -> s.getRecordType().equals(RecordType.ALBUM))
						.mapToInt(s -> s.getFiles().size()).sum();
				Long count;
				if (types.stream().allMatch(RecordType.ALBUM::equals)) {
					item = artistTitre + ": " + RecordType.ALBUM + " (" + albumCount + ")";
					count = albumCount;
				} else if (types.stream().allMatch(RecordType.SONG::equals)) {
					item = artistTitre + ": " + RecordType.SONG + " (" + songCount + ")";
					count = songCount;
				} else {
					item = artistTitre + Constant.NEW_LINE + RecordType.SONG + ": " + songCount + Constant.NEW_LINE
							+ RecordType.ALBUM + ": " + albumCount;
					count = (songCount + albumCount) / 2;
				}
				String key = fileName + ";" + count;
				if (result.containsKey(key)) {
					result.get(key).add(item);
				} else {
					result.put(key, new LinkedList<String>(Arrays.asList(item)));
				}
			} else {
				notFound.incrementAndGet();
			}
		});
		String currentKey = "";
		for (Entry<String, List<String>> e : result.entrySet()) {
			String key = StringUtils.substringBeforeLast(e.getKey(), ";");
			if (!StringUtils.equalsIgnoreCase(currentKey, key)) {
				sb.append(Constant.NEW_LINE + "### " + key + ": " + Constant.NEW_LINE);
				currentKey = key;
			}
			e.getValue().stream().forEach(v -> sb.append(v + Constant.NEW_LINE));
		}
		sb.append(Constant.NEW_LINE + "### Not Found: " + notFound.get());
		LOG.debug("End findUnknown");
		return writeInFile(sb, "Unknown.txt");
	}

	public static String findSuspiciousComposition() {
		LOG.debug("Start findSuspiciousComposition");
		StringBuilder result = new StringBuilder();
		addLine(result, "Suspicious: ", true);

		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		emptyTitleOrArtist(importXML, result);
		addLine(result, "", false);
		titleSlash(importXML, result);
		addLine(result, "", false);
		sizeZero(importXML, result);
		addLine(result, "", false);
		publishZero(importXML, result);
		addLine(result, "", false);
		rankZero(importXML, result);
		addLine(result, "", false);
		rankGreaterThanSize(importXML, result);
		addLine(result, "", false);
		duplicateCompositionInFile(importXML, result);

		LOG.debug("End findSuspiciousComposition");
		return writeInFile(result, "Suspicious.txt");
	}

	public static String findDuplicateTitleComposition() {
		LOG.debug("Start findDuplicateTitleComposition");
		StringBuilder result = new StringBuilder();
		addLine(result, "Duplicate Title: ", true);

		similarTitle(result);

		LOG.debug("End findDuplicateTitleComposition");
		return writeInFile(result, "Duplicate Title.txt");
	}

	public static String findIncorrectFileNames() {
		LOG.debug("Start findIncorrectFileNames");
		StringBuilder result = new StringBuilder();
		addLine(result, "IncorrectFileNames: ", true);

		List<String> res = new ArrayList<>();
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		Arrays.asList(OngletPanel.getAuthorList()).parallelStream().forEach(author -> {
			if (StringUtils.equalsIgnoreCase(author, "Divers")) {
				return;
			}
			Map<String, String> criteria = new HashMap<>();
			criteria.put(SearchUtils.CRITERIA_AUTHOR, author);
			res.addAll(SearchUtils.search(importXML, criteria, true, SearchMethod.CONTAINS, false, false)
					.parallelStream().map(Composition::getFiles).flatMap(List::stream)
					.filter(f -> (!StringUtils.startsWithIgnoreCase(f.getFileName(), f.getAuthor() + " - ")
							|| !StringUtils.endsWithIgnoreCase(f.getFileName(),
									" - " + String.valueOf(f.getPublishYear()))))
					.map(f -> f.getFileName() + " # " + String.valueOf(f.getPublishYear())).distinct().sorted()
					.collect(Collectors.toList()));
		});
		res.stream().forEach(f -> addLine(result, f, false));

		LOG.debug("End findIncorrectFileNames");
		return writeInFile(result, "Incorrect Filenames.txt");
	}

	private static void titleSlash(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## Title Slash: ", true);
		importXML.stream().map(c -> {
			if (c.getFiles().size() == 1 && StringUtils.contains(c.getTitre(), "/")) {
				return c.getArtist() + " - " + c.getTitre();
			} else {
				return "";
			}
		}).distinct().filter(StringUtils::isNotBlank).sorted().forEach(line -> addLine(result, line, false));
	}

	private static void similarTitle(StringBuilder result) {
		addLine(result, "## Same title but different artist: ", true);
		JaroWinklerDistance jaro = new JaroWinklerDistance();
		Map<String, String> criteria = new HashMap<>();
		criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, RecordType.SONG.toString());
		List<Composition> importXML = SearchUtils
				.search(ImportXML.importXML(Constant.getFinalFilePath()), criteria, true, SearchMethod.CONTAINS, false,
						false)
				.stream().sorted((c1, c2) -> StringUtils.compareIgnoreCase(c1.getTitre(), c2.getTitre()))
				.collect(Collectors.toList());
		for (int i = 0; i < importXML.size(); i++) {
			for (int j = 0; j < importXML.size(); j++) {
				if (i < j) {
					Composition c1 = importXML.get(i);
					Composition c2 = importXML.get(j);
					String c1Titre = SearchUtils.removePunctuation(c1.getTitre());
					String c2Titre = SearchUtils.removePunctuation(c2.getTitre());
					if (c1Titre.length() < 11 || c2Titre.length() < 11) {
						continue;
					}
					if (SearchUtils.isEqualsJaro(jaro, c1Titre, c2Titre, BigDecimal.valueOf(0.985D))
							&& CompositionUtils.artistJaroEquals(c1.getArtist(), c2.getArtist(), jaro,
									Constant.SCORE_LIMIT_ARTIST_FUSION) == null) {
						addLine(result, c1.getArtist() + " - " + c1.getTitre() + " // " + c2.getArtist() + " - "
								+ c2.getTitre(), false);
					}
				}
			}
		}
	}

	private static void rankZero(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## Rank Zero: ", true);
		importXML
				.stream().filter(
						c -> c.getFiles().stream().anyMatch(f -> f.getClassement() == 0))
				.forEach(c -> addLine(result,
						c.getArtist() + " - " + c.getTitre() + " / "
								+ StringUtils.join(c.getFiles().stream().filter(f -> f.getClassement() == 0)
										.map(Fichier::getFileName).collect(Collectors.toList()), ","),
						false));
	}

	private static void rankGreaterThanSize(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## Rank Greater Than Size: ", true);
		importXML
				.stream().map(Composition::getFiles).flatMap(List::stream).filter(
						f -> f.getClassement() > f.getSize() && f.getSize() != 0)
				.collect(
						Collectors
								.groupingBy(Fichier::getFileName,
										Collectors
												.collectingAndThen(
														Collectors
																.reducing((Fichier d1,
																		Fichier d2) -> d1.getClassement() > d2
																				.getClassement() ? d1 : d2),
														Optional::get)))
				.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(f -> addLine(result, f.getValue().getFileName() + ", size: " + f.getValue().getSize()
						+ ", classement max: " + f.getValue().getClassement(), false));
	}

	private static void duplicateCompositionInFile(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## Duplicate Composition In File: ", true);
		importXML.parallelStream().forEach(c -> {
			Map<String, Long> collect = c.getFiles().stream()
					.collect(Collectors.groupingBy(Fichier::getFileName, Collectors.counting()));
			if (collect.values().stream().anyMatch(v -> v > 1)) {
				collect.entrySet().stream().forEach(e -> {
					if (e.getValue() > 1) {
						addLine(result, c.getArtist() + ", " + c.getTitre() + ". " + e.getKey() + ": " + e.getValue(),
								false);
					}
				});
			}
		});
	}

	private static void sizeZero(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## File Size Zero: ", true);
		importXML.stream().map(Composition::getFiles).flatMap(List::stream).filter(f -> f.getSize() == 0)
				.map(Fichier::getFileName).distinct().sorted().forEach(f -> addLine(result, f, false));
	}

	private static void publishZero(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## File Publish Year Zero: ", true);
		importXML.stream().map(Composition::getFiles).flatMap(List::stream).filter(f -> f.getPublishYear() == 0)
				.map(Fichier::getFileName).distinct().sorted().forEach(f -> addLine(result, f, false));
	}

	private static void emptyTitleOrArtist(List<Composition> importXML, StringBuilder result) {
		addLine(result, "## Empty Title or Artist: ", true);
		importXML.stream().forEach(c -> {
			if (StringUtils.equalsIgnoreCase(StringUtils.trim(c.getTitre()), "")
					|| StringUtils.equalsIgnoreCase(StringUtils.trim(c.getArtist()), "")) {
				addLine(result, c.getArtist() + " - " + c.getTitre(), false);
			}
		});
	}

	public static String averageOfFilesByFiles(BatchPanel batchPanel) {
		LOG.debug("Start averageOfFilesByFiles");
		StringBuilder text = new StringBuilder();
		addLine(text, "Start AverageOfFilesByFiles", true);
		// Moyenne par fichier du nombre de fichiers de chaque composition
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		List<String> nomFichier = importXML.stream().map(Composition::getFiles).flatMap(List::stream)
				.map(Fichier::getFileName).distinct().sorted().collect(Collectors.toList());
		String[] header = { "Fichier", "Type", "Cat", "Average" };
		List<List<String>> result = new ArrayList<>();
		final AtomicInteger count = new AtomicInteger(0);
		final BigDecimal total = new BigDecimal(nomFichier.size());
		DecimalFormat decimalFormat = new Constant().getDecimalFormat();
		nomFichier.parallelStream().forEach(name -> {
			List<String> row = new ArrayList<>();
			Map<String, String> criteria = new HashMap<>();
			criteria.put(SearchUtils.CRITERIA_FILENAME, name);
			List<Composition> xml = SearchUtils.search(importXML, criteria, false, SearchMethod.WHOLE_WORD, true,
					false);
			row.add(name);
			row.add(xml.get(0).getRecordType().toString());
			row.add(xml.get(0).getFiles().stream().filter(f -> StringUtils.equalsIgnoreCase(f.getFileName(), name))
					.findFirst().get().getCategorie().getCat());
			row.add(decimalFormat
					.format(xml.stream().map(c -> c.getFiles().size()).mapToInt(x -> x).average().getAsDouble()));
			result.add(row);
			if (count.incrementAndGet() % 10 == 0) {
				batchPanel.displayText(BigDecimal.valueOf(100D).setScale(2).multiply(new BigDecimal(count.get()))
						.divide(total, RoundingMode.HALF_UP).doubleValue() + "%", count.get() != 10);
			}
		});
		CsvFile.exportCsv("Average", result,
				Arrays.asList(new SortKey(3, SortOrder.ASCENDING), new SortKey(0, SortOrder.ASCENDING)), header);
		LOG.debug("End averageOfFilesByFiles");
		addLine(text, "End AverageOfFilesByFiles", true);
		return writeInFile(text, Constant.BATCH_FILE);
	}

	public static String weirdFileSize() {
		LOG.debug("Start weirdFileSize");
		StringBuilder text = new StringBuilder();
		DecimalFormat decimalFormat = new Constant().getDecimalFormat();
		addLine(text, "Start weirdFileSize", true);
		// Moyenne par fichier du nombre de fichiers de chaque composition
		List<String> nomFichier = ImportXML.importXML(Constant.getFinalFilePath()).stream().map(Composition::getFiles)
				.flatMap(List::stream).map(Fichier::getFileName).distinct().sorted().collect(Collectors.toList());
		String[] header = { "Fichier", "Type", "Real Size", "Theoric Size", "Ratio" };
		List<List<String>> result = new ArrayList<>();
		nomFichier.parallelStream().forEach(name -> {
			List<Composition> xml = ImportXML.importXML(Constant.getXmlPath() + name + Constant.XML_EXTENSION);
			int realSize = xml.size();
			Integer theoricSize = xml.get(0).getFiles().get(0).getSize();
			if (theoricSize != 0 && realSize != theoricSize) {
				BigDecimal ratio = BigDecimal.valueOf(realSize).multiply(BigDecimal.valueOf(100D))
						.divide(BigDecimal.valueOf(theoricSize), BigDecimal.ROUND_DOWN);
				List<String> row = new ArrayList<>();
				row.add(name);
				row.add(xml.get(0).getRecordType().toString());
				row.add(decimalFormat.format(realSize));
				row.add(decimalFormat.format(theoricSize));
				row.add(decimalFormat.format(ratio.doubleValue()));
				result.add(row);
			}
		});
		CsvFile.exportCsv("Weird", result, Arrays.asList(new SortKey(4, SortOrder.ASCENDING)), header);
		LOG.debug("End weirdOfFilesByFiles");
		addLine(text, "End weirdFileSize", true);
		return writeInFile(text, Constant.BATCH_FILE);
	}

	public static String massDeletion(String type, File file, ArtistPanel artistPanel) {
		LOG.debug("Start massDeletion");
		StringBuilder text = new StringBuilder();
		addLine(text, "Start massDeletion", true);

		List<CsvComposition> compoCsv = CsvFile.importCsv(file, CsvComposition.class);
		addLine(text, "Import csv file successfully", true);

		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		artistPanel.interruptUpdateArtist(true);
		if (type.equals(RecordType.SONG.toString())) {
			massDeletionForSongs(text, compoCsv, importXML);
		} else {
			massDeletionForAlbums(text, compoCsv, importXML);
		}

		// Modifies csv entry file
		CustomColumnPositionMappingStrategy<CsvComposition> mappingStrategy = new CustomColumnPositionMappingStrategy<>();
		mappingStrategy.setType(CsvComposition.class);
		String[] columns = new String[] { "titre", "artist", "album", "duration", "bitrate", "added", "year",
				"playCount", "rank", "lastPlay", "trackNumber", "cdNumber", "deletedSong", "deletedAlbum" };
		mappingStrategy.setColumnMapping(columns);
		CsvFile.exportBeanList(file, compoCsv, mappingStrategy);
		addLine(text, "Csv file successfully exported", true);

		try {
			ExportXML.exportXML(importXML, Constant.getFinalFile());
			artistPanel.updateArtistPanel();
			addLine(text, "Final file successfully exported", true);
		} catch (IOException e1) {
			LOG.error("Erreur lors de l'export du fichier final", e1);
			addLine(text, "Erreur lors de l'export du fichier final !!" + e1, true);
		}

		LOG.debug("End massDeletion");
		addLine(text, "End massDeletion", true);
		return writeInFile(text, "Delete.txt");
	}

	/**
	 * Mass deletion for songs.
	 * 
	 * @param text log
	 * @param compoCsv compo to delete
	 * @param importXML compo from final file
	 */
	private static void massDeletionForSongs(StringBuilder text, List<CsvComposition> compoCsv,
			List<Composition> importXML) {
		DeleteCompoDialog deleteCompoDialog = new DeleteCompoDialog(null, compoCsv.size());
		for (int i = 0; i < compoCsv.size(); i++) {
			// Search composition
			CsvComposition compoToDelete = compoCsv.get(i);
			if (StringUtils.isNotBlank(compoToDelete.getDeletedSong())) {
				// Already processed
				continue;
			}
			Map<String, String> criteria = fillSearchCriteriaForMassDeletion(RecordType.SONG.toString(),
					compoToDelete.getArtist(), compoToDelete.getTitre());

			// Search composition
			List<Composition> compoFound = SearchUtils.search(importXML, criteria, false, SearchMethod.CONTAINS, true,
					false);
			if (compoFound.isEmpty()) {
				// nothing found
				compoToDelete.setDeletedSong("Not Found");
				continue;
			} else if (compoFound.size() > 1) {
				// Multiple result
				compoToDelete.setDeletedSong("Size: " + compoFound.size());
				continue;
			} else if (compoFound.get(0).isDeleted()) {
				// Already deleted
				compoToDelete.setDeletedSong("Already");
				continue;
			}
			Composition found = compoFound.get(0);
			// update dialog
			deleteCompoDialog.updateDialog(prettyPrintForSong(compoToDelete), found, i, warningForSong(compoToDelete));
			deleteCompoDialog.setVisible(true);
			Boolean action = deleteCompoDialog.getSendData();
			if (action == null) {
				// stop everything
				LOG.debug("Stop");
				break;
			} else if (action) {
				// Delete composition
				try {
					Composition toRemoveToFinal = CompositionUtils.findByArtistTitreAndType(importXML,
							found.getArtist(), found.getTitre(), found.getRecordType().toString(), true);
					importXML.get(SearchUtils.indexOf(importXML, toRemoveToFinal)).setDeleted(true);
					CompositionUtils.removeCompositionsInFiles(found);
					compoToDelete.setDeletedSong("OK");
				} catch (MyException e) {
					LOG.error("Error when deleting compostion: " + compoToDelete, e);
					compoToDelete.setDeletedSong("Error");
				}
			} else {
				// Skip composition
				compoToDelete.setDeletedSong("KO");
			}
		}
		addLine(text, "End of deleting Song", true);
	}

	private static String prettyPrintForSong(CsvComposition csv) {
		StringBuilder sb = new StringBuilder();
		List<String> ignoreField = Arrays.asList("deletedSong", "deletedAlbum", "artist", "titre", "trackNumber",
				"cdNumber");
		sb.append(Constant.NEW_LINE).append(csv.getArtist()).append(" - ").append(csv.getTitre());
		try {
			Field[] declaredFields = CsvComposition.class.getDeclaredFields();
			for (int i = 0; i < declaredFields.length; i++) {
				Field field = declaredFields[i];
				if (ignoreField.contains(field.getName())) {
					continue;
				}
				CsvBindByName annotation = field.getAnnotationsByType(CsvBindByName.class)[0];
				Object fieldValue = FieldUtils.readField(field, csv, true);
				if (fieldValue != null) {
					sb.append(Constant.NEW_LINE).append(annotation.column()).append(": ")
							.append(convertValueField(field, fieldValue));
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOG.error("This should not append", e);
		}
		return sb.toString();
	}

	/**
	 * Converts or formats given value depending on the field type and name.
	 * 
	 * @param field the field
	 * @param fieldValue the value of the field
	 * @return formats dates, if rank field converts to stars
	 */
	private static String convertValueField(Field field, Object fieldValue) {
		String result;
		if (field.getType().equals(Date.class)) {
			result = fieldValue != null ? new Constant().getSdfDttm().format(fieldValue) : "";
		} else if (field.getName().equals("rank")) {
			result = fieldValue != null ? String.valueOf((Integer) fieldValue / 20) + " Stars" : "0 Stars";
		} else if (field.getType().equals(Integer.class)) {
			result = fieldValue != null ? String.valueOf(fieldValue) : "0";
		} else {
			result = fieldValue != null ? String.valueOf(fieldValue) : "";
		}
		return result;
	}

	private static String warningForSong(CsvComposition csv) {
		List<String> result = new ArrayList<>();
		if (csv.getPlayCount() != null && csv.getPlayCount() < 10) {
			result.add("Nombre de lecture < 10");
		} else if (csv.getPlayCount() == null) {
			result.add("Nombre de lecture 0 !");
		}
		if (csv.getBitRate() != null) {
			String[] split = StringUtils.split(csv.getBitRate(), " ");
			if (split.length == 2 && NumberUtils.isDigits(split[0]) && Integer.valueOf(split[0]) < 128) {
				result.add("Bit Rate < 128");
			}
		}
		if (csv.getRank() != null && csv.getRank() < 90) {
			result.add("Classement < 5 Étoiles");
		} else if (csv.getRank() == null) {
			result.add("Classement 0 Étoiles !");
		}
		return result.stream().collect(Collectors.joining(Constant.NEW_LINE));
	}

	/**
	 * Mass deletion for albums.
	 * 
	 * @param text log
	 * @param compoCsv compo ffrom csv file
	 * @param importXML all compo from final file
	 */
	private static void massDeletionForAlbums(StringBuilder text, List<CsvComposition> compoCsv,
			List<Composition> importXML) {
		List<String> albumList = compoCsv.stream()
				.sorted(Comparator.comparing(CsvComposition::getAlbum).thenComparing(compareByTrackNumber.reversed()))
				.map(CsvComposition::getAlbum).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
		DeleteCompoDialog deleteCompoDialog = new DeleteCompoDialog(null, albumList.size());
		for (int i = 0; i < albumList.size(); i++) {
			String album = albumList.get(i);
			List<CsvComposition> compoAlbum = compoCsv.stream().filter(csv -> csv.getAlbum().equals(album))
					.collect(Collectors.toList());
			if (compoAlbum.stream().allMatch(csv -> StringUtils.isNotBlank(csv.getDeletedAlbum()))) {
				// Already processed
				continue;
			}
			CsvComposition compoToDelete = compoAlbum.get(0);
			String trackNumber = compoToDelete.getTrackNumber();
			String albumToSearch = "";
			if (compoAlbum.size() < 5 || StringUtils.isBlank(trackNumber)) {
				compoAlbum.forEach(csv -> csv.setDeletedAlbum("Invalid"));
				continue;
			} else if (StringUtils.contains(trackNumber, Constant.TRACK_NUMBER_SEPARATOR)) {
				String[] split = StringUtils.split(trackNumber, Constant.TRACK_NUMBER_SEPARATOR);
				String max = split[1];
				if (compoAlbum.size() >= Integer.valueOf(max)) {
					albumToSearch = album;
				} else {
					compoAlbum.forEach(csv -> csv.setDeletedAlbum("Incomplete"));
					continue;
				}
			} else if (compoAlbum.size() >= Integer.valueOf(trackNumber)) {
				albumToSearch = album;
			} else {
				compoAlbum.forEach(csv -> csv.setDeletedAlbum("Too Small"));
				continue;
			}
			Map<String, String> criteria = fillSearchCriteriaForMassDeletion(RecordType.ALBUM.toString(),
					compoToDelete.getArtist(), albumToSearch);
			// Search composition
			List<Composition> compoFound = SearchUtils.search(importXML, criteria, false, SearchMethod.CONTAINS, true,
					true);
			if (compoFound.isEmpty()) {
				// nothing found
				compoAlbum.forEach(csv -> csv.setDeletedAlbum("Not Found"));
				continue;
			} else if (compoFound.size() > 1) {
				// Multiple result
				compoAlbum.forEach(csv -> csv.setDeletedAlbum("Size: " + compoFound.size()));
				continue;
			} else if (compoFound.get(0).isDeleted()) {
				// Already deleted
				compoAlbum.forEach(csv -> csv.setDeletedAlbum("Already"));
				continue;
			}
			Composition found = compoFound.get(0);
			// update dialog
			deleteCompoDialog.updateDialog(prettyPrintForAlbum(compoAlbum), found, i, null);
			deleteCompoDialog.setVisible(true);
			Boolean action = deleteCompoDialog.getSendData();
			if (action == null) {
				// stop everything
				LOG.debug("Stop");
				break;
			} else if (action) {
				// Delete composition
				try {
					Composition toRemoveToFinal = CompositionUtils.findByArtistTitreAndType(importXML,
							found.getArtist(), found.getTitre(), found.getRecordType().toString(), true);
					importXML.get(SearchUtils.indexOf(importXML, toRemoveToFinal)).setDeleted(true);
					CompositionUtils.removeCompositionsInFiles(found);
					compoAlbum.forEach(csv -> csv.setDeletedAlbum("OK"));
				} catch (MyException e) {
					LOG.error("Error when deleting compostion: " + compoToDelete, e);
					compoAlbum.forEach(csv -> csv.setDeletedAlbum("Error"));
				}
			} else {
				// Skip composition
				compoAlbum.forEach(csv -> csv.setDeletedAlbum("KO"));
			}
		}
		addLine(text, "End of deleting Album", true);
	}

	private static String prettyPrintForAlbum(List<CsvComposition> list) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(Constant.NEW_LINE).append(groupByField(list, "artist"));
			sb.append(Constant.NEW_LINE).append("Album: " + list.get(0).getAlbum());
			sb.append(Constant.NEW_LINE).append(groupByField(list, "added"));
			sb.append(Constant.NEW_LINE).append(groupByField(list, "year"));
			sb.append(Constant.NEW_LINE).append(groupByField(list, "playCount"));
			sb.append(Constant.NEW_LINE).append(groupByField(list, "rank"));
		} catch (IllegalArgumentException | NoSuchFieldException e) {
			LOG.error("This should not append", e);
		}
		return sb.toString();
	}

	private static String groupByField(List<CsvComposition> list, String field) throws NoSuchFieldException {
		String result = "";
		Map<Object, Long> collect = list.stream().collect(Collectors.groupingBy(csv -> {
			try {
				return Optional.ofNullable(FieldUtils.readField(csv, field, true));
			} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
				LOG.error("This should not append", e);
				return "";
			}
		}, Collectors.counting()));
		Field declaredField = FieldUtils.getDeclaredField(CsvComposition.class, field, true);
		if (!collect.isEmpty()) {
			if (collect.size() == 1) {
				result = convertValueField(declaredField,
						((Optional<?>) collect.keySet().iterator().next()).orElse(null));
			} else {
				result = "{ "
						+ collect.entrySet().stream()
								.collect(Collectors.toMap(Map.Entry::getValue, e -> new ArrayList<String>(Arrays.asList(
										convertValueField(declaredField, ((Optional<?>) e.getKey()).orElse(null)))),
										(o, n) -> {
											o.addAll(n);
											if (declaredField.getType().equals(Integer.class)
													&& !declaredField.getName().equals("rank")) {
												o.sort(MiscUtils.compareInteger.reversed());
											}
											return o;
										}))
								.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
								.map(e -> {
									String res = e.getKey() + "x = ";
									if (e.getValue().size() > 1) {
										return res + "[" + StringUtils.join(e.getValue(), ", ") + "]";
									} else {
										return res + e.getValue().get(0);
									}
								}).collect(Collectors.joining(", "))
						+ " }";
			}
		}
		if (StringUtils.isNotBlank(result)) {
			return declaredField.getAnnotationsByType(CsvBindByName.class)[0].column() + ": " + result;
		} else {
			return "";
		}
	}

	private static Map<String, String> fillSearchCriteriaForMassDeletion(String type, String artist, String titre) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, type);
		// Clean artist and title
		Set<Entry<String, String>> entrySet = CleanFile.getModifSet();
		String stripArtist = StringUtils.substringBefore(
				SearchUtils.removeParentheses(CleanFile.removeDiactriticals(cleanLine(artist.toLowerCase(), entrySet))),
				Constant.SEPARATOR_AND);
		if (StringUtils.startsWith(stripArtist, "the ")) {
			stripArtist = StringUtils.substringAfter(stripArtist, "the ");
		}
		criteria.put(SearchUtils.CRITERIA_ARTIST, SearchUtils.removePunctuation(stripArtist));
		criteria.put(SearchUtils.CRITERIA_TITRE, SearchUtils.removePunctuation(SearchUtils
				.removeParentheses(CleanFile.removeDiactriticals(cleanLine(titre.toLowerCase(), entrySet)))));
		return criteria;
	}

	public static String cleanLine(String line, Set<Entry<String, String>> entrySet) {
		for (Entry<String, String> entry : entrySet) {
			if (StringUtils.containsIgnoreCase(line, entry.getKey())) {
				line = StringUtils.replaceIgnoreCase(line, entry.getKey(), entry.getValue());
			}
		}
		return line;
	}

	/**
	 * Search if a composition has similar files (same author and same rank).
	 * 
	 * @return
	 */
	public static String findDuplicateFiles() {
		LOG.debug("Start findDuplicateFiles");
		StringBuilder text = new StringBuilder();
		addLine(text, "FindDuplicateFiles: ", true);

		Map<String, Integer> result = new HashMap<>();
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		for (Composition composition : importXML) {
			for (int i = 0; i < composition.getFiles().size(); i++) {
				for (int j = 0; j < composition.getFiles().size(); j++) {
					if (i > j) {
						Fichier f1 = composition.getFiles().get(i);
						Fichier f2 = composition.getFiles().get(j);
						if (f1.getClassement().equals(f2.getClassement())
								&& StringUtils.equalsIgnoreCase(f1.getAuthor(), f2.getAuthor())) {
							String key = f1.getFileName() + ", " + f2.getFileName();
							if (!result.containsKey(key)) {
								result.put(key, 1);
							} else {
								result.put(key, result.get(key) + 1);
							}
						}
					}
				}
			}
		}
		result.keySet().stream().sorted().forEach(key -> {
			if (result.get(key) > 3) {
				addLine(text, key + ": " + result.get(key), true);
			}
		});

		LOG.debug("End findDuplicateFiles");
		return writeInFile(text, Constant.BATCH_FILE);
	}

	/**
	 * Search if there are txt files which are not convert to xml files.
	 * 
	 * @return
	 */
	public static String missingXML() {
		LOG.debug("Start missingXML");
		StringBuilder text = new StringBuilder();
		addLine(text, "MissingXML: ", true);

		// Recupère tous les nom des fichiers txt
		List<File> music = new ArrayList<>();
		FichierUtils.listFilesForFolder(new File(Constant.getMusicAbsDirectory()), music, Constant.TXT_EXTENSION, true);
		List<String> collectMusic = music.stream().map(File::getName)
				.map(s -> StringUtils.substringBeforeLast(s, Constant.TXT_EXTENSION)).collect(Collectors.toList());

		// Recupère tous les nom des fichiers xml
		List<File> xml = new ArrayList<>();
		FichierUtils.listFilesForFolder(new File(Constant.getXmlPath()), xml, Constant.XML_EXTENSION, true);
		List<String> collectXml = xml.stream().map(File::getName)
				.map(s -> StringUtils.substringBeforeLast(s, Constant.XML_EXTENSION)).collect(Collectors.toList());

		addLine(text, "TXT: ", true);
		for (String txt : collectMusic) {
			if (collectXml.stream().noneMatch(s -> StringUtils.equalsAnyIgnoreCase(s, txt))) {
				addLine(text, "Missing: " + txt, true);
				LOG.debug("Missing: " + txt);
			}
		}
		addLine(text, "XML: ", true);
		for (String xmlFile : collectXml) {
			if (collectMusic.stream().noneMatch(s -> StringUtils.equalsAnyIgnoreCase(s, xmlFile))) {
				addLine(text, "Missing: " + xmlFile, true);
				LOG.debug("Missing: " + xmlFile);
			}
		}

		LOG.debug("End missingXML");
		return writeInFile(text, Constant.BATCH_FILE);
	}

	public static String topYear(int yearBegin, int yearEnd, int albumLimit, int songLimit, boolean deleted) {
		LOG.debug("Start topYear");
		StringBuilder text = new StringBuilder();
		addLine(text, "Top Year: ", true);
		addLine(text, "Year Begin: " + yearBegin, true);
		addLine(text, "Year End: " + yearEnd, true);
		addLine(text, "Album Limit: " + albumLimit, true);
		addLine(text, "Song Limit: " + songLimit, true);

		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		for (int i = yearBegin; i <= yearEnd; i++) {
			topYear(i, importXML, albumLimit, songLimit, deleted, text);
		}
		if (yearBegin == 0 && yearEnd == 0) {
			topYear(0, importXML, albumLimit, songLimit, deleted, text);
		}

		LOG.debug("End topYear");
		return writeInFile(text, Constant.BATCH_FILE);
	}

	/**
	 * Show all the duplicates for a year and a type regardless of the artist, only
	 * based on the song or album.
	 * 
	 * @param batchPanel TODO
	 */
	private static void detectsDuplicateFinal(String type, boolean ignoreUnmergeableFiles, boolean byYear,
			StringBuilder result, BatchPanel batchPanel) {
		LOG.debug("Start detectsDuplicateFinal");
		double startTime = System.currentTimeMillis();
		final JaroWinklerDistance jaro = new JaroWinklerDistance();
		int i = 0;
		while ((!byYear && findFirstDuplicate(type, jaro, ignoreUnmergeableFiles, result, batchPanel))
				|| (byYear && detectsDuplicate(type, jaro, result))) {
			i++;
		}
		double endTime = System.currentTimeMillis();
		addLine(result, "Time: " + (endTime - startTime) / 1000 + " secondes", true);
		addLine(result, "Nombre de compositions fusionnées: " + i, true);
		LOG.debug("End detectsDuplicateFinal");
	}

	private static boolean findFirstDuplicate(String type, final JaroWinklerDistance jaro,
			boolean ignoreUnmergeableFiles, StringBuilder result, BatchPanel batchPanel) {
		LOG.debug("Start findFirstDuplicate");
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		if (!importXML.isEmpty()) {
			int size = importXML.size();
			addLine(result, "Size: " + size, true);
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					Composition c1 = importXML.get(i);
					Composition c2 = importXML.get(j);
					if ((!c1.getRecordType().toString().equals(type) || !c2.getRecordType().toString().equals(type))
							|| (ignoreUnmergeableFiles && (!c1.isCanBeMerged() || !c2.isCanBeMerged()))) {
						continue;
					}
					boolean isCriteria = true;
					if (i != j && isCriteria) {
						Composition composition1 = c1;
						Composition composition2 = c2;
						boolean similarArtist = isArtistSimilar(composition1, composition2);
						if (similarArtist) {
							String titre1 = composition1.getTitre().toLowerCase();
							String remParTitre1 = SearchUtils.removeParentheses(titre1);
							String parTitre1 = SearchUtils.removePunctuation(remParTitre1);
							String titre2 = composition2.getTitre().toLowerCase();
							String remParTitre2 = SearchUtils.removeParentheses(titre2);
							String parTitre2 = SearchUtils.removePunctuation(remParTitre2);
							boolean parTitreEqu = StringUtils.startsWithIgnoreCase(parTitre1, parTitre2)
									|| StringUtils.startsWithIgnoreCase(parTitre2, parTitre1);
							if (parTitreEqu
									&& (StringUtils.containsIgnoreCase(remParTitre1, Constant.SEPARATOR_AND)
											|| StringUtils.containsIgnoreCase(remParTitre2, Constant.SEPARATOR_AND))
									&& !StringUtils.containsIgnoreCase(remParTitre1, "/")
									&& !StringUtils.containsIgnoreCase(remParTitre2, "/")) {
								String andTitre1 = SearchUtils.removePunctuation(
										StringUtils.substringBefore(remParTitre1, Constant.SEPARATOR_AND));
								String andTitre2 = SearchUtils.removePunctuation(
										StringUtils.substringBefore(remParTitre2, Constant.SEPARATOR_AND));
								parTitre1 = andTitre1;
								parTitre2 = andTitre2;
							}
							boolean equalsJaroPar = SearchUtils.isEqualsJaro(jaro, parTitre1, parTitre2,
									Constant.SCORE_LIMIT_TITLE_FUSION);
							if (equalsJaroPar) {
								mergeTwoCompositions(importXML, i, j, result);
								LOG.debug("End findFirstDuplicate, find duplicate");
								batchPanel.displayText(
										BigDecimal.valueOf(100D).setScale(2).multiply(new BigDecimal(i))
												.divide(new BigDecimal(size), RoundingMode.HALF_UP).doubleValue() + "%",
										true);
								return true;
							}
						}
					}
				}
			}
		}
		LOG.debug("End findFirstDuplicate, no result");
		return false;
	}

	private static boolean isArtistSimilar(Composition composition1, Composition composition2) {
		String artist1 = composition1.getArtist();
		String artist2 = composition2.getArtist();
		return StringUtils.startsWithIgnoreCase(artist1, artist2) || StringUtils.startsWithIgnoreCase(artist2, artist1);
	}

	/**
	 * Show all the duplicates for a year and a type regardless of the artist, only
	 * based on the song or album.
	 */
	private static boolean detectsDuplicate(String type, final JaroWinklerDistance jaro, StringBuilder result) {
		LOG.debug("Debut detectsDuplicate");
		List<Composition> importXML = ImportXML.importXML(Constant.getFinalFilePath());
		int maxYear = importXML.stream().map(Composition::getFiles).flatMap(List::stream).map(Fichier::getPublishYear)
				.mapToInt(i -> i).max().getAsInt();
		int minYear = importXML.stream().map(Composition::getFiles).flatMap(List::stream).map(Fichier::getPublishYear)
				.mapToInt(i -> i).filter(y -> y != 0).min().getAsInt();
		for (int year = minYear; year <= maxYear; year++) {
			Map<String, String> criteria = new HashMap<>();
			criteria.put(SearchUtils.CRITERIA_CAT, Cat.YEAR.toString());
			criteria.put(SearchUtils.CRITERIA_PUBLISH_YEAR, String.valueOf(year));
			criteria.put(SearchUtils.CRITERIA_PUBLISH_YEAR_RANGE, SearchRange.EQUAL.getValue());
			criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, type);
			List<Composition> yearList = SearchUtils.search(importXML, criteria, true, SearchMethod.CONTAINS, false,
					false);
			addLine(result, "Year: " + year, true);
			addLine(result, "Size: " + yearList.size(), true);
			for (int i = 0; i < yearList.size(); i++) {
				for (int j = 0; j < yearList.size(); j++) {
					if (i < j) {
						Composition composition1 = yearList.get(i);
						Composition composition2 = yearList.get(j);
						String titre1 = composition1.getTitre();
						String titre2 = composition2.getTitre();
						String newTitre1 = SearchUtils.removePunctuation(titre1);
						String newTitre2 = SearchUtils.removePunctuation(titre2);
						String artist1 = composition1.getArtist();
						String artist2 = composition2.getArtist();
						Integer publishYear1 = composition1.getFiles().get(0).getPublishYear();
						Integer publishYear2 = composition2.getFiles().get(0).getPublishYear();
						boolean similarArtist = StringUtils.startsWithIgnoreCase(artist1, artist2)
								|| StringUtils.startsWithIgnoreCase(artist2, artist1);
						boolean equalsJaroPar = publishYear1.equals(publishYear2) && (SearchUtils.isEqualsJaro(jaro,
								newTitre1, newTitre2, Constant.SCORE_LIMIT_TITLE_FUSION)
								|| StringUtils.startsWithIgnoreCase(titre1, titre2)
								|| StringUtils.startsWithIgnoreCase(newTitre1, newTitre2)) && similarArtist;
						if (equalsJaroPar) {
							mergeTwoCompositions(yearList, i, j, result);
							LOG.debug("End detectsDuplicate, find duplicate");
							return true;
						}
					}
				}
			}
		}
		LOG.debug("Fin detectsDuplicate");
		return false;
	}

	private static void mergeTwoCompositions(List<Composition> importXML, int index1, int index2,
			StringBuilder result) {
		LOG.debug("Start mergeTwoCompositions");
		Composition c1 = importXML.get(index1);
		List<Fichier> files1 = c1.getFiles();
		Composition c2 = importXML.get(index2);

		boolean isDeleted = c1.isDeleted() || c2.isDeleted();
		c1.setDeleted(isDeleted);
		c2.setDeleted(isDeleted);

		addLine(result, "i: " + index1, true);
		addLine(result, "j: " + index2, true);
		addLine(result, "c1: " + c1, true);
		addLine(result, "c2: " + c2, true);
		Composition tempC2 = new Composition(c2);
		c2.getFiles().addAll(files1);
		if (((c1.getFiles().size() >= c2.getFiles().size()
				&& !StringUtils.containsIgnoreCase(c1.getArtist(), Constant.SEPARATOR_AND))
				|| StringUtils.containsIgnoreCase(c2.getArtist(), Constant.SEPARATOR_AND))) {
			c2.setArtist(c1.getArtist());
			c2.setTitre(c1.getTitre());
			try {
				CompositionUtils.modifyCompositionsInFiles(tempC2, c1.getArtist(), c1.getTitre(),
						c1.getRecordType().toString(), isDeleted);
			} catch (MyException e) {
				addLine(result, "Erreur modif compo" + e.getMessage(), true);
				LOG.error("Erreur modif compo", e);
			}
		} else {
			try {
				CompositionUtils.modifyCompositionsInFiles(c1, tempC2.getArtist(), tempC2.getTitre(),
						c1.getRecordType().toString(), isDeleted);
			} catch (MyException e) {
				addLine(result, "Erreur modif compo" + e.getMessage(), true);
				LOG.error("Erreur modif compo", e);
			}
		}
		importXML.remove(c1);
		try {
			ExportXML.exportXML(importXML, Constant.getFinalFile());
		} catch (IOException e) {
			LOG.error("Error !!", e);
		}
		addLine(result, "Final size: " + importXML.size(), true);
		LOG.debug("End mergeTwoCompositions");
	}

	/**
	 * Generates the top excel files of a year.
	 * 
	 * @param list
	 * @param deleted if true all compositions, false only not deleted compositions
	 */
	private static void topYear(int yearTop, List<Composition> list, int albumLimit, int songLimit, boolean deleted,
			StringBuilder result) {
		LOG.debug("Start topYear");
		List<String> files = new ArrayList<>();
		String year = String.valueOf(yearTop);
		addLine(result, "Year: " + year, true);
		files.add(topOccurence(list, year, deleted));
		files.add(topRecords(list, RecordType.SONG, "Top Songs", songLimit, deleted, year));
		files.add(topRecords(list, RecordType.ALBUM, "Top Albums", albumLimit, deleted, year));
		files.add(topRecordsByPoints(list, RecordType.SONG, "Points Songs", deleted, year));
		files.add(topRecordsByPoints(list, RecordType.ALBUM, "Points Albums", deleted, year));
		files.add(topSongsParPublication(list, year, deleted));
		moveFilesInFolder(files, new File(Constant.getOutputDir() + "Top by Year" + FileUtils.FS + year), result);
		LOG.debug("End topYear");
	}

	private static void moveFilesInFolder(List<String> files, File folder, StringBuilder result) {
		FichierUtils.createFolderIfNotExists(folder.getAbsolutePath());
		Path pathFolder = folder.toPath();
		files.stream().forEach(f -> {
			Path pathFile = new File(f).toPath();
			try {
				Files.move(pathFile, pathFolder.resolve(pathFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				addLine(result, "Error while moving file: " + f + " " + e.getMessage(), true);
				LOG.error("Error while moving file: " + f, e);
			}
		});
	}

	/**
	 * Generates csv file with each top 10 songs of every publication.
	 * 
	 * @param list
	 * @param year
	 * @param deleted if true all compositions, false only not deleted compositions
	 */
	private static String topSongsParPublication(List<Composition> list, String year, boolean deleted) {
		List<String> authors = Arrays.asList(OngletPanel.getAuthorList());
		List<List<String>> result = new ArrayList<>();
		for (String author : authors) {
			Map<String, String> criteria = new HashMap<>();
			criteria.put(SearchUtils.CRITERIA_CAT, Cat.YEAR.toString());
			if (!"0".equals(year)) {
				criteria.put(SearchUtils.CRITERIA_DATE_BEGIN, year);
				criteria.put(SearchUtils.CRITERIA_DATE_END, year);
			}
			criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, RecordType.SONG.toString());
			criteria.put(SearchUtils.CRITERIA_AUTHOR, author);
			List<Composition> yearList = SearchUtils.search(list, criteria, true, SearchMethod.CONTAINS, deleted,
					false);
			if (yearList.isEmpty() || !yearList.get(0).getFiles().get(0).getSorted()) {
				// If no file for the given author and year or if the file is not sorted
				continue;
			}
			List<List<String>> temp = new ArrayList<>();
			for (int i = 0; i < yearList.size(); i++) {
				List<String> row = new ArrayList<>();
				Composition composition = yearList.get(i);
				if (composition.getFiles().get(0).getClassement() <= 10) {
					row.add(composition.getArtist());
					row.add(composition.getTitre());
					row.add(String.valueOf(composition.getFiles().get(0).getClassement()));
					if ("0".equals(year)) {
						row.add(String.valueOf(composition.getFiles().get(0).getRangeDateBegin()));
					}
					row.add(String.valueOf(composition.isDeleted()));
					temp.add(row);
				}
			}
			temp.add(0, initList(author, "-1"));
			temp.add(0, initList("", "-2"));
			if (temp.size() > 2) {
				List<List<String>> hello = temp.stream()
						.sorted((e1, e2) -> Integer.valueOf(e1.get(2)).compareTo(Integer.valueOf(e2.get(2))))
						.collect(Collectors.toList());
				result.addAll(hello);
			}
		}
		for (List<String> strings : result) {
			if (StringUtils.isBlank(strings.get(1))) {
				strings.set(2, "");
			}
		}
		String[] header = { "Artiste", "Titre", "Classement", "Supprimé" };
		if ("0".equals(year)) {
			String[] tmp = { "Artiste", "Titre", "Classement", "Année", "Supprimé" };
			header = tmp;
		}
		return CsvFile.exportCsv("Top Songs Par Publication - " + year, result, null, header);
	}

	private static List<String> initList(String author, String value) {
		List<String> row = new ArrayList<>();
		row.add(author);
		row.add("");
		row.add(value);
		return row;
	}

	/**
	 * Top songs or top albums.
	 * 
	 * @param list
	 * @param type Album or Song
	 * @param fileName the name of the result csv file
	 * @param limit the minimim of number of occurence a Composition to have to be
	 *            in the result file
	 * @param deleted if true all compositions, false only not deleted compositions
	 * @param year the year of the top
	 * @param score
	 */
	private static String topRecords(List<Composition> list, RecordType type, String fileName, int limit,
			boolean deleted, String year) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put(SearchUtils.CRITERIA_CAT, Cat.YEAR.toString());
		if (!"0".equals(year)) {
			criteria.put(SearchUtils.CRITERIA_DATE_BEGIN, year);
			criteria.put(SearchUtils.CRITERIA_DATE_END, year);
		}
		criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, type.toString());
		List<Composition> yearList = SearchUtils.search(list, criteria, true, SearchMethod.CONTAINS, deleted, false);
		List<Vector<Object>> occurenceListTemp = CompositionUtils
				.convertCompositionListToVector(yearList, null, false, true, false, true, false).stream()
				.filter(c -> (int) c.get(3) >= limit).collect(Collectors.toList());
		Vector<Vector<Object>> occurenceList = new Vector<>();
		for (Vector<Object> vector : occurenceListTemp) {
			occurenceList.add(vector);
		}
		String[] csvHeader = { "Artiste", "Titre", "Type", "Nombre de fichiers", "Score", "Décile", "Supprimé",
				"Year: " + year + " Type: " + type.toString() };
		return CsvFile.exportCsv(fileName + " - " + year, MiscUtils.convertVectorToList(occurenceList),
				Arrays.asList(new SortKey(3, SortOrder.DESCENDING), new SortKey(4, SortOrder.DESCENDING)), csvHeader);
	}

	/**
	 * Top songs or top albums with a sytem of points.
	 * 
	 * @param list
	 * @param type Album or Song
	 * @param fileName the name of the result csv file
	 * @param deleted if true all compositions, false only not deleted compositions
	 * @param year the year of the top
	 */
	private static String topRecordsByPoints(List<Composition> list, RecordType type, String fileName, boolean deleted,
			String year) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put(SearchUtils.CRITERIA_CAT, Cat.YEAR.toString());
		if (!"0".equals(year)) {
			criteria.put(SearchUtils.CRITERIA_DATE_BEGIN, year);
			criteria.put(SearchUtils.CRITERIA_DATE_END, year);
		}
		criteria.put(SearchUtils.CRITERIA_RECORD_TYPE, type.toString());
		criteria.put(SearchUtils.CRITERIA_SORTED, Boolean.TRUE.toString());
		List<Composition> yearList = SearchUtils.search(list, criteria, true, SearchMethod.CONTAINS, deleted, false);
		List<List<String>> occurenceList = new ArrayList<>();
		if (yearList.stream().map(Composition::getFiles).flatMap(List::stream).map(Fichier::getAuthor)
				.map(WordUtils::capitalize).distinct().count() > 2) {
			for (Composition composition : yearList) {
				List<String> row = new ArrayList<>();
				row.add(composition.getArtist());
				row.add(composition.getTitre());
				if (!"0".equals(year)) {
					row.add(composition.getRecordType().toString());
				} else {
					row.add(String.valueOf(composition.getFiles().get(0).getRangeDateBegin()));
				}
				Integer points = composition.getFiles().stream().map(Fichier::getClassement).filter(rank -> rank < 10)
						.reduce(0, (a, b) -> a + 20 * (11 - b));
				if (points > 0) {
					row.add(String.valueOf(points));
					occurenceList.add(row);
				}
				row.add(String.valueOf(composition.isDeleted()));
			}
		}
		String[] csvHeader = { "Artiste", "Titre", "Type", "Score", "Supprimé",
				"Year: " + year + " Type: " + type.toString() };
		return CsvFile.exportCsv(fileName + " - " + year, occurenceList,
				Arrays.asList(new SortKey(3, SortOrder.DESCENDING)), csvHeader);
	}

	/**
	 * Generates csv file with the most occurence of an artist, songs and albums
	 * combine. Limited with minimum 10 total occurences.
	 * 
	 * @param list
	 * @param year
	 * @param deleted if true all compositions, false only not deleted compositions
	 */
	private static String topOccurence(List<Composition> list, String year, boolean deleted) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put(SearchUtils.CRITERIA_CAT, Cat.YEAR.toString());
		if (!"0".equals(year)) {
			criteria.put(SearchUtils.CRITERIA_DATE_BEGIN, year);
			criteria.put(SearchUtils.CRITERIA_DATE_END, year);
		}
		List<Composition> yearList = SearchUtils.search(list, criteria, true, SearchMethod.CONTAINS, deleted, false);
		List<Vector<Object>> occurenceListTemp = CompositionUtils
				.convertArtistPanelResultToVector(CompositionUtils.groupCompositionByArtist(yearList), false).stream()
				.filter(c -> (int) c.get(1) > 9).collect(Collectors.toList());
		Vector<Vector<Object>> occurenceList = new Vector<>();
		for (Vector<Object> vector : occurenceListTemp) {
			occurenceList.add(vector);
		}
		String[] csvHeader = { "Artiste", "Nombre d'Occurrences", "Albums", "Chansons", "% De Supprimés", "Score Total",
				"Score Album", "Score Chanson", "Score Supprimés", "Year: " + year };
		return CsvFile.exportCsv("Top Occurence - " + year, MiscUtils.convertVectorToList(occurenceList),
				Arrays.asList(new SortKey(1, SortOrder.DESCENDING), new SortKey(5, SortOrder.DESCENDING)), csvHeader);
	}

	private static String writeInFile(StringBuilder sb, String fileName) {
		String filePath = Constant.getOutputDir() + FileUtils.FS + fileName;
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(filePath), Constant.ANSI_ENCODING));) {
			writer.append(sb);
			writer.flush();
		} catch (IOException e) {
			LOG.error("Error when write batch result in: " + filePath, e);
		}
		return filePath;
	}

	private static void addLine(StringBuilder sb, String text, boolean displayTime) {
		sb.append(displayTime ? MiscUtils.getCurrentTime() : "").append(displayTime ? ": " : "").append(text)
				.append(Constant.NEW_LINE);
	}

	private BatchUtils() {
		// Nothing to do
	}
}
