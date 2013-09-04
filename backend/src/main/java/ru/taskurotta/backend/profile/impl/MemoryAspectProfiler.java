package ru.taskurotta.backend.profile.impl;

import ru.taskurotta.backend.console.model.ProfileVO;
import ru.taskurotta.backend.console.retriever.ProfileInfoRetriever;
import ru.taskurotta.backend.profile.AbstractAspectProfiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for profile in memory
 * User: dimadin
 * Date: 28.05.13 10:42
 */
public class MemoryAspectProfiler extends AbstractAspectProfiler implements ProfileInfoRetriever {

    private Map<String, ProfileVO> profiles = new HashMap<String, ProfileVO>();

    public synchronized void addData(String profileName, long time) {
        ProfileVO profile = getProfile(profileName);
        if (time > profile.getMax()) {
            profile.setMax(time);
        }
        if (profile.getMin()==0 || time < profile.getMin()) {
            profile.setMin(time);
        }

        //update mean value: mean = (summ+newTime)/(count+1)
        double newMeanValue = ((profile.getMean() * profile.getMeasured())+time)/(profile.getMeasured()+1);
        profile.setMean(newMeanValue);

        //increment counter
        profile.setMeasured(profile.getMeasured()+1);

    }

    public synchronized ProfileVO getProfile(String name) {
        ProfileVO result = profiles.get(name);
        if (result == null) {
            result = new ProfileVO();
            result.setName(name);
            profiles.put(name, result);
        }
        return result;
    }


    public synchronized List<ProfileVO> getProfileInfo() {
        List<ProfileVO> result = null;
        if (!profiles.keySet().isEmpty()) {
            result = new ArrayList<>();
            result.addAll(profiles.values());
        }
        return result;
    }

}
