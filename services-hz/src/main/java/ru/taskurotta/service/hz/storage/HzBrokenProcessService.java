package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.model.BrokenProcess;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.storage.BrokenProcessService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 19.12.13
 * Time: 14:17
 */
public class HzBrokenProcessService implements BrokenProcessService {

    private static final Logger logger = LoggerFactory.getLogger(HzBrokenProcessService.class);

    private IMap<UUID, BrokenProcess> brokenProcessIMap;

    public HzBrokenProcessService(HazelcastInstance hazelcastInstance, String brokenProcessMapName) {
        this.brokenProcessIMap = hazelcastInstance.getMap(brokenProcessMapName);
    }

    @Override
    public void save(BrokenProcess brokenProcess) {
        brokenProcessIMap.put(brokenProcess.getProcessId(), brokenProcess);
    }

    @Override
    public Collection<BrokenProcess> find(SearchCommand searchCommand) {

        logger.trace("Try to find broken processes by searchCommand [{}]", searchCommand);

        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(searchCommand.getStartActorId())) {
            predicates.add(new Predicates.LikePredicate("startActorId", searchCommand.getStartActorId() + "*"));
        }

        if (StringUtils.hasText(searchCommand.getBrokenActorId())) {
            predicates.add(new Predicates.LikePredicate("brokenActorId", searchCommand.getBrokenActorId() + "*"));
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            predicates.add(new Predicates.LikePredicate("errorClassName", searchCommand.getErrorClassName() + "*"));
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            predicates.add(new Predicates.LikePredicate("errorMessage", searchCommand.getErrorMessage() + "*"));
        }

        if (searchCommand.getEndPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", 0l, searchCommand.getEndPeriod()));
        }

        if (searchCommand.getStartPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", searchCommand.getStartPeriod(), Long.MAX_VALUE));
        }

        if (predicates.isEmpty()) {
            return brokenProcessIMap.values();
        } else {
            Predicate[] predicateArray = new Predicate[predicates.size()];
            Predicate predicate = new Predicates.AndPredicate(predicates.toArray(predicateArray));
            return brokenProcessIMap.values(predicate);
        }
    }

    @Override
    public Collection<BrokenProcess> findAll() {
        return brokenProcessIMap.values();
    }

    @Override
    public void delete(UUID processId) {
        brokenProcessIMap.delete(processId);
    }

    @Override
    public void deleteCollection(Collection<UUID> processIds) {
        for (UUID processId : processIds) {
            delete(processId);
        }
    }
}
