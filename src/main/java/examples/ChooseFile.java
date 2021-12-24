package examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.function.Consumer;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ChooseFile {
	public final File selectedFile=null;
	private File initialDirectory = new File("imgs");
	private String initialFileName=null;
	private static final String filePathForStoredInitialDirectoryAndFilename="last-read.txt";
	private void populateInitialDirectoryAndFileName() {
		try {
			File file = new File(filePathForStoredInitialDirectoryAndFilename);
			if (file.exists()) {
				FileInputStream inputStream =new FileInputStream(filePathForStoredInitialDirectoryAndFilename);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String line=reader.readLine();
				File directory=new File(line);
				if (directory.exists() && directory.isDirectory()) {
					initialDirectory = directory;
					initialFileName = reader.readLine();
					System.out.println("initialFileName = " + initialFileName);
				}
				reader.close();
			}
		} catch (Throwable thr) {
			thr.printStackTrace();
		}	
	}
	private void saveInitialDirectoryAndFilename(File file) {
		try {
			PrintWriter writer = new PrintWriter(filePathForStoredInitialDirectoryAndFilename);
			writer.println(file.getParent());
			writer.println(file.getName());
			writer.println();
			writer.close();
		} catch (Throwable thr) {
			thr.printStackTrace();
		}
	}
	public ChooseFile(Stage primaryStage, final String title, final Consumer<File> consumer) throws Exception {
		this(primaryStage,title,null,consumer);
	}
	public ChooseFile(Stage primaryStage, final String title,final ExtensionFilter filter, final Consumer<File> consumer) throws Exception {
		 FileChooser fileChooser = new FileChooser();
		 populateInitialDirectoryAndFileName();
		 fileChooser.setInitialDirectory(initialDirectory);
		 if (initialFileName!=null) {
			 System.out.println("Using initialFileName " + initialFileName);
			 fileChooser.setInitialFileName(initialFileName);
		 }
		 if (filter!=null) {
			 fileChooser.getExtensionFilters().add(filter);
		 }
		 fileChooser.setTitle(title);
//		 fileChooser.getExtensionFilters().addAll(
//		         new ExtensionFilter("Text Files", "*.txt"),
//		         new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
//		         new ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
//		         new ExtensionFilter("All Files", "*.*"));
		 File selectedFile = fileChooser.showOpenDialog(primaryStage);
		 if (selectedFile != null) {
			 System.out.println(selectedFile);
			 saveInitialDirectoryAndFilename(selectedFile);
		 }
		 consumer.accept(selectedFile);
	}
	
}
