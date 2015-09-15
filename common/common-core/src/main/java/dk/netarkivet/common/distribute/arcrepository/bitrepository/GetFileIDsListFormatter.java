package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import org.bitrepository.commandline.output.OutputHandler;
import org.bitrepository.commandline.outputformatter.GetFileIDsOutputFormatter;
import org.bitrepository.commandline.resultmodel.FileIDsResult;

public class GetFileIDsListFormatter implements GetFileIDsOutputFormatter {
	
	List<String> result = new ArrayList<String>();
	//private OutputHandler outputHandler;
	
	public GetFileIDsListFormatter(OutputHandler outputHandler) {
        //this.outputHandler = outputHandler;
    }
	
	@Override
	public void formatHeader() {
	}

	@Override
	public void formatResult(Collection<FileIDsResult> results) {
		for (FileIDsResult a: results) {
			result.add(a.getID());
		}
	}
	
	public List<String> getFoundIds() {
		return result;
	}
	
}
