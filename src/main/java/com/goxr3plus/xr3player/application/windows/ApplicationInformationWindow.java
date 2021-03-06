/**
 * 
 */
package main.java.com.goxr3plus.xr3player.application.windows;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.java.com.goxr3plus.xr3player.application.Main;
import main.java.com.goxr3plus.xr3player.application.tools.ActionTool;
import main.java.com.goxr3plus.xr3player.application.tools.InfoTool;

/**
 * @author GOXR3PLUS
 *
 */
public class ApplicationInformationWindow extends BorderPane {
	
	//---------------------------------------------
	
	@FXML
	private Label topLabel;
	
	@FXML
	private VBox centerVBox;
	
	@FXML
	private InlineCssTextArea cssTextArea;
	
	@FXML
	private Button close;
	
	@FXML
	private Button visitWebsite;
	
	@FXML
	private Button reportBug;
	
	// -------------------------------------------------------------
	/** The logger. */
	private Logger logger = Logger.getLogger(getClass().getName());
	
	/** Window **/
	private Stage window = new Stage();
	
	/**
	 * Constructor
	 */
	public ApplicationInformationWindow() {
		
		// ------------------------------------FXMLLOADER--------------------------------------
		FXMLLoader loader = new FXMLLoader(getClass().getResource(InfoTool.FXMLS + "ApplicationInformation.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		
		try {
			loader.load();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "", ex);
			
		}
		
		// --window
		window.setTitle("About-Report Bug");
		window.initStyle(StageStyle.UTILITY);
		//window.setMaxWidth(600);
		//window.setMaxHeight(520);
		
	}
	
	@FXML
	private void initialize() {
		
		// Scene
		window.setScene(new Scene(this));
		window.getScene().getStylesheets().add(getClass().getResource(InfoTool.STYLES + InfoTool.APPLICATIONCSS).toExternalForm());
		
		// InlineCssTextArea
		VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(cssTextArea);
		vsPane.setMinSize(300, 400);
		vsPane.setMaxWidth(Double.MAX_VALUE);
		vsPane.setMaxHeight(Double.MAX_VALUE);
		
		centerVBox.getChildren().remove(cssTextArea);
		centerVBox.getChildren().add(0, vsPane);
		
		// Information - Copyright
		String text = "A cross platform Java/JavaFX Media Player\n";
		cssTextArea.appendText(text);
		
		text = "Copyright (C) <2015-2350>  (www.goxr3plus.co.nf) . All rights reserved.\n\n";
		cssTextArea.appendText(text);
		
		// --Style
		String style = "-fx-font-weight:bold; -fx-font-size:14; -fx-fill:black;";
		
		// Author-Version-Release Date-Home Page
		text = "Author :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style);
		cssTextArea.appendText("GOXR3Plus Studio\n");
		
		text = "Version :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style);
		cssTextArea.appendText(Main.internalInformation.get("Version") + "\n");
		
		text = "Release Date :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style);
		cssTextArea.appendText(Main.internalInformation.get("ReleasedDate") + "\n");
		
		text = "Home Page :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style);
		cssTextArea.appendText("https://sourceforge.net/projects/xr3player\n\n");
		
		// Java Version - Java Vendor - Java Home
		text = "Java Version :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "green"));
		cssTextArea.appendText(System.getProperty("java.version") + "\n");
		
		text = "Java Vendor :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "green"));
		cssTextArea.appendText(System.getProperty("java.vendor") + "\n");
		
		text = "Java Home :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "green"));
		cssTextArea.appendText(System.getProperty("java.home") + "\n\n");
		
		// OS Name - Os Architecture - Os Version
		text = "Os Name :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "orange"));
		cssTextArea.appendText(System.getProperty("os.name") + "\n");
		
		text = "Os Arch :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "orange"));
		cssTextArea.appendText(System.getProperty("os.arch") + "\n");
		
		text = "Os Version :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "orange"));
		cssTextArea.appendText(System.getProperty("os.version") + "\n\n");
		
		// User Name - User Home - User directory
		text = "User Name :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "firebrick"));
		cssTextArea.appendText(System.getProperty("user.name") + "\n");
		
		text = "User Home :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "firebrick"));
		cssTextArea.appendText(System.getProperty("user.home") + "\n");
		
		text = "User Dir :\t";
		cssTextArea.appendText(text);
		cssTextArea.setStyle(cssTextArea.getLength() - text.length(), cssTextArea.getLength() - 1, style.replace("black", "firebrick"));
		cssTextArea.appendText(InfoTool.getBasePathForClass(Main.class) + "\n");
		
		// --close
		close.setOnAction(a -> window.close());
		
		// --visitWebsite
		visitWebsite.setOnAction(a -> ActionTool.openWebSite(InfoTool.WEBSITE));
		
		// --reportBug
		reportBug.setOnAction(a -> ActionTool.openWebSite("https://github.com/goxr3plus/XR3Player/issues"));
		
	}
	
	/**
	 * Shows the Window
	 */
	public void show() {
		window.sizeToScene();
		window.show();
	}
	
	/**
	 * Close the Window
	 */
	public void close() {
		window.close();
	}
	
	/**
	 * @return the window
	 */
	public Stage getWindow() {
		return window;
	}
	
}
