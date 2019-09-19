/**
 *
 */
package pmb.music.AllMusic.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang3.StringUtils;

import pmb.music.AllMusic.model.Cat;
import pmb.music.AllMusic.model.RecordType;
import pmb.music.AllMusic.utils.Constant;
import pmb.music.AllMusic.view.ColumnIndex.Index;
import pmb.music.AllMusic.view.model.AbstractModel;

/**
 * Classe decrivant le style des tableaux de l'application. Colorise une ligne
 * sur deux.
 *
 * @author pmbroca
 * @see DefaultTableCellRenderer
 */
public class EvenOddRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 7366600520755781512L;
    private static final Color BLUE = new Color(47, 129, 210);
    /**
     * Normal row selected.
     */
    private static final Color DARK_BLUE = new Color(33, 93, 153);
    private static final Color GRAY = new Color(238, 229, 222);
    /**
     * Deleted not selected
     */
    private static final Color GREEN = new Color(10, 208, 111);
    /**
     * Deleted selected.
     */
    private static final Color DARK_GREEN = new Color(7, 145, 77);
    /**
     * ALBUM color.
     */
    private static final Color YELLOW = new Color(206, 200, 66);
    /**
     * SONG color.
     */
    private static final Color RED = new Color(255, 143, 143);
    /**
     * UNKNOWN/YEAR color.
     */
    private static final Color PURPLE = new Color(216, 150, 255);
    private static final Color GENRE = new Color(167, 196, 76);
    private static final Color ALL_TIME = new Color(246, 132, 27);
    private static final Color THEME = new Color(0, 174, 219);
    private static final Color LONG_PERIOD = new Color(36, 92, 72);
    private static final Color DECADE = new Color(120, 25, 70);

    private static final Color SORTED = new Color(251, 224, 131);

    private static final Color[] DECILE_SCORE_PURPLE = { new Color(243, 233, 252), new Color(219, 191, 246),
            new Color(196, 149, 240), new Color(173, 106, 234), new Color(149, 64, 228), new Color(138, 43, 226),
            new Color(110, 34, 180), new Color(82, 25, 135), new Color(55, 17, 90), new Color(27, 8, 45) };

    private ColumnIndex index;

    /**
     * Constructor for {@link EvenOddRenderer}.
     *
     * @param index.get(Index.DELETED) index of the deleted column, used to draw deleted row
     *            with specific color
     * @param index.get(Index.TYPE) index of the record type column, used to color record type
     *            cell depending on the type
     * @param index.get(Index.CAT) index of the category column, used to color category cell
     * @param index.get(Index.DECILE) index of the decile column, used to add tooltip
     * @param index.get(Index.SCORE) index of the score column, used to add color scale of
     *            purple
     * @param index.get(Index.SORTED) index of the sorted column, used to add color if sorted
     * @param index.get(Index.RANK) index of the rank column, used to add color if sorted
     */
    public EvenOddRenderer(ColumnIndex index) {
        this.index = index;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        value = formatValue(value);
        setTooltip(table, value, row, column);

        Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        table.setBorder(noFocusBorder);
        if (isSelected) {
            setBorder(new MatteBorder(1, 0, 1, 0, Color.black));
        }
        Font font = renderer.getFont();
        Boolean rowDeleted = false;
        if (index.get(Index.DELETED) != null) {
            // If displayed row is a deleted row
            rowDeleted = Boolean.valueOf((String) getValueByColumnIndex(table, row, index.get(Index.DELETED)));
        }

        Color foreground;
        Color background;
        if (index.get(Index.TYPE) != null && column == index.get(Index.TYPE)) {
            foreground = getTypeRenderer(table, value, isSelected, row, renderer, font, rowDeleted);
            background = getDefaultBackground(isSelected, row, rowDeleted);
        } else if (index.get(Index.CAT) != null && column == index.get(Index.CAT)) {
            foreground = getCatRenderer(table, value, isSelected, row, renderer, rowDeleted, font);
            background = getDefaultBackground(isSelected, row, rowDeleted);
        } else if (index.get(Index.SCORE) != null && index.get(Index.DECILE) != null && column == index.get(Index.SCORE)) {
            return getScoreRenderer(table, isSelected, row, renderer, font);
        } else if (index.get(Index.SORTED) != null
                && (column == index.get(Index.SORTED) - 1 || column == index.get(Index.SORTED) || (index.get(Index.RANK) != null && column == index.get(Index.RANK)))) {
            return getSortRenderer(table, row, renderer);
        } else {
            foreground = getDefaultForeground(isSelected, row, rowDeleted);
            background = getDefaultBackground(isSelected, row, rowDeleted);
        }

        renderer.setForeground(foreground);
        renderer.setBackground(background);
        return renderer;
    }

    private Color getTypeRenderer(JTable table, Object value, boolean isSelected, int row, Component renderer,
            Font font, Boolean rowDeleted) {
        Color foreground;
        // If display a row with record type
        RecordType type = RecordType.getByValue((String) getValueByColumnIndex(table, row, index.get(Index.TYPE)));
        renderer.setFont(new Font(font.getName(), font.getStyle(), font.getSize() + 5));
        if (type.getRecordType().equals(value) && !Boolean.TRUE.equals(rowDeleted)) {
            // only the record type cell is colored
            switch (type) {
            case ALBUM:
                foreground = YELLOW;
                break;
            case SONG:
                foreground = RED;
                break;
            default:
                foreground = PURPLE;
                break;
            }
        } else {
            foreground = getDefaultForeground(isSelected, row, rowDeleted);
        }
        return foreground;
    }

    private Color getCatRenderer(JTable table, Object value, boolean isSelected, int row, Component renderer,
            Boolean rowDeleted, Font font) {
        Color foreground;
        // If display a row with cat
        Cat cat = Cat.getByValue((String) getValueByColumnIndex(table, row, index.get(Index.CAT)));
        renderer.setFont(new Font(font.getName(), font.getStyle(), font.getSize() + 5));
        if (cat.getCat().equals(value) && !Boolean.TRUE.equals(rowDeleted)) {
            // only the cat cell is colored
            switch (cat) {
            case YEAR:
                foreground = PURPLE;
                break;
            case DECADE:
                foreground = DECADE;
                break;
            case LONG_PERIOD:
                foreground = LONG_PERIOD;
                break;
            case THEME:
                foreground = THEME;
                break;
            case GENRE:
                foreground = GENRE;
                break;
            case ALL_TIME:
                foreground = ALL_TIME;
                break;
            default:
                foreground = row % 2 != 0 ? GRAY : BLUE;
                break;
            }
        } else {
            foreground = getDefaultForeground(isSelected, row, rowDeleted);
        }
        return foreground;
    }

    private Component getScoreRenderer(JTable table, boolean isSelected, int row, Component renderer, Font font) {
        // If display a row with score
        Integer decile = (Integer) (getValueByColumnIndex(table, row, index.get(Index.DECILE)));
        renderer.setFont(new Font(font.getName(), font.getStyle(), font.getSize() + 5));
        if (decile != 0) {
            // only the score cell is colored
            Color foreground;
            if (isSelected) {
                foreground = DARK_BLUE;
            } else if (decile <= 2) {
                foreground = BLUE;
            } else {
                foreground = row % 2 == 0 ? BLUE : GRAY;
            }
            renderer.setForeground(foreground);
            renderer.setBackground(DECILE_SCORE_PURPLE[decile - 1]);
        }
        return renderer;
    }

    private Component getSortRenderer(JTable table, int row, Component renderer) {
        String sortedString = (String) getValueByColumnIndex(table, row, index.get(Index.SORTED));
        Boolean sorted = StringUtils.equalsIgnoreCase(sortedString, "oui") ? Boolean.TRUE : Boolean.FALSE;
        renderer.setForeground(sorted || row % 2 == 0 ? BLUE : GRAY);
        Color background;
        if (sorted) {
            background = SORTED;
        } else {
            background = row % 2 == 0 ? GRAY : BLUE;
        }
        renderer.setBackground(background);
        return renderer;
    }

    private static Color getDefaultBackground(boolean isSelected, int row, Boolean rowDeleted) {
        Color background;
        if (rowDeleted && isSelected) {
            background = DARK_GREEN;
        } else if (!rowDeleted && isSelected) {
            background = DARK_BLUE;
        } else if (rowDeleted) {
            background = GREEN;
        } else if (row % 2 == 0) {
            background = GRAY;
        } else {
            background = BLUE;
        }
        return background;
    }

    private static Color getDefaultForeground(boolean isSelected, int row, Boolean rowDeleted) {
        Color foreground;
        if (isSelected) {
            foreground = Color.BLACK;
        } else if (!rowDeleted && row % 2 == 0) {
            foreground = BLUE;
        } else {
            foreground = GRAY;
        }
        return foreground;
    }

    private void setTooltip(JTable table, Object value, int row, int column) {
        if (value instanceof String && ((String) value).length() > 30) {
            // If value is a long string
            setToolTipText((String) value);
        } else if (index.get(Index.DECILE) != null && index.get(Index.SCORE) != null && column == index.get(Index.SCORE)) {
            // if decile
            setToolTipText(String.valueOf(getValueByColumnIndex(table, row, index.get(Index.DECILE))) + " / 10");
        } else {
            setToolTipText(null);
        }
    }

    private static Object formatValue(Object value) {
        if (value instanceof Number) {
            // format number
            value = NumberFormat.getNumberInstance().format(value);
        } else if (value instanceof LocalDateTime) {
            // format date
            value = new Constant().getDateDTF().format((LocalDateTime) value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Object getValueByColumnIndex(JTable table, int row, int index) {
        return ((Vector<Object>) ((AbstractModel) table.getModel()).getDataVector()
                .get(table.getRowSorter().convertRowIndexToModel(row))).get(index);
    }
}
