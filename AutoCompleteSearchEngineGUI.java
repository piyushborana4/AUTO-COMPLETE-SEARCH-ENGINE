import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutoCompleteSearchEngineGUI {
    private final Trie trie = new Trie();
    private final WordFrequencyTable frequencies =
            new WordFrequencyTable(101);

    private final JTextField prefixField = new JTextField(24);
    private final JTextField newWordField = new JTextField(18);

    private final DefaultListModel<Suggestion> suggestionModel =
            new DefaultListModel<>();

    private final JList<Suggestion> suggestionList =
            new JList<>(suggestionModel);

    private final JLabel statusLabel =
            new JLabel("Enter a prefix to begin. All counts start at 0.");

    private final JLabel countLabel = new JLabel();

    public AutoCompleteSearchEngineGUI() {
        addStarterWords();
        buildWindow();
        updateCount();
    }

    private void addStarterWords() {
        String[] words = {
            "apple", "application", "apply", "appreciate",
            "banana", "band", "bandwidth",
            "cat", "catalog", "category",
            "computer", "computing", "compile",
            "data", "database", "data structure",
            "graph", "hash table", "java", "javascript",
            "tree", "trie"
        };

        for (String word : words) {
            addWord(word);
        }
    }

    private void buildWindow() {
        JFrame frame = new JFrame("Auto-Complete Search Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(680, 520);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(18, 20, 18, 20));
        mainPanel.setBackground(new Color(245, 248, 251));

        JLabel title = new JLabel("Auto-Complete Search Engine");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(new Color(25, 55, 84));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 5, 6, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        centerPanel.add(new JLabel("Search prefix:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        centerPanel.add(prefixField, constraints);

        JButton searchButton = new JButton("Search");
        constraints.gridx = 2;
        constraints.weightx = 0;
        centerPanel.add(searchButton, constraints);

        suggestionList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(new Font("SansSerif", Font.PLAIN, 16));
        suggestionList.setFixedCellHeight(32);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;

        centerPanel.add(new JScrollPane(suggestionList), constraints);

        JButton selectButton =
                new JButton("Select Highlighted Suggestion");

        constraints.gridy = 2;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(selectButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        centerPanel.add(new JLabel("Add word:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        centerPanel.add(newWordField, constraints);

        JButton addButton = new JButton("Add");
        constraints.gridx = 2;
        constraints.weightx = 0;
        centerPanel.add(addButton, constraints);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 4));
        footer.setOpaque(false);

        countLabel.setForeground(new Color(25, 55, 84));
        footer.add(countLabel, BorderLayout.WEST);

        statusLabel.setForeground(new Color(0, 115, 125));
        footer.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(footer, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        prefixField.addActionListener(event -> search());
        selectButton.addActionListener(event -> selectSuggestion());
        addButton.addActionListener(event -> addFromField());
        newWordField.addActionListener(event -> addFromField());

        suggestionList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()
                    && suggestionList.getSelectedValue() != null) {
                statusLabel.setText(
                        "Selected suggestion: "
                        + suggestionList.getSelectedValue().word);
            }
        });

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private void search() {
        String prefix = normalize(prefixField.getText());
        List<Suggestion> suggestions = suggest(prefix, 10);

        suggestionModel.clear();

        for (Suggestion suggestion : suggestions) {
            suggestionModel.addElement(suggestion);
        }

        if (suggestions.isEmpty()) {
            statusLabel.setText("No matching words found.");
        } else {
            statusLabel.setText(
                    suggestions.size()
                    + " suggestion(s). Select one from the list.");
        }
    }

    private void selectSuggestion() {
        Suggestion selected = suggestionList.getSelectedValue();

        if (selected == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "Choose a suggestion from the list first.",
                    "No selection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        recordSelection(selected.word);

        String message = "Selected \"" + selected.word
                + "\". Its count is now "
                + frequencies.get(selected.word) + ".";

        prefixField.setText("");
        suggestionModel.clear();
        suggestionList.clearSelection();

        statusLabel.setText(message);
        prefixField.requestFocusInWindow();
    }

    private void addFromField() {
        String rawWord = newWordField.getText();
        String word = normalize(rawWord);

        if (word.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Enter a word containing letters or digits.",
                    "Invalid word",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean alreadyExists = trie.contains(word);
        addWord(rawWord);
        newWordField.setText("");
        updateCount();
        search();

        if (alreadyExists) {
            statusLabel.setText("That word is already in the dictionary.");
        } else {
            statusLabel.setText("Added \"" + word + "\" with count 0.");
        }
    }

    private void updateCount() {
        countLabel.setText(
                "Dictionary words: " + trie.wordCount());
    }

    private void addWord(String rawWord) {
        String word = normalize(rawWord);

        if (word.isEmpty()) {
            return;
        }

        trie.insert(word);
        frequencies.ensureWord(word);
    }

    private boolean recordSelection(String rawWord) {
        String word = normalize(rawWord);

        if (!trie.contains(word)) {
            return false;
        }

        frequencies.increment(word);
        return true;
    }

    private List<Suggestion> suggest(
            String rawPrefix, int limit) {

        String prefix = normalize(rawPrefix);
        List<String> matchingWords =
                trie.wordsWithPrefix(prefix);

        List<Suggestion> results = new ArrayList<>();

        for (String word : matchingWords) {
            results.add(
                    new Suggestion(word, frequencies.get(word)));
        }

        // Selection sort: highest count first,
        // then alphabetical order for ties.
        for (int i = 0; i < results.size(); i++) {
            int best = i;

            for (int j = i + 1; j < results.size(); j++) {
                Suggestion candidate = results.get(j);
                Suggestion currentBest = results.get(best);

                if (candidate.frequency > currentBest.frequency
                        || (candidate.frequency
                            == currentBest.frequency
                            && candidate.word.compareTo(
                                    currentBest.word) < 0)) {
                    best = j;
                }
            }

            Suggestion temp = results.get(i);
            results.set(i, results.get(best));
            results.set(best, temp);
        }

        if (limit < results.size()) {
            return new ArrayList<>(
                    results.subList(0, Math.max(0, limit)));
        }

        return results;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9'-]+", "");
    }

    private static final class Suggestion {
        final String word;
        final int frequency;

        Suggestion(String word, int frequency) {
            this.word = word;
            this.frequency = frequency;
        }

        @Override
        public String toString() {
            return word + "     | selected "
                    + frequency + " time(s)";
        }
    }

    // 38 child slots: a-z, 0-9, apostrophe, and hyphen.
    private static final class Trie {
        private static final int ALPHABET_SIZE = 38;

        private final Node root = new Node();
        private int size;

        void insert(String word) {
            Node current = root;

            for (int i = 0; i < word.length(); i++) {
                int childIndex = index(word.charAt(i));

                if (childIndex < 0) {
                    continue;
                }

                if (current.children[childIndex] == null) {
                    current.children[childIndex] = new Node();
                }

                current = current.children[childIndex];
            }

            if (!current.isWord) {
                current.isWord = true;
                size++;
            }
        }

        boolean contains(String word) {
            Node node = find(word);
            return node != null && node.isWord;
        }

        List<String> wordsWithPrefix(String prefix) {
            List<String> words = new ArrayList<>();
            Node node = find(prefix);

            if (node != null) {
                collect(node, new StringBuilder(prefix), words);
            }

            return words;
        }

        int wordCount() {
            return size;
        }

        private Node find(String text) {
            Node current = root;

            for (int i = 0; i < text.length(); i++) {
                int childIndex = index(text.charAt(i));

                if (childIndex < 0
                        || current.children[childIndex] == null) {
                    return null;
                }

                current = current.children[childIndex];
            }

            return current;
        }

        private void collect(
                Node node,
                StringBuilder path,
                List<String> words) {

            if (node.isWord) {
                words.add(path.toString());
            }

            for (int i = 0; i < ALPHABET_SIZE; i++) {
                if (node.children[i] != null) {
                    path.append(character(i));
                    collect(node.children[i], path, words);
                    path.deleteCharAt(path.length() - 1);
                }
            }
        }

        private int index(char character) {
            if (character >= 'a' && character <= 'z') {
                return character - 'a';
            }

            if (character >= '0' && character <= '9') {
                return 26 + character - '0';
            }

            if (character == '\'') {
                return 36;
            }

            if (character == '-') {
                return 37;
            }

            return -1;
        }

        private char character(int index) {
            if (index < 26) {
                return (char) ('a' + index);
            }

            if (index < 36) {
                return (char) ('0' + index - 26);
            }

            return index == 36 ? '\'' : '-';
        }

        private static final class Node {
            final Node[] children =
                    new Node[ALPHABET_SIZE];

            boolean isWord;
        }
    }

    // Custom hash table that handles collisions with linked chains.
    private static final class WordFrequencyTable {
        private Entry[] buckets;
        private int size;

        WordFrequencyTable(int capacity) {
            buckets = new Entry[capacity];
        }

        void ensureWord(String word) {
            if (find(word) == null) {
                put(word, 0);
            }
        }

        int get(String word) {
            Entry entry = find(word);
            return entry == null ? 0 : entry.frequency;
        }

        void increment(String word) {
            Entry entry = find(word);

            if (entry != null) {
                entry.frequency++;
            }
        }

        private void put(String word, int frequency) {
            if ((size + 1.0) / buckets.length > 0.75) {
                resize();
            }

            int bucketIndex = bucket(word, buckets.length);

            buckets[bucketIndex] =
                    new Entry(word, frequency, buckets[bucketIndex]);

            size++;
        }

        private Entry find(String word) {
            int bucketIndex = bucket(word, buckets.length);

            for (Entry entry = buckets[bucketIndex];
                    entry != null;
                    entry = entry.next) {
                if (entry.word.equals(word)) {
                    return entry;
                }
            }

            return null;
        }

        private void resize() {
            Entry[] oldBuckets = buckets;
            buckets = new Entry[oldBuckets.length * 2 + 1];
            size = 0;

            for (Entry head : oldBuckets) {
                for (Entry entry = head;
                        entry != null;
                        entry = entry.next) {
                    put(entry.word, entry.frequency);
                }
            }
        }

        private int bucket(String word, int capacity) {
            return (word.hashCode() & 0x7fffffff) % capacity;
        }

        private static final class Entry {
            final String word;
            int frequency;
            final Entry next;

            Entry(String word, int frequency, Entry next) {
                this.word = word;
                this.frequency = frequency;
                this.next = next;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new AutoCompleteSearchEngineGUI());
    }
}