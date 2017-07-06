package pmb.music.AllMusic;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import pmb.music.AllMusic.view.BasicFrame;

/**
 * Hello world!
 * 
 */
public class AllMusic {

	private static final Logger LOG = Logger.getLogger(AllMusic.class);
	
	/**
	 * La méthode d'entrée du programme.
	 * @param args 
	 */
	public static void main(String[] args) {

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				// Si je veux ajouter un traitement qui se lance quand l'appli se ferme
			}
		}));
		try {
			UIManager.setLookAndFeel("com.jtattoo.plaf.aluminium.AluminiumLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			LOG.error("", e);
		}
		
		final BasicFrame f = new BasicFrame();
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dim.height = 19 * dim.height / 20;
		// f.setPreferredSize(dim);
		f.setExtendedState(JFrame.MAXIMIZED_BOTH);
		try {
			f.setLocation(null);
		} catch (NullPointerException e) {
			LOG.debug("",e);
		}
		f.pack();
		f.setVisible(true);
	}
}
