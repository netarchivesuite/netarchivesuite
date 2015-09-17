package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.util.Collection;
import org.bitrepository.commandline.output.OutputHandler;
import org.bitrepository.commandline.outputformatter.GetFileIDsOutputFormatter;
import org.bitrepository.commandline.resultmodel.FileIDsResult;

public class GetFileIDsNoFormatter implements GetFileIDsOutputFormatter {
	
	public GetFileIDsNoFormatter(OutputHandler outputHandler) {
    }
	
	@Override
	public void formatHeader() {
	}

	@Override
	public void formatResult(Collection<FileIDsResult> results) {
	}	
}
