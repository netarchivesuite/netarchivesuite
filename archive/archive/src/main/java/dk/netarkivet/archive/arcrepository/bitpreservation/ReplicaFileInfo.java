package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;

public class ReplicaFileInfo {

    public long guid;
    public String replicaId;
    public long fileId;
    public long segmentId;
    public String checksum;
    public int uploadStatus;
    public FileListStatus filelistStatus;
    public Date filelistCheckdatetime;
    public Date checksumCheckdatetime;
    
    public ReplicaFileInfo(long gId, String rId, long fId, long sId, 
	    String cs, int us, int fs, Date fDate, Date cDate) {
	// validate ?
	this.guid = gId;
	this.replicaId = rId;
	this.fileId = fId;
	this.segmentId = sId;
	this.checksum = cs;
	this.uploadStatus = us;
	this.filelistStatus = FileListStatus.fromOrdinal(fs);
	this.filelistCheckdatetime = fDate;
	this.checksumCheckdatetime = cDate;
    }
    
    public String toString() {
	return guid + ":" + replicaId + ":" + fileId 
	        + ":" + segmentId + ":" + checksum + ":" + uploadStatus + ":" 
	        + filelistStatus + ":" + filelistCheckdatetime + ":" 
	        + checksumCheckdatetime;
    }
}
