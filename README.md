# Java Project for the University
![yt_dlp_Java_GUI](https://github.com/user-attachments/assets/8d26e4f0-62e3-46b2-b290-498ad6433aa3)

This is a Java application that uses Javax.Swing to create a simple GUI for downloading video/audio from YouTube/Reddit/X/etc. This is done by using [yt-dlp](https://github.com/yt-dlp/yt-dlp), specifically its Python module at [pypi](https://pypi.org/project/yt-dlp/). That's right, Python, while the GUI application itself is written in Java. This is done, because of the assignment for that same project, which requires the usage of these components:

1. OOP. Classes and Objects.
2. OOP. Polymorphism and Inheritance.
3. O0P. Abstractions and Interfaces.
4. Collections.
5. Object Serialization or File reading and writing.
6. Threads.
7. Socket programming.
8. JDBC + MySQL database.
9. GUI - Graphical User Interface.

All of this must be implemented in the project in one way or another. So, for the 7th requirement, I implemented a socket connection between a Java GUI client and a Python Server "yt_dlp_server.py".

Let's go into some detail about each of these requirements.

### OOP. Classes and everything else about them

I combined 1-3 parts because they all affect pretty much the same part of the code. The project contains a Main class which on its own contains a subclass Thread_with_arg, used for threading. There is also two subclasses video_mp4 and audio_mp3, each for corresponding format (video and audio), that inherit from the abstract base class Resource. Additionally, this project has a DB_manager class, that is fully responsible for managing the MySQL database.
The main class has these functions:
- ```public static void main``` - runnable function, that starts the whole program.
- ```private static void mainGUI``` - a function that creates the whole GUI.
- ```private static void setPlaceholder, styleTextField, styleLabel, styleComboBox, styleRadioButton, styleSendButton``` - all style methods related to main GUI.
- ```private static String getSelectedQuality``` - a method that returns what quality was chosen.
- ```private static void enter_key_action``` - event function that runs when the user clicks on the send button or presses enter key on input fields of the main GUI.
- ```private static void handleInput``` - a function that is only called in the event method enter_key_action. It starts a new thread responsible for sending data to Python.
- ```private static JFrame historyGUI``` - event function that creates and returns a new window (Frame) for the user's upload history, when the user presses on the History button.
- ```private static JButton createItemPanel``` - a function that creates and returns each entry in the form of buttons for the upload history panel in HistoryGUI.
- ```private static void openFile``` - event function for opening File Chooser window and reading the .txt file full of different videos/audio to download. It starts when the user presses on the Use File button.

abstract class Resource and it's subclasses (video_mp4,audio_mp3) all have two methods:
- ```String display``` - a method that returns a string that is similar in format to a JSON dictionary, that can be easily read by Python.
- ```String send_over_data_to_Python``` - sending data to Python server and getting response using sockets.

The thread class Thread_with_arg has only one method, public void run - that checks the data, creates new variables using subclasses video_mp4 and audio_mp3, and using their methods sends their data over to the Python server. After that, this method receives the status of the download of the media.

And finally, DB_manager class five methods:
- ```Connection connect``` - connects and returns the connection to MySQL database.
- ```public void createTable``` - creates a table "history" with five columns (id int auto_increment PK, url varchar(255), title varchar(1024), format varchar(255), quality varchar(255)).
- ```public List<List<String>> select_history``` - returns a 2D list for each row and column in the history table.
- ```public void insert_history``` - if the user successfully downloads a video, then it is added to the table using INSERT.
- ```public void drop_history``` - drop the whole history table, thus removing all entries. Not used anywhere, but was used for debugging.

### Collections

In the project, List and ArrayList were used. It is specifically was used for containing user data while passing it through multiple functions and methods. Also, as seen in the DB_manager class, a 2D list is used to contain the full upload history of the user, or in other words, the whole history table.

### Object Serialization or File reading and writing

File reading and writing can be understood as working with databases, however, a new feature was added in the form of allowing users to mass upload media they want, by using a file. Users can submit a .txt file that contains url, format, and quality of multiple videos or audio by using "Use File" button. This file should be in this format:

```
url,format,quality (If the format is a video)
```

As an example:

```
https://youtu.be/0V4TiaU06uo,mp4,best
https://youtu.be/0CdMqJ9Lidg,mp3
https://youtu.be/C4cfo0f88Ug,webm,medium
```

### Threads

As was stated in the classes section, an additional subclass Thread_with_arg is used for threading. It allows the program to run multiple tasks at the same time in parallel without freezing other tasks/functions/methods. It is used for sending data to the Python server and waiting for download/conversion to complete so that the whole GUI program won't just stop responding while waiting.

### Socket programming

Once again, as described previously, sockets are used for communicating between Java and Python. This is possible because both Java and Python has libraries responsible for communicating in the network (java.net.Socket in Java, and socket in Python). As the communication and the processing of sending the data in Java were already discussed, we now move on to the Python side.

Python also uses threads, and additionally queues, for aligning users' data one-by-one. First of all, the program starts accepting, in total, 100 clients (in this project's case, requests basically). Next, Python starts a thread for processing the current queue, which runs infinitely. And then starts another parallel thread, also running infinitely, which tries to get the data from those clients, and if does, then it adds that data to a queue. When the first thread process_queue notices that there is a new element in the queue, it starts getting data from it (URL, format, and quality), forms yt-dlp options, and starts yt-dlp with them, downloading the media. Regardless of the outcome of the download, it closes the connection with the client that sent that data. So because of this, each client only sends one piece of data before the closure of the communication.

### JDBC + MySQL database

By using the JDBC driver it was possible to make a connection with a local MySQL Database. Currently, the DB_manager class has a default URL with "upload_history" as the database name, and also has no user and password data. You need to change them to that matching of your MySQL database. Specifically here:

```java
String url = "jdbc:mysql://localhost:3306/upload_history"; //this is an address to the database that will be used
String user = "YOUR_USER_HERE";
String password = "YOUR_PASSWORD_HERE";
```

### GUI - Graphical User Interface.

The project uses javax.Swing to create a graphical interface. As I stated before, this project contains two GUIs - the Main one, which allows users to submit media to download, and the History GUI, which allows users to see their upload history and repeat it if they want.

## Installation:

The application was tested and developed on jdk-19 and Python 3.10, other versions were not tested.

It was also programmed and tested on Windows 11, it is unknown if these Python apps will work with other operating systems.

Clone the repository on both servers from git clone: [https://github.com/ManulProgramming/university_java_project](https://github.com/ManulProgramming/university_java_project).

Go to the /university_java_project/yt_dlp_java_GUI and change the MySQL URL, user, and password in the DB_manager class in the src folder. And then build the java application in the /university_java_project/yt_dlp_java_GUI folder, using IntelliJ Idea or any other application that allows you to create a .jar file from the Java project.

Then, you need to go to the /university_java_project/python_yt_dlp folder and by using Python pip, install the requirements there:

```bash
pip install -r requirements.txt
```

## Usage:

Before running the Java application itself, using Python you need to run the Python yt_dlp_server.py:

```bash
python yt_dlp_server.py
```

This will run the yt-dlp server, that waits for the requests from the Java GUI.

Then run the Java application using a .jar file, that you've built:

```bash
java -jar yt_dlp_java_GUI.jar
```

or some kind of IDEA, like IntelliJ IDEA.

## Notes:

This application is created for educational purposes only. It should not be considered as a serious application, but rather as a basic Python project.

## License:

[MIT](https://github.com/ManulProgramming/university_java_project/blob/main/LICENSE)
