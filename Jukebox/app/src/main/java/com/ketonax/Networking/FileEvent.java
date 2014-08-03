package com.ketonax.Networking;

import java.io.Serializable;
	
	public class FileEvent implements Serializable {
	
	    public FileEvent() {
	    }
	
	    private static final long serialVersionUID = 1L;

	    private String filename;
        private String songName;
	    private long fileSize;
	    private byte[] fileData;
	    private String status;

	
	    public String getFilename() {
	        return filename;
	    }

        public void setSongName (String songName){
            this.songName = songName;
        }

        public String getSongName (){
            return songName;
        }
	
	    public void setFilename(String filename) {
	        this.filename = filename;
	    }
	
	    public long getFileSize() {
	        return fileSize;
	    }
	
	    public void setFileSize(long fileSize) {
	        this.fileSize = fileSize;
	    }
	
	    public String getStatus() {
	        return status;
	    }
	
	    public void setStatus(String status) {
	        this.status = status;
	    }
	
	    public byte[] getFileData() {
	        return fileData;
	    }
	
	    public void setFileData(byte[] fileData) {
	        this.fileData = fileData;
	    }
	}



