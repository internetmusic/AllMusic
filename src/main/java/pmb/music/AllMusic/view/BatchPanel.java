package pmb.music.AllMusic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;

import pmb.music.AllMusic.utils.BatchUtils;
import pmb.music.AllMusic.utils.Constant;

/**
 * Onglet pour lancer des traitements.
 * Batchs List: 
 * - Find duplicate compositions: FDC
 * - Find duplicate files: FDF 
 * - Missing XML files: MXF
 * - Top year: top
 * - Find suspicious compositions: FSC
 * - Nettoyer dossier historique
 * @author PBR
 */
public class BatchPanel extends JPanel {
	private static final long serialVersionUID = -7659089306956006760L;
	private static final Logger LOG = Logger.getLogger(BatchPanel.class);
	
	/**
	 * Les messages.
	 */
	private JTextArea resultLabel;
	private List<String> resultLabelData;
	
	public BatchPanel() {
		super();
		LOG.debug("Start BatchPanel");
		this.setLayout(new GridLayout(10, 1));

		findDuplicateComposition();
		
		lastLine();
		
		LOG.debug("End BatchPanel");
	}

	/**
	 * Initialise les composants pour trouver les compositions en double (FDC).
	 */
	private void findDuplicateComposition() {
		JPanel fdc = new JPanel();

		JLabel fdcLabel = new JLabel("Recherche les compositions en double: ");
		fdc.add(fdcLabel);

		// Checkbox song
		JPanel fdcSongPanel = new JPanel();
		fdcSongPanel.setPreferredSize(new Dimension(200, 60));
		JLabel fdcSongLabel = new JLabel("Chanson: ");
		JCheckBox fdcSong = new JCheckBox();
		fdcSong.setPreferredSize(new Dimension(130, 25));
		fdcSong.setSelected(true);
		fdcSongPanel.add(fdcSongLabel);
		fdcSongPanel.add(fdcSong);
		fdc.add(fdcSongPanel);

		// Checkbox album
		JPanel fdcAlbumPanel = new JPanel();
		fdcAlbumPanel.setPreferredSize(new Dimension(200, 60));
		JLabel fdcAlbumLabel = new JLabel("Album: ");
		JCheckBox fdcAlbum = new JCheckBox();
		fdcAlbum.setPreferredSize(new Dimension(130, 25));
		fdcAlbum.setSelected(true);
		fdcAlbumPanel.add(fdcAlbumLabel);
		fdcAlbumPanel.add(fdcAlbum);
		fdc.add(fdcAlbumPanel);

		// Checkbox unmergeable
		JPanel fdcUnmergeablePanel = new JPanel();
		fdcUnmergeablePanel.setPreferredSize(new Dimension(300, 60));
		JLabel fdcUnmergeableLabel = new JLabel("Ignorer les fichier non mergeables: ");
		JCheckBox fdcUnmergeable = new JCheckBox();
		fdcUnmergeable.setSelected(true);
		fdcUnmergeablePanel.add(fdcUnmergeableLabel);
		fdcUnmergeablePanel.add(fdcUnmergeable);
		fdc.add(fdcUnmergeablePanel);

		JButton fdcBtn = new JButton("Go");
		fdcBtn.setToolTipText("Fusionne les compositions identiques mais non détectées à la fusion classique.");
		fdcBtn.addActionListener((ActionEvent arg0) -> {
			displayText("Start findDuplicateComposition: " + BatchUtils.getCurrentTime());
			new Thread(() -> {
				BatchUtils.detectsDuplicateFinal(fdcSong.isSelected(), fdcAlbum.isSelected(),
						fdcUnmergeable.isSelected());
				displayText("End findDuplicateComposition: " + BatchUtils.getCurrentTime());
			}).start();
		});
		fdc.add(fdcBtn);

		this.add(fdc);
	}

	/**
	 * Initialise la dernière ligne de composant.
	 */
	private void lastLine() {
		JPanel lastLine = new JPanel(new GridLayout(0, 2));
		// result
		JPanel resultPanel = new JPanel(new BorderLayout());
		resultLabel = new JTextArea();
		resultLabel.setWrapStyleWord(true);
		resultLabel.setLineWrap(true);
		resultLabel.setOpaque(false);
		resultLabel.setEditable(false);
		resultLabel.setBackground(UIManager.getColor("Label.background"));
		resultLabel.setFont(UIManager.getFont("Label.font"));
		resultLabel.setBorder(UIManager.getBorder("Label.border"));
		resultPanel.add(new JScrollPane(resultLabel), BorderLayout.CENTER);
		lastLine.add(resultPanel);

		JPanel btnPanel = new JPanel();
		JButton batcFileBtn = new JButton("Ouvrir le fichier de résultat");
		batcFileBtn.addActionListener((ActionEvent arg0) -> openResultFileInNotepad());
		btnPanel.add(batcFileBtn);
		
		JButton clearBtn = new JButton("Vider la zone de résultat");
		clearBtn.addActionListener((ActionEvent arg0) -> {
			resultLabelData = null;
			displayText("");
		});
		btnPanel.add(clearBtn);
		
		lastLine.add(btnPanel);
		this.add(lastLine);
	}
	
	/**
	 * Ouvrir le fichier de resultat dans notepad.
	 */
	private void openResultFileInNotepad() {
		LOG.debug("Start openResultFileInNotepad");
		if (StringUtils.isNotBlank(Constant.BATCH_FILE_PATH)) {
			try {
				if (FileUtils.fileExists(Constant.BATCH_FILE_PATH)) {
					Runtime.getRuntime().exec(Constant.NOTEPAD_PATH + Constant.BATCH_FILE_PATH);
				}
			} catch (IOException e) {
				LOG.error("Erreur lors de l'ouverture du fichier: " + Constant.BATCH_FILE_PATH, e);
				displayText(e.toString());
			}
		}
		LOG.debug("End openResultFileInNotepad");
	}

	/**
	 * Ajoute un message dans la zone de texte resultLabel.
	 * 
	 * @param text un nouveau texte à afficher
	 */
	private void displayText(String text) {
		LOG.debug("Start displayText");
		if(resultLabelData == null) {
			resultLabelData = new ArrayList<>();
		}
		StringBuilder s = new StringBuilder();
		resultLabelData.add(text);
		for (String string : resultLabelData) {
			s.append(string).append(Constant.NEW_LINE);
		}
		resultLabel.setText(s.toString());
		resultLabel.setForeground(new Color(243, 16, 16));
		Font labelFont = resultLabel.getFont();
		resultLabel.setFont(new Font(labelFont.getName(), labelFont.getStyle(), 20));
		LOG.debug("End displayText");
	}
}
