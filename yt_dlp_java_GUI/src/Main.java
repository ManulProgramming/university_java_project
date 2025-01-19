import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

//Base class, doesn't work by itself, because it is "abstract"
abstract class Resource {
    String url;
    JFrame frame;

    public Resource(String url, JFrame frame) {
        this.url = url;
        this.frame = frame;
    }

    String send_over_data_to_Python(String host, int port) {
        System.out.println("This function will do nothing unless you declare a specific format at least.");
        return "Error";
    }

    String display() {
        return "Warning! This class is only used as a base, and has no proper functionality!\n{'url': '" + this.url + "', 'format': 'Unknown', 'quality': 'Unknown'}";
    }
}

//Subclass for Resource, dedicated for videos (not only for mp4)
class video_mp4 extends Resource {
    String format;
    String quality;

    public video_mp4(String url, String format, String quality, JFrame frame) {
        super(url, frame);
        this.format = format;
        this.quality = quality;
    }

    @Override
    String send_over_data_to_Python(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to Python Server!");
            //This is only stated in videos, it automatically changes "general" format to yt-dlp appropriate format
            //This is not needed in audio part, because audio is ALWAYS downloaded in the highest possible quality
            if (this.quality.equals("best")) {
                this.quality = "bestvideo+bestaudio/best";
            } else if (this.quality.equals("medium")) {
                this.quality = "best";
            }
            //Sending Python (or JSON) dictionary to the Python server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true); //Auto-flush to clear the stream when needed
            String Data = this.display();
            writer.println(Data);
            System.out.println("Sent to Python: " + Data);
            //Receiving status from Python
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String response = reader.readLine(); //This will wait forever, until it receives data from Python
            //That's okay because it is running in the Thread
            System.out.println("Received from Python: " + response);
            return response;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this.frame, "Error! Python server is unreachable or closed prematurely!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return "Error";
    }

    @Override
    String display() {
        //This format matches a JSON dictionary, which can be read by Python easily
        return "{'url': '" + this.url + "', 'format': '" + this.format + "', 'quality': '" + this.quality + "'}";
    }
}

//Subclass for Resource, dedicated for audio (not only for mp3)
class audio_mp3 extends Resource {
    String format;

    public audio_mp3(String url, String format, JFrame frame) {
        super(url, frame);
        this.format = format;
    }

    @Override
    String send_over_data_to_Python(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to Python Server!");
            //Sending Python (or JSON) dictionary to the Python server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true); //Auto-flush to clear the stream when needed
            String Data = this.display();
            writer.println(Data);
            System.out.println("Sent to Python: " + Data);
            //Receiving status from Python
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String response = reader.readLine(); //This will wait forever, until it receives data from Python
            //That's okay because it is running in the Thread
            System.out.println("Received from Python: " + response);
            return response;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this.frame, "Error! Python server is unreachable or closed prematurely!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return "Error";
    }

    @Override
    String display() {
        //This format matches a JSON dictionary, which can be read by Python easily
        return "{'url': '" + this.url + "', 'format': '" + this.format + "', 'quality': 'bestaudio'}";
    }
}

//Main class, named after the file itself (which is interesting for a Python developer, as files here act as classes themselves)
public class Main {
    //This is an event function that runs when the user sends their url/format/quality by pressing enter or pressing a button
    private static void enter_key_action(DB_manager db, Connection conn, JTextField urlField, JComboBox<String> formatComboBox, JRadioButton bestButton, JRadioButton mediumButton, JRadioButton specificButton, JTextField resolutionField, JButton sendButton, JButton HistoryButton, JFrame history_frame, JButton MassUploadButton, JFrame frame, JLabel loadingImage, JLabel successImage, JLabel errorImage) {
        //If user left upload history opened when sending the download request, the program will attempt to close the history window
        //This is needed because the history with buttons representing entries in the upload history will break the whole app
        //And also it uses a database to get all the previous uploads from user, so that will also be broken
        try {
            history_frame.dispose();
        } catch (Exception e) {
            ; //This is like "None" in Python, basically does nothing when it attempts to close the upload history window, even though it is not open
        }
        urlField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        String url = urlField.getText();
        String format = (String) formatComboBox.getSelectedItem();
        String quality = getSelectedQuality(bestButton, mediumButton, specificButton, resolutionField);
        //Somewhat of a huge check before even sending the data forward to Python.
        //It checks if the user wrote everything correctly
        //While it doesn't check for the correct url, it at least attempts to prevent user from sending just text instead of the url with http/https protocol
        //(Correct URL check is implemented later with yt-dlp itself)
        if (url.equals("") || url == null || url.equals("Please enter the URL...") || url.length() <= 0 || (!(url.matches("http[s]*://.+")))) {
            urlField.setBorder(BorderFactory.createLineBorder(Color.RED));
        }
        if (specificButton.isSelected()) {
            if ((!(quality.matches("[0-9]+"))) || (quality.length() < 3) || (quality.length() > 5)) {
                resolutionField.setBorder(BorderFactory.createLineBorder(Color.RED));
            }
        }
        if ((!(url.equals(""))) && (url != null && url.length() > 0) && (!(url.equals("Please enter the URL..."))) && (url.matches("http[s]*://.+"))) {
            urlField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
        if (specificButton.isSelected()) {
            if ((quality.matches("[0-9]+")) && (quality.length() >= 3) && (quality.length() <= 5)) {
                resolutionField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        }
        if (specificButton.isSelected()) {
            if ((!(url.equals(""))) && (url != null && url.length() > 0) && (!(url.equals("Please enter the URL..."))) && (url.matches("http[s]*://.+")) && (quality.matches("[0-9]+")) && (quality.length() >= 3) && (quality.length() <= 5)) {
                urlField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                resolutionField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                handleInput(db, conn, url, format, quality, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, MassUploadButton, frame, loadingImage, successImage, errorImage);
            }
        } else {
            if ((!(url.equals(""))) && (url != null && url.length() > 0) && (!(url.equals("Please enter the URL..."))) && (url.matches("http[s]*://.+"))) {
                urlField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                resolutionField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                handleInput(db, conn, url, format, quality, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, MassUploadButton, frame, loadingImage, successImage, errorImage);
            }
        }
    }
    //Function for opening File Chooser window and reading the .txt file full of different videos/audio to download
    private static void openFile(JFrame frame, DB_manager db, Connection conn, JTextField urlField, JComboBox<String> formatComboBox, JRadioButton bestButton, JRadioButton mediumButton, JRadioButton specificButton, JTextField resolutionField, JButton sendButton, JButton HistoryButton, JButton MassUploadButton, JLabel loadingImage, JLabel successImage, JLabel errorImage) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Files ending in .txt", "txt"));
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                //Reading data from the file, line by line.
                BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                BufferedReader reader_lines = new BufferedReader(new FileReader(selectedFile));
                String line;
                int total_lines = 0;
                while (reader_lines.readLine() != null) total_lines++;
                int curr_line=0;
                while ((line = reader.readLine()) != null) {
                    curr_line++;
                    List<String> data = new ArrayList<>();
                    try {
                        String[] e = line.split(",");
                        data.add(e[0]);
                        data.add(e[1].toLowerCase());
                        if (!(e[1].toLowerCase().equals("mp3"))) {
                            data.add(e[2].toLowerCase());
                        } else {
                            data.add("best");
                        }
                        successImage.setVisible(false);
                        errorImage.setVisible(false);
                        loadingImage.setVisible(true);
                        Thread_with_arg t = new Thread_with_arg(db, conn, data, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, MassUploadButton, frame, loadingImage, successImage, errorImage);
                        t.start();
                    } catch (Exception e) { //If the file has a wrong format or is something else than expected
                        System.out.println(curr_line+" "+total_lines);
                        if (curr_line==total_lines) {
                            loadingImage.setVisible(false);
                            successImage.setVisible(false);
                            errorImage.setVisible(true);
                        }
                        JOptionPane.showMessageDialog(frame, "Error reading file: " + e.getMessage() + "\nAn entry (" + line + ")\nis written incorrectly in the file.", "Error", JOptionPane.ERROR_MESSAGE);
                        if (curr_line==total_lines) {
                            urlField.setEnabled(true);
                            formatComboBox.setEnabled(true);
                            bestButton.setEnabled(true);
                            mediumButton.setEnabled(true);
                            specificButton.setEnabled(true);
                            resolutionField.setEnabled(true);
                            sendButton.setEnabled(true);
                            HistoryButton.setEnabled(true);
                            MassUploadButton.setEnabled(true);
                            loadingImage.setVisible(false);
                            successImage.setVisible(false);
                            errorImage.setVisible(false);
                        }
                    }
                }
                reader.close();
            } catch (IOException ex) {
                loadingImage.setVisible(false);
                successImage.setVisible(false);
                errorImage.setVisible(true);
                JOptionPane.showMessageDialog(frame, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                urlField.setEnabled(true);
                formatComboBox.setEnabled(true);
                bestButton.setEnabled(true);
                mediumButton.setEnabled(true);
                specificButton.setEnabled(true);
                resolutionField.setEnabled(true);
                sendButton.setEnabled(true);
                HistoryButton.setEnabled(true);
                MassUploadButton.setEnabled(true);
                loadingImage.setVisible(false);
                successImage.setVisible(false);
                errorImage.setVisible(false);
            }
        } else { //This is when the user just closes the File Chooser window, without choosing anything
            urlField.setEnabled(true);
            formatComboBox.setEnabled(true);
            bestButton.setEnabled(true);
            mediumButton.setEnabled(true);
            specificButton.setEnabled(true);
            resolutionField.setEnabled(true);
            sendButton.setEnabled(true);
            HistoryButton.setEnabled(true);
            MassUploadButton.setEnabled(true);
            successImage.setVisible(false);
            errorImage.setVisible(false);
            loadingImage.setVisible(false);
        }
    }
    //Function that creates and returns a new window (Frame) for user's upload history
    private static JFrame historyGUI(List<List<String>> history, JTextField urlField, JComboBox<String> formatComboBox, JRadioButton bestButton, JRadioButton mediumButton, JRadioButton specificButton, JTextField resolutionField) {
        //The frame itself
        JFrame frame = new JFrame("History");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //DISPOSE, so that it won't end the whole program when closed
        frame.setSize(800, 600);

        //Panel will contain all the entries of user's upload history
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.DARK_GRAY);

        //Adds each entry to the Panel
        for (List<String> entry : history) {
            JButton itemPanel = createItemPanel(entry, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField);
            panel.add(itemPanel);
        }
        //This is only displayed when User attempts to see his upload history, with it being completely empty
        if (history.isEmpty()) {
            JLabel nothingLabel = new JLabel("Nothing here yet!");
            nothingLabel.setFont(new Font("Arial", Font.ITALIC, 20));
            nothingLabel.setForeground(Color.WHITE);
            nothingLabel.setBounds(150, 5, 20, 20); // Position ID at the top-left
            panel.add(nothingLabel);
        }

        //Allow scrollbars to appear, both vertical and horizontal
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollPane);

        frame.setVisible(true);
        return frame;
    }
    //Function that creates and returns each entry in form of buttons for the upload history panel in HistoryGUI
    private static JButton createItemPanel(List<String> entry, JTextField urlField, JComboBox<String> formatComboBox, JRadioButton bestButton, JRadioButton mediumButton, JRadioButton specificButton, JTextField resolutionField) {
        //Extracting each data from the List
        String id = entry.get(0);
        String title = entry.get(2);
        String format = "format: " + entry.get(3);
        String quality = "quality: " + entry.get(4);

        //Creating the panel itself
        JButton itemPanel = new JButton();
        itemPanel.setLayout(null); //Absolute position
        itemPanel.setBackground(Color.DARK_GRAY);
        itemPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        itemPanel.setPreferredSize(new Dimension(750, 60)); //Default size (in other words preferred)
        itemPanel.addActionListener(e -> {
            urlField.setText("");
            urlField.setForeground(Color.WHITE);
            urlField.setText(entry.get(1));
            formatComboBox.setSelectedItem(entry.get(3).toUpperCase());
            if (entry.get(4).matches("[0-9]+")) {
                specificButton.setSelected(true);
                resolutionField.setVisible(true);
                resolutionField.setText("");
                resolutionField.setForeground(Color.WHITE);
                resolutionField.setText(entry.get(4));
            } else if (entry.get(4).equals("best")) {
                bestButton.setSelected(true);
            } else if (entry.get(4).equals("medium")) {
                mediumButton.setSelected(true);
            }
        });
        //ID label, just a number right next to the title
        JLabel idLabel = new JLabel(id);
        idLabel.setFont(new Font("Arial", Font.BOLD, 20));
        idLabel.setForeground(Color.WHITE);
        idLabel.setBounds(5, 5, 30, 20); // Position ID at the top-left
        itemPanel.add(idLabel);

        //Title label, title of the video/audio
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(30, 5, 1000, 25); // Position URL next to the ID
        itemPanel.add(titleLabel);

        //Format label, extension, to understand what this is
        JLabel formatLabel = new JLabel(format);
        formatLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formatLabel.setForeground(Color.WHITE);
        formatLabel.setBounds(30, 30, 150, 20); // Position format below the URL
        itemPanel.add(formatLabel);

        //Quality label, self-explanatory
        JLabel qualityLabel = new JLabel(quality);
        qualityLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        qualityLabel.setForeground(Color.WHITE);
        qualityLabel.setBounds(190, 30, 150, 20); // Position quality next to format
        itemPanel.add(qualityLabel);

        //Adjust the Panel width depending on how long is the Title
        int titleWidth = titleLabel.getPreferredSize().width;
        if (titleWidth > 700) {
            itemPanel.setPreferredSize(new Dimension(titleWidth + 50, 60));
        }

        return itemPanel;
    }

    //function that creates a main GUI for the program, that the user sees after launching the program
    private static void mainGUI(DB_manager db, Connection conn) {
        //The frame itself
        JFrame frame = new JFrame("yt-dlp Java GUI");
        AtomicReference<JFrame> history_frame = new AtomicReference<>();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(400, 600);
        frame.setLayout(null); //Absolute layout for precise positioning
        frame.getContentPane().setBackground(Color.DARK_GRAY);

        //Images that show the status of the download process
        JLabel loadingImage = new JLabel(new ImageIcon(Main.class.getResource("data/yt_dlp_loading_process.png")));
        loadingImage.setBounds(0, 0, 400, 600);
        loadingImage.setVisible(false);
        frame.add(loadingImage);
        JLabel successImage = new JLabel(new ImageIcon(Main.class.getResource("data/yt_dlp_success.png")));
        successImage.setBounds(0, 0, 400, 600);
        successImage.setVisible(false);
        frame.add(successImage);
        JLabel errorImage = new JLabel(new ImageIcon(Main.class.getResource("data/yt_dlp_error.png")));
        errorImage.setBounds(0, 0, 400, 600);
        errorImage.setVisible(false);
        frame.add(errorImage);

        //Title image (Which I got from here: https://github.com/yt-dlp/yt-dlp and then modified it a bit)
        JLabel titleImage = new JLabel(new ImageIcon(Main.class.getResource("data/yt_dlp_logo.png")));
        titleImage.setBounds(70, 10, 249, 110);
        frame.add(titleImage);

        //URL input field with placeholder text
        JTextField urlField = new JTextField();
        setPlaceholder(urlField, "Please enter the URL...");
        styleTextField(urlField);
        urlField.setBounds(50, 130, 300, 30);
        frame.add(urlField);

        //Format label and the combobox itself
        JLabel formatLabel = new JLabel("Format:");
        styleLabel(formatLabel);
        formatLabel.setBounds(170, 180, 100, 20);
        frame.add(formatLabel);
        JComboBox<String> formatComboBox = new JComboBox<>(new String[]{"MP4", "MP3", "WEBM"});
        styleComboBox(formatComboBox);
        formatComboBox.setBounds(150, 210, 100, 30);
        frame.add(formatComboBox);

        //Quality label and options themselves
        JLabel qualityLabel = new JLabel("Quality:");
        styleLabel(qualityLabel);
        qualityLabel.setBounds(170, 260, 100, 20);
        frame.add(qualityLabel);
        JRadioButton bestButton = new JRadioButton("Best");
        JRadioButton mediumButton = new JRadioButton("Medium", true);
        JRadioButton specificButton = new JRadioButton("Specific");
        styleRadioButton(bestButton);
        styleRadioButton(mediumButton);
        styleRadioButton(specificButton);
        ButtonGroup qualityGroup = new ButtonGroup();
        qualityGroup.add(bestButton);
        qualityGroup.add(mediumButton);
        qualityGroup.add(specificButton);
        bestButton.setBounds(150, 290, 100, 20);
        mediumButton.setBounds(150, 320, 100, 20);
        specificButton.setBounds(150, 350, 100, 20);
        frame.add(bestButton);
        frame.add(mediumButton);
        frame.add(specificButton);

        //Resolution input, which appears only when "specific" quality is chosen
        JTextField resolutionField = new JTextField();
        setPlaceholder(resolutionField, "Enter resolution without 'p'...");
        styleTextField(resolutionField);
        resolutionField.setBounds(100, 380, 200, 30);
        resolutionField.setVisible(false); //Hidden by default
        frame.add(resolutionField);

        //Buttons right at the bottom of the GUI
        JButton sendButton = new JButton("Send");
        styleSendButton(sendButton);
        sendButton.setBounds(150, 380, 100, 30);
        frame.add(sendButton);
        JButton HistoryButton = new JButton("History");
        styleSendButton(HistoryButton);
        HistoryButton.setBounds(150, 420, 100, 30);
        frame.add(HistoryButton);
        JButton MassUploadButton = new JButton("Use File");
        styleSendButton(MassUploadButton);
        MassUploadButton.setBounds(150, 460, 100, 30);
        frame.add(MassUploadButton);

        //Script to automatically choose "Best" quality option for the audio, and block user from changing it.
        //Otherwise, if the video format is chosen then reactivate them again.
        formatComboBox.addActionListener(e -> {
                String format = (String) formatComboBox.getSelectedItem();
                if (format.equals("MP3")) {
                    bestButton.setSelected(true);
                    bestButton.setEnabled(false);
                    mediumButton.setEnabled(false);
                    specificButton.setEnabled(false);
                    resolutionField.setVisible(false);
                } else {
                    bestButton.setEnabled(true);
                    mediumButton.setEnabled(true);
                    specificButton.setEnabled(true);
                }
        });

        //Adding actions for all the buttons and inputs
        urlField.addActionListener(e -> enter_key_action(db, conn, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, history_frame.get(), MassUploadButton, frame, loadingImage, successImage, errorImage));
        resolutionField.addActionListener(e -> enter_key_action(db, conn, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, history_frame.get(), MassUploadButton, frame, loadingImage, successImage, errorImage));
        sendButton.addActionListener(e -> enter_key_action(db, conn, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, history_frame.get(), MassUploadButton, frame, loadingImage, successImage, errorImage));
        HistoryButton.addActionListener(e -> {
            List<List<String>> history_data = db.select_history(conn); //history_data is collected from the Database by using SELECT
            Collections.reverse(history_data); //The List is reversed, so that the first item in the history will be the latest one
            history_frame.set(historyGUI(history_data, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField));
        });
        MassUploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                urlField.setEnabled(false);
                formatComboBox.setEnabled(false);
                bestButton.setEnabled(false);
                mediumButton.setEnabled(false);
                specificButton.setEnabled(false);
                resolutionField.setEnabled(false);
                sendButton.setEnabled(false);
                HistoryButton.setEnabled(false);
                MassUploadButton.setEnabled(false);
                openFile(frame, db, conn, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, MassUploadButton, loadingImage, successImage, errorImage);
            }
        });
        //Add listener to show or hide the resolution field
        specificButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                resolutionField.setVisible(true); //Show when "Specific" is selected
                //re-adjust buttons to not overlap the resolution field
                sendButton.setBounds(150, 420, 100, 30);
                HistoryButton.setBounds(150, 460, 100, 30);
                MassUploadButton.setBounds(150, 500, 100, 30);
            } else {
                resolutionField.setVisible(false); //Hide otherwise
                sendButton.setBounds(150, 380, 100, 30);
                HistoryButton.setBounds(150, 420, 100, 30);
                MassUploadButton.setBounds(150, 460, 100, 30);
            }
        });

        frame.setVisible(true);
    }

    //Function that allows inputs to have a placeholder text
    private static void setPlaceholder(JTextField textField, String placeholder) {
        //It works by noticing when user focused the cursor on the input field
        //By default, the input has a gray placeholder text
        //But when the user clicks on it, then the scripts checks if the text in that field is the same as the placeholder, and if it is, then clear the input field and return the white color to the text
        //After that, when user clicks somewhere else, if the input field is empty, then the placeholder text returns alongside the gray color for the text
        //(Surprisingly similar to the method you use in Python Tkinter, wow)
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.WHITE);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
    }

    //Style used for the text field, that includes both URL input field and Resolution one
    private static void styleTextField(JTextField textField) {
        textField.setBackground(new Color(60, 60, 60)); //Dark gray, almost black background
        textField.setForeground(Color.GRAY);
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        textField.setCaretColor(Color.WHITE);
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
    }
    //Style used for labels
    private static void styleLabel(JLabel label) {
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setForeground(Color.WHITE);
    }
    //Style used for combobox, that includes "format" entry
    private static void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setBackground(new Color(60, 60, 60));
        comboBox.setForeground(Color.WHITE);
        comboBox.setFont(new Font("Arial", Font.PLAIN, 14));
    }
    //Style used for radio button, that is "quality" thing
    private static void styleRadioButton(JRadioButton radioButton) {
        radioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        radioButton.setForeground(Color.WHITE);
        radioButton.setBackground(Color.DARK_GRAY);
    }
    //Style used for ALL buttons, not just send button
    private static void styleSendButton(JButton sendButton) {
        sendButton.setBackground(new Color(60, 60, 60));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Arial", Font.PLAIN, 14));
        sendButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }
    //Function that returns what quality was chosen.
    private static String getSelectedQuality(JRadioButton best, JRadioButton medium, JRadioButton specific, JTextField resolutionField) {
        if (best.isSelected()) {
            return "Best";
        } else if (medium.isSelected()) {
            return "Medium";
        } else if (specific.isSelected()) {
            return resolutionField.getText();
        }
        return "Medium";
    }
    //Now that's a function that actually starts a thread, responsible for sending data to a Python server
    private static void handleInput(DB_manager db, Connection conn, String url, String format, String quality, JTextField urlField, JComboBox<String> formatComboBox, JRadioButton bestButton, JRadioButton mediumButton, JRadioButton specificButton, JTextField resolutionField, JButton sendButton, JButton HistoryButton, JButton MassUploadButton, JFrame frame, JLabel loadingImage, JLabel successImage, JLabel errorImage) {
        urlField.setEnabled(false);
        formatComboBox.setEnabled(false);
        bestButton.setEnabled(false);
        mediumButton.setEnabled(false);
        specificButton.setEnabled(false);
        resolutionField.setEnabled(false);
        sendButton.setEnabled(false);
        HistoryButton.setEnabled(false);
        MassUploadButton.setEnabled(false);
        errorImage.setVisible(false);
        successImage.setVisible(false);
        loadingImage.setVisible(true);
        List<String> data = new ArrayList<>();
        data.add(url);
        data.add(format.toLowerCase());
        data.add(quality.toLowerCase());
        System.out.println(data);
        Thread_with_arg t = new Thread_with_arg(db, conn, data, urlField, formatComboBox, bestButton, mediumButton, specificButton, resolutionField, sendButton, HistoryButton, MassUploadButton, frame, loadingImage, successImage, errorImage);
        t.start();
    }
    //Basically if __name__ == "__main__"
    public static void main(String[] args) {
        DB_manager db = new DB_manager();
        Connection conn = db.connect();
        //db.drop_history(conn); //This drops full history table
        db.createTable(conn);
        mainGUI(db, conn);
    }
    //And this is a Thread, which runs in parallel to the main javax Swing window.
    //I was surprised how easy it is to set up, comparing to Python.
    static class Thread_with_arg extends Thread {
        private List<String> data;
        private String response;
        private JTextField urlField;
        private JComboBox<String> formatComboBox;
        private JRadioButton bestButton;
        private JRadioButton mediumButton;
        private JRadioButton specificButton;
        private JTextField resolutionField;
        private JButton sendButton;
        private JButton HistoryButton;
        private JButton MassUploadButton;
        private JLabel loadingImage;
        private JLabel successImage;
        private JLabel errorImage;
        private JFrame frame;
        private DB_manager db;
        private Connection conn;

        public Thread_with_arg(DB_manager db, Connection conn, List<String> data, JTextField urlField, JComboBox<String> formatComboBox, JRadioButton bestButton, JRadioButton mediumButton, JRadioButton specificButton, JTextField resolutionField, JButton sendButton, JButton HistoryButton, JButton MassUploadButton, JFrame frame, JLabel loadingImage, JLabel successImage, JLabel errorImage) {
            this.data = data;
            this.response = "Error";
            this.urlField = urlField;
            this.formatComboBox = formatComboBox;
            this.bestButton = bestButton;
            this.mediumButton = mediumButton;
            this.specificButton = specificButton;
            this.resolutionField = resolutionField;
            this.sendButton = sendButton;
            this.HistoryButton = HistoryButton;
            this.MassUploadButton = MassUploadButton;
            this.loadingImage = loadingImage;
            this.successImage = successImage;
            this.errorImage = errorImage;
            this.frame = frame;
            this.db = db;
            this.conn = conn;
        }

        //This is a function which actually runs in parallel
        @Override
        public void run() {
            //Now this is a bit complicated.
            //It receives the data from the user, then checks what format this is, to use the appropriate subclass
            //Then it sends data to Python, and receives the response from it, which might be:
            //1. Done queue empty
            //- Successful upload
            //2. Done
            //- Successful upload, but triggers only when there is a queue of videos left. Occurs when the file is used to massively download videos/audio
            //3. Error
            //- Some kind of error occurred in the yt-dlp, most of the time, because the URL is wrong or video/audio is privated
            //Each status corresponds to specific action and will show an overlay image for the user, for him to know what happened

            //There is also some kind of additional "format" variable that gets format from the formatComboBox itself
            //This variable is needed to now what buttons to enable or not enable (as video allows the usage of quality, while audio doesn't)
            String format = (String) formatComboBox.getSelectedItem();
            if (data.get(1).equalsIgnoreCase("mp4") || data.get(1).equalsIgnoreCase("webm")) {
                video_mp4 video = new video_mp4(data.get(0), data.get(1), data.get(2), frame);
                response = video.send_over_data_to_Python("127.0.0.1", 5000);
                if (response.split(",", 2)[0].equals("Done queue empty")) {
                    data.add(response.split(",", 2)[1]);
                    System.out.println("Success");
                    db.insert_history(conn, data);
                    loadingImage.setVisible(false);
                    errorImage.setVisible(false);
                    successImage.setVisible(true);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    loadingImage.setVisible(false);
                    errorImage.setVisible(false);
                    successImage.setVisible(false);
                    urlField.setEnabled(true);
                    formatComboBox.setEnabled(true);
                    if (format.equalsIgnoreCase("mp4") || format.equalsIgnoreCase("webm")) {
                        bestButton.setEnabled(true);
                        mediumButton.setEnabled(true);
                        specificButton.setEnabled(true);
                        resolutionField.setEnabled(true);
                    }
                    sendButton.setEnabled(true);
                    HistoryButton.setEnabled(true);
                    MassUploadButton.setEnabled(true);
                } else if (response.split(",", 2)[0].equals("Error")) {
                    loadingImage.setVisible(false);
                    successImage.setVisible(false);
                    errorImage.setVisible(true);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    urlField.setBorder(BorderFactory.createLineBorder(Color.RED));
                    resolutionField.setBorder(BorderFactory.createLineBorder(Color.RED));
                    loadingImage.setVisible(false);
                    errorImage.setVisible(false);
                    successImage.setVisible(false);
                    urlField.setEnabled(true);
                    formatComboBox.setEnabled(true);
                    if (format.equalsIgnoreCase("mp4") || format.equalsIgnoreCase("webm")) {
                        bestButton.setEnabled(true);
                        mediumButton.setEnabled(true);
                        specificButton.setEnabled(true);
                        resolutionField.setEnabled(true);
                    }
                    sendButton.setEnabled(true);
                    HistoryButton.setEnabled(true);
                    MassUploadButton.setEnabled(true);
                } else if (response.split(",", 2)[0].equals("Done")) {
                    data.add(response.split(",", 2)[1]);
                    db.insert_history(conn, data);
                }
            } else if (data.get(1).equalsIgnoreCase("mp3")) {
                audio_mp3 audio = new audio_mp3(data.get(0), data.get(1), frame);
                System.out.println(audio.display());
                response = audio.send_over_data_to_Python("127.0.0.1", 5000);
                if (response.split(",", 2)[0].equals("Done queue empty")) {
                    data.add(response.split(",", 2)[1]);
                    db.insert_history(conn, data);
                    loadingImage.setVisible(false);
                    errorImage.setVisible(false);
                    successImage.setVisible(true);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    loadingImage.setVisible(false);
                    errorImage.setVisible(false);
                    successImage.setVisible(false);
                    urlField.setEnabled(true);
                    formatComboBox.setEnabled(true);
                    if (format.equalsIgnoreCase("mp4") || format.equalsIgnoreCase("webm")) {
                        bestButton.setEnabled(true);
                        mediumButton.setEnabled(true);
                        specificButton.setEnabled(true);
                        resolutionField.setEnabled(true);
                    }
                    sendButton.setEnabled(true);
                    resolutionField.setEnabled(true);
                    HistoryButton.setEnabled(true);
                    MassUploadButton.setEnabled(true);
                } else if (response.split(",", 2)[0].equals("Error")) {
                    loadingImage.setVisible(false);
                    successImage.setVisible(false);
                    errorImage.setVisible(true);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    urlField.setBorder(BorderFactory.createLineBorder(Color.RED));
                    resolutionField.setBorder(BorderFactory.createLineBorder(Color.RED));
                    loadingImage.setVisible(false);
                    errorImage.setVisible(false);
                    successImage.setVisible(false);
                    urlField.setEnabled(true);
                    formatComboBox.setEnabled(true);
                    if (format.equalsIgnoreCase("mp4") || format.equalsIgnoreCase("webm")) {
                        bestButton.setEnabled(true);
                        mediumButton.setEnabled(true);
                        specificButton.setEnabled(true);
                        resolutionField.setEnabled(true);
                    }
                    sendButton.setEnabled(true);
                    resolutionField.setEnabled(true);
                    HistoryButton.setEnabled(true);
                    MassUploadButton.setEnabled(true);
                } else if (response.split(",", 2)[0].equals("Done")) {
                    data.add(response.split(",", 2)[1]);
                    db.insert_history(conn, data);
                }
            } else {
                throw new RuntimeException("Unrecognized argument");
            }
        }
    }
}