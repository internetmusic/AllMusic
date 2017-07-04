package pmb.music.AllMusic.view;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import pmb.music.AllMusic.utils.Constant;

public class BasicFrame extends JFrame{
    private static final long serialVersionUID = 3539279683629505967L;
    private Onglet tab;
    private MenuPanel menuPanel;
    
    public BasicFrame(){
        super(Constant.DEFAULT_TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTab(new Onglet(this));
        this.getContentPane().add(tab,BorderLayout.EAST);
        associateMenuPanel();
    }
    
    public void associateMenuPanel(){
        this.setMenuPanel(new MenuPanel(this));
        this.getContentPane().add(getMenuPanel(),BorderLayout.WEST);
        pack();
        setVisible(true);
    }
    
    public void setTab(final Onglet tab) {
        this.tab = tab;
    }
    
    public Onglet getTab() {
        return tab;
    }   
    
    public MenuPanel getMenuPanel(){
        return menuPanel;
    }
    
    public void setMenuPanel(final MenuPanel myMenuPanel){
        menuPanel=myMenuPanel;
    }
}
