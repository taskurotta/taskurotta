package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.ProfileVO;

import java.util.List;

/**
 * Interface providing information String for profiles
 * User: dimadin
 * Date: 28.05.13 10:37
 */
public interface ProfileInfoRetriever {

    public List<ProfileVO> getProfileInfo();

}
