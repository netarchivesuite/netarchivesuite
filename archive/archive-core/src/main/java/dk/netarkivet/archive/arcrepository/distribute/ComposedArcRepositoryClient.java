package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.distribute.arcrepository.ReaderRepository;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.UploadRepository;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.BatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Created by csr on 3/13/19.
 */
public class ComposedArcRepositoryClient<J extends BatchJob, U extends UploadRepository, R extends  ReaderRepository>
        implements ArcRepositoryClient<J, U, R> {

    ProcessorRepository<J> processorRepository;
    U uploadRepository;
    R readerRepository;

    public ComposedArcRepositoryClient(
            ProcessorRepository<J> processorRepository,
            U uploadRepository,
            R readerRepository) {
        this.processorRepository = processorRepository;
        this.uploadRepository = uploadRepository;
        this.readerRepository = readerRepository;
    }

    @Override public BatchStatus batch(J job, String replicaId, String... args) {
        return processorRepository.batch(job, replicaId, args);
    }


    @Override public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid {
        return readerRepository.get(arcfile, index);
    }

    @Override public void getFile(String arcfilename, Replica replica, File toFile) {
        readerRepository.getFile(arcfilename, replica, toFile);
    }

    @Override public void store(File file) throws IOFailure, ArgumentNotValid {
        uploadRepository.store(file);
    }

    @Override public void close() {
        uploadRepository.close();
        processorRepository.close();
        readerRepository.close();
    }


    @Override public void updateAdminData(String fileName, String replicaId, ReplicaStoreState newval) {

    }

    @Override public void updateAdminChecksum(String filename, String checksum) {

    }

    @Override public File removeAndGetFile(String fileName, String replicaId, String checksum, String credentials) {
        return null;
    }

    @Override public File getAllChecksums(String replicaId) {
        return null;
    }

    @Override public String getChecksum(String replicaId, String filename) {
        return null;
    }

    @Override public File getAllFilenames(String replicaId) {
        return null;
    }

    @Override public File correct(String replicaId, String checksum, File file, String credentials) {
        return null;
    }
}
