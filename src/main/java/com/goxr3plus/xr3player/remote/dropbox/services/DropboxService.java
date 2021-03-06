package main.java.com.goxr3plus.xr3player.remote.dropbox.services;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationResult;
import com.dropbox.core.v2.users.FullAccount;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Duration;
import main.java.com.goxr3plus.xr3player.application.tools.ActionTool;
import main.java.com.goxr3plus.xr3player.application.tools.InfoTool;
import main.java.com.goxr3plus.xr3player.application.tools.NotificationType;
import main.java.com.goxr3plus.xr3player.remote.dropbox.presenter.DropboxFile;
import main.java.com.goxr3plus.xr3player.remote.dropbox.presenter.DropboxViewer;

public class DropboxService extends Service<Boolean> {
	
	public enum DropBoxOperation {
		REFRESH, SEARCH, CREATE_FOLDER, DELETE, PERMANENTLY_DELETE, RENAME, STOPPED;
	}
	
	/**
	 * DropBoxViewer
	 */
	public DropboxViewer dropBoxViewer;
	
	// Create Dropbox client
	private final DbxRequestConfig config = new DbxRequestConfig("XR3Player");
	private DbxClientV2 client;
	private String previousAccessToken;
	private String currentPath;
	private String folderName;
	private String searchWord;
	private final SearchCacheService searchCacheService;
	
	//
	private DropboxFile dropboxFile;
	private String newPath;
	
	private int searchMatchingFilesCounter;
	
	/**
	 * This path is being used to delete files
	 */
	private DropBoxOperation operation;
	
	/**
	 * Constructor
	 * 
	 * @param dropBoxViewer
	 */
	public DropboxService(DropboxViewer dropBoxViewer) {
		this.dropBoxViewer = dropBoxViewer;
		this.searchCacheService = new SearchCacheService(dropBoxViewer);
		
		//On Successful exiting
		setOnSucceeded(s -> {
			
			//Check if failed
			if (!getValue()) {
				
				//Set Login Visible Again
				dropBoxViewer.getLoginVBox().setVisible(true);
				
				//Show message to the User
				ActionTool.showNotification("Authantication Failed",
						"Failed connecting in that Dropbox Account, try : \n1) Connect again with a new Dropbox Account \n2) Connect with another saved DropBox Account \n3) Delete this corrupted saved account",
						Duration.millis(3000), NotificationType.ERROR);
			}
		});
		
	}
	
	/**
	 * Restart the Service
	 * 
	 * @param path
	 *            The path to follow and open the Tree
	 */
	public void refresh(String path) {
		this.currentPath = path;
		this.operation = DropBoxOperation.REFRESH;
		
		//Clear all the children
		dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().clear();
		
		//Set LoginScreen not visible 
		dropBoxViewer.getLoginVBox().setVisible(false);
		
		//RefreshLabel
		dropBoxViewer.getRefreshLabel().setText("Connecting to Server ...");
		
		//Restart
		restart();
	}
	
	/**
	 * Search whole Dropbox for the given word
	 * 
	 * @param searchWord
	 */
	public void search(String searchWord) {
		this.searchWord = searchWord.toLowerCase();
		this.operation = DropBoxOperation.SEARCH;
		
		//Clear all the children
		dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().clear();
		
		//RefreshLabel
		dropBoxViewer.getRefreshLabel().setText("Searching for matching files ...");
		
		//Restart
		restart();
	}
	
	/**
	 * After calling this method the Service will find the selected file or files and delete them from Dropbox Account
	 */
	public void delete(DropBoxOperation operation) {
		this.operation = operation;
		
		//RefreshLabel
		dropBoxViewer.getRefreshLabel().setText("Deleting requested files ...");
		
		//Restart
		restart();
	}
	
	/**
	 * Create a new Folder with that name on Dropbox Account
	 * 
	 * @param folderName
	 *            The new folder name
	 */
	public void createFolder(String folderName) {
		this.folderName = folderName;
		this.operation = DropBoxOperation.CREATE_FOLDER;
		
		//RefreshLabel
		dropBoxViewer.getRefreshLabel().setText("Creating requested folder ...");
		
		//Restart                                                                   
		restart();
	}
	
	/**
	 * Renames a dropbox file
	 * 
	 * @param dropboxFile
	 *            The dropbox file that will receive this procedure
	 * @param newPath
	 *            newPath of file
	 * 
	 */
	public void rename(DropboxFile dropboxFile , String newPath) {
		this.dropboxFile = dropboxFile;
		this.newPath = newPath;
		this.operation = DropBoxOperation.RENAME;
		
		//RefreshLabel
		dropBoxViewer.getRefreshLabel().setText("Renaming requested file ...");
		
		//Restart                                                                   
		restart();
	}
	
	@Override
	public void restart() {
		this.dropBoxViewer.getCancelDropBoxService().setDisable(operation != DropBoxOperation.SEARCH);
		super.restart();
	}
	
	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				
				try {
					
					//REFRESH?
					if (operation == DropBoxOperation.REFRESH) {
						
						//Create the Client
						if (client == null || previousAccessToken == null || !previousAccessToken.equals(dropBoxViewer.getAccessToken())) {
							previousAccessToken = dropBoxViewer.getAccessToken();
							client = new DbxClientV2(config, dropBoxViewer.getAccessToken());
						}
						
						// Get current account info
						FullAccount account = client.users().getCurrentAccount();
						Platform.runLater(() -> dropBoxViewer.getTopMenuButton().setText(" " + account.getName().getDisplayName()));
						
						//List all the files brooooo!
						ObservableList<DropboxFile> observableList = FXCollections.observableArrayList();
						listAllFiles(currentPath, observableList, false, true);
						
						//Check if folder is empty
						Platform.runLater(() -> {
							dropBoxViewer.getSearchResultsLabel().setVisible(false);
							dropBoxViewer.getEmptyFolderLabel().setVisible(observableList.isEmpty());
							
							//Set the items to TableView
							dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().clear();
							dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().addAll(observableList);
							dropBoxViewer.getDropboxFilesTableViewer().updateLabel();
							
							//Sort the Table
							dropBoxViewer.getDropboxFilesTableViewer().sortTable();
							
							//Make the CacheService Available to the bro user
							if (searchCacheService.getCachedList().isEmpty() && !searchCacheService.isRunning())
								searchCacheService.prepareCachedSearch(client);
						});
					} else if (operation == DropBoxOperation.DELETE) {
						
						//Delete all the selected files and folders
						List<DropboxFile> list = dropBoxViewer.getDropboxFilesTableViewer().getSelectionModel().getSelectedItems().stream().collect(Collectors.toList());
						
						//Remove from the TreeView one by one
						list.forEach(item -> {
							if (delete(item.getMetadata().getPathLower()))
								Platform.runLater(() -> dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().remove(item));
						});
						
						//Update the bottom label
						Platform.runLater(() -> {
							
							//Update the Label
							dropBoxViewer.getDropboxFilesTableViewer().updateLabel();
							
							//Sort the Table
							dropBoxViewer.getDropboxFilesTableViewer().sortTable();
						});
						
					} else if (operation == DropBoxOperation.CREATE_FOLDER) {
						
						//Create Folder
						createFolder(folderName);
						
						//Refresh
						Platform.runLater(() -> refresh(currentPath));
						
					} else if (operation == DropBoxOperation.RENAME) {
						
						//Try to rename
						rename(dropboxFile.getMetadata().getPathLower(), newPath);
						
					} else if (operation == DropBoxOperation.SEARCH) {
						ObservableList<DropboxFile> observableList;
						
						//CountDown Latch
						CountDownLatch countDown = new CountDownLatch(1);
						boolean[] searchCacheServiceIsRunning = { false };
						Platform.runLater(() -> {
							searchCacheServiceIsRunning[0] = searchCacheService.isRunning();
							countDown.countDown();
						});
						
						//Wait
						countDown.await();
						
						//Check if cached search is available
						if (!searchCacheService.getCachedList().isEmpty() && !searchCacheServiceIsRunning[0]) {
							
							System.out.println("Doing ++CACHED SEARCH++");
							
							//Search based on cachedList
							observableList = cachedSearch(searchCacheService.getCachedList());
							
						}
						
						//Do normal global search
						else {
							
							System.out.println("Doing --NORMAL SEARCH--");
							searchMatchingFilesCounter = 0;
							
							//Prepare an observableList
							observableList = FXCollections.observableArrayList();
							
							//Clear Cached Search
							searchCacheService.getCachedList().clear();
							
							//Start a normal Search
							search("", observableList);
							
						}
						
						//Run of JavaFX Thread
						Platform.runLater(() -> {
							
							//Set Label Visible
							dropBoxViewer.getSearchResultsLabel().setVisible(true);
							
							//Set the items to TableView
							dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().clear();
							dropBoxViewer.getDropboxFilesTableViewer().getTableView().getItems().addAll(observableList);
							
							//Set Label Visible
							dropBoxViewer.getSearchResultsLabel().setText("Total Found -> " + InfoTool.getNumberWithDots(observableList.size()));
							dropBoxViewer.getDropboxFilesTableViewer().updateLabel();
							
							//Sort the Table
							dropBoxViewer.getDropboxFilesTableViewer().sortTable();
						});
						
					}
				} catch (ListFolderErrorException ex) {
					ex.printStackTrace();
					
					//Show to user about the error
					Platform.runLater(
							() -> ActionTool.showNotification("Missing Folder", "Folder : [ " + currentPath + " ] doesn't exist.", Duration.seconds(2), NotificationType.ERROR));
					
					//Check the Internet Connection
					checkConnection();
					
				} catch (Exception ex) {
					ex.printStackTrace();
					
					//Check the Internet Connection
					checkConnection();
					
					//Change the Operation so CachedSearch works correctly
					operation = DropBoxOperation.STOPPED;
					
					return false;
				}
				
				//Change the Operation so CachedSearch works correctly
				operation = DropBoxOperation.STOPPED;
				
				return true;
			}
			
			/**
			 * Check if there is Internet Connection
			 */
			private boolean checkConnection() {
				
				//Check if there is Internet Connection
				if (!InfoTool.isReachableByPing("www.google.com")) {
					Platform.runLater(() -> dropBoxViewer.getErrorVBox().setVisible(true));
					return false;
				}
				
				return true;
			}
			
			/**
			 * List all the Files inside DropboxAccount
			 * 
			 * @param client
			 * @param path
			 * @param children
			 * @param arrayList
			 * @throws DbxException
			 * @throws ListFolderErrorException
			 */
			public void listAllFiles(String path , ObservableList<DropboxFile> children , boolean recursive , boolean appendToMap) throws DbxException {
				
				ListFolderResult result = client.files().listFolder(path);
				
				while (true) {
					for (Metadata metadata : result.getEntries()) {
						if (metadata instanceof DeletedMetadata) { // Deleted
							//	children.remove(metadata.getPathLower())
						} else if (metadata instanceof FolderMetadata) { // Folder
							String folder = metadata.getPathLower();
							//String parent = new File(metadata.getPathLower()).getParent().replace("\\", "/")
							if (appendToMap)
								children.add(new DropboxFile(metadata));
							
							//boolean subFileOfCurrentFolder = path.equals(parent)
							//System.out.println( ( subFileOfCurrentFolder ? "" : "\n" ) + "Folder ->" + folder)
							
							if (recursive)
								listAllFiles(folder, children, recursive, appendToMap);
						} else if (metadata instanceof FileMetadata) { //File
							//String file = metadata.getPathLower()
							//String parent = new File(metadata.getPathLower()).getParent().replace("\\", "/")
							if (appendToMap)
								children.add(new DropboxFile(metadata));
							
							//boolean subFileOfCurrentFolder = path.equals(parent)
							//System.out.println( ( subFileOfCurrentFolder ? "" : "\n" ) + "File->" + file + " Media Info: " + InfoTool.isAudioSupported(file))
						}
					}
					
					if (!result.getHasMore())
						break;
					
					try {
						result = client.files().listFolderContinue(result.getCursor());
						//System.out.println("Entered result next")
					} catch (ListFolderContinueErrorException ex) {
						ex.printStackTrace();
					}
				}
				
			}
			
			/**
			 * List all the Files inside DropboxAccount
			 * 
			 * @param client
			 * @param path
			 * @param children
			 * @throws DbxException
			 */
			public void search(String path , ObservableList<DropboxFile> children) throws DbxException {
				
				ListFolderResult result = client.files().listFolder(path);
				
				while (true) {
					for (Metadata metadata : result.getEntries()) {
						if (metadata instanceof DeletedMetadata) { // Deleted
							//	children.remove(metadata.getPathLower())
						} else if (metadata instanceof FolderMetadata) { // Folder
							String folder = metadata.getPathLower();
							if (metadata.getName().toLowerCase().contains(searchWord))
								children.add(new DropboxFile(metadata));
							
							//Run again
							search(folder, children);
						} else if (metadata instanceof FileMetadata) { //File
							if (metadata.getName().toLowerCase().contains(searchWord)) {
								children.add(new DropboxFile(metadata));
								++searchMatchingFilesCounter;
								
								//Refresh the Search Label
								Platform.runLater(() -> dropBoxViewer.getRefreshLabel()
										.setText("Searching , found [ " + InfoTool.getNumberWithDots(searchMatchingFilesCounter) + " ] matching files"));
								
								//System.out.println(searchMatchingFilesCounter)
							}
							
							//Add each and every item to the cached search list
							searchCacheService.getCachedList().add(metadata);
							
							//System.out.println(searchCacheService.getCachedList().size());
						}
					}
					
					if (!result.getHasMore())
						break;
					
					try {
						result = client.files().listFolderContinue(result.getCursor());
						//System.out.println("Entered result next")
					} catch (ListFolderContinueErrorException ex) {
						ex.printStackTrace();
					}
				}
				
			}
			
			/**
			 * List all the Files inside DropboxAccount
			 * 
			 * @return
			 * 
			 */
			public ObservableList<DropboxFile> cachedSearch(List<Metadata> cachedList) {
				
				//Find matching patterns
				return cachedList.stream().filter(metadata -> metadata.getName().toLowerCase().contains(searchWord)).map(DropboxFile::new)
						.collect(Collectors.toCollection(FXCollections::observableArrayList));
				
			}
			
			/**
			 * Deletes the given file or folder from Dropbox Account
			 * 
			 * @param path
			 *            The path of the Dropbox File or Folder
			 */
			public boolean delete(String path) {
				try {
					if (operation == DropBoxOperation.DELETE)
						client.files().deleteV2(path);
					else
						client.files().permanentlyDelete(path); //SUPPORTED ONLY ON BUSINESS PLAN
						
					//Show message to the User
					Platform.runLater(() -> ActionTool.showNotification("Delete was successful", "Successfully deleted selected files/folders", Duration.millis(2000),
							NotificationType.SIMPLE, DropboxViewer.dropBoxImage, 0, 0));
					
					return true;
				} catch (DbxException dbxe) {
					dbxe.printStackTrace();
					
					//Show message to the User
					Platform.runLater(
							() -> ActionTool.showNotification("Failed deleting files", "Failed to delete selected files/folders", Duration.millis(2000), NotificationType.ERROR));
					
					return false;
				}
			}
			
			/**
			 * Renames the given file or folder from Dropbox Account
			 * 
			 * @param oldPath
			 * @param newPath
			 */
			public boolean rename(String oldPath , String newPath) {
				try {
					RelocationResult result = client.files().moveV2(oldPath, newPath);
					
					//Run on JavaFX Thread
					Platform.runLater(() -> {
						
						//Show message
						ActionTool.showNotification("Rename Successful",
								"Succesfully renamed file :\n [ " + dropboxFile.getMetadata().getName() + " ] to -> [ " + result.getMetadata().getName() + " ]",
								Duration.millis(2500), NotificationType.SIMPLE, DropboxViewer.dropBoxImage, 0, 0);
						
						//Return the previous name
						dropboxFile.setMetadata(result.getMetadata());
						
						//Sort the Table
						dropBoxViewer.getDropboxFilesTableViewer().sortTable();
					});
					
					return true;
				} catch (DbxException dbxe) {
					dbxe.printStackTrace();
					
					//Run on JavaFX Thread
					Platform.runLater(() -> {
						
						//Show message
						ActionTool.showNotification("Error Message", "Failed to rename the File:\n [ " + dropboxFile.getMetadata().getName() + " ] to -> [ " + newPath + " ]",
								Duration.millis(2500), NotificationType.ERROR);
						
						//Return the previous name
						dropboxFile.titleProperty().set(dropboxFile.getMetadata().getName());
						
					});
					
					return false;
				}
			}
			
			/**
			 * Create a folder from Dropbox Account
			 * 
			 * @param path
			 *            Folder name
			 */
			public boolean createFolder(String path) {
				try {
					
					//Create new folder
					CreateFolderResult result = client.files().createFolderV2(path, true);
					
					//Show message to the User
					Platform.runLater(() -> ActionTool.showNotification("New folder created", "Folder created with name :\n [ " + result.getMetadata().getName() + " ]",
							Duration.millis(2000), NotificationType.SIMPLE, DropboxViewer.dropBoxImage, 0, 0));
					
					return true;
				} catch (DbxException dbxe) {
					dbxe.printStackTrace();
					
					//Show message to the User
					Platform.runLater(() -> ActionTool.showNotification("Failed creating folder", "Folder was not created", Duration.millis(2000), NotificationType.ERROR));
					
					return false;
				}
			}
			
		};
	}
	
	/**
	 * The client
	 * 
	 * @return the client
	 */
	public DbxClientV2 getClient() {
		return client;
	}
	
	/**
	 * The Current Path on Dropbox Account
	 * 
	 * @return The Current Path on Dropbox Account
	 */
	public String getCurrentPath() {
		return currentPath;
	}
	
	/**
	 * @return the searchCacheService
	 */
	public SearchCacheService getSearchCacheService() {
		return searchCacheService;
	}
	
	/**
	 * @return the operation
	 */
	public DropBoxOperation getOperation() {
		return operation;
	}
	
}
