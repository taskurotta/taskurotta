package ru.taskurotta.schedule.storage.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.storage.JobStore;
import ru.taskurotta.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of a job store using files in JSON format to store jobs.
 * It is strongly recommended to use this implementation only for development/testing purposes
 *
 * Date: 09.12.13 13:35
 */
public class JsonDirectoryJobStore implements JobStore {

    private static final Logger logger = LoggerFactory.getLogger(JsonDirectoryJobStore.class);
    protected ObjectMapper objectMapper = new ObjectMapper();
    public static final String STORE_FILE_EXTENSION = ".json";
    public static final int JSON_FILE_MIN_INDEX = 1;

    private String storeLocation = "job_store";
    private File storeDir;


    public void init() throws IllegalStateException {
        if (!StringUtils.isBlank(storeLocation)) {
            storeDir = new File(storeLocation);
            storeDir.mkdirs();
        }
        if (!storeDir.exists()) {
            throw new IllegalStateException("Cannot find or create directory for job store [" + storeLocation + "]");
        }
        logger.debug("Store dir initialized, location [{}]", storeDir.getPath());
    }

    protected int getAvailableFileNumber() {
        int result = JSON_FILE_MIN_INDEX;
        while (new File(storeDir, result + STORE_FILE_EXTENSION).exists()) {
            result++;
        }
        logger.debug("Available file number is [{}]", result);
        return result;
    }

    @Override
    public long addJob(JobVO task) {
        int fileNumber = getAvailableFileNumber();
        try {
            task.setId(fileNumber);
            objectMapper.writeValue(new File(storeDir, fileNumber + STORE_FILE_EXTENSION), task);
            logger.debug("Job added with number [{}]", fileNumber);
        } catch (IOException e) {
            logger.error("Cannot add job["+task+"] to store", e);
            fileNumber = -1;
        }
        return fileNumber;
    }

    @Override
    public void removeJob (long id) {
        logger.debug("Try to remove job with id [{}]", id);
        File jobFile = new File(storeDir, id + STORE_FILE_EXTENSION);
        if (jobFile.exists()) {
            jobFile.delete();
        }
        if (jobFile.exists()) {
            throw new IllegalStateException("Cannot delete file with id: " + id);
        }
        logger.debug("Job with id [{}] successfully removed", id);
    }

    @Override
    public Collection<Long> getJobIds() {
        Collection<Long> result = null;
        String[] stores = storeDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(STORE_FILE_EXTENSION);
            }
        });
        if (stores != null && stores.length>0) {
            result = new ArrayList<>();
            for (String name : stores) {
                try {
                    result.add(Long.valueOf(name.substring(0, name.length() - STORE_FILE_EXTENSION.length())));
                } catch (Exception e) {
                    logger.error("Cannot extract id from file name " + name);
                }
            }
        }
        logger.debug("Job ids are [{}]", result);
        return result;
    }

    @Override
    public JobVO getJob(long id) {
        JobVO result = null;
        File jobFile = new File(storeDir, id + STORE_FILE_EXTENSION);
        if (jobFile.exists()) {
            try {
                result = objectMapper.readValue(jobFile, JobVO.class);
                result.setId(id);
            } catch (IOException e) {
                logger.error("Cannot read job file with id["+id+"]", e);
            }
        }
        logger.debug("Job got by id [{}] is [{}]", id, result);
        return result;
    }

    @Override
    public void updateJobStatus(long id, int status) {
        JobVO jobVO = getJob(id);
        if (jobVO != null) {
            jobVO.setId(id);
            jobVO.setStatus(status);
            updateJob(jobVO);
        }
    }

    @Override
    public void updateJob(JobVO jobVO) {
        if (jobVO != null && jobVO.getId()>=0) {
            try {
                objectMapper.writeValue(new File(storeDir, jobVO.getId() + STORE_FILE_EXTENSION), jobVO);
            } catch (IOException e) {
                logger.error("Cannot update job["+jobVO+"] to store", e);
            }
        }
    }

    @Override
    public int getJobStatus(long jobId) {
        int result = JobConstants.STATUS_UNDEFINED;
        JobVO job = getJob(jobId);
        if (job!=null) {
            result = job.getStatus();
        }
        return result;
    }

    @Override
    public void updateErrorCount(long jobId, int count, String message) {
        JobVO job = getJob(jobId);
        if (job!=null) {
            job.setId(jobId);
            job.setErrorCount(count);
            job.setLastError(message);
            updateJob(job);
        }
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }
}
