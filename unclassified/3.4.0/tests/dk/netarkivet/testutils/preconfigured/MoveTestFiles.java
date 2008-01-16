package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

import java.io.File;
import java.io.IOException;

public class MoveTestFiles implements TestConfigurationIF {
    private File originalsDir;
    private File workingDir;
    public MoveTestFiles(File originalsDir, File workingDir) {
        this.originalsDir = originalsDir;
        this.workingDir = workingDir;
    }
    public void setUp() {
        FileUtils.removeRecursively(workingDir);
        workingDir.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(originalsDir,workingDir);
    }

    public void tearDown() {
        FileUtils.removeRecursively(workingDir);
    }

    public File working(File f) {
        if (!f.getAbsolutePath().startsWith(originalsDir.getAbsolutePath())) {
            throw new ArgumentNotValid(f + " is not in " + originalsDir);
        }
        return new File(workingDir,f.getAbsolutePath().substring(originalsDir.getAbsolutePath().length()));
    }

    public File newTmpFile() {
        try {
            return File.createTempFile(this.getClass().getSimpleName(),"Tmp",workingDir);
        } catch (IOException e) {
            throw new IOFailure("Failed to create a temp file in " + workingDir,e);
        }
    }

    public File newTmpDir() {
        try {
            File tmpFile = File.createTempFile(this.getClass().getSimpleName(),"Tmp",workingDir);
            tmpFile.delete(); // Maybe not necessary
            tmpFile.mkdir();
            return tmpFile;
        } catch (IOException e) {
            throw new IOFailure("Failed to create a temp dir in " + workingDir,e);
        }
    }
}
