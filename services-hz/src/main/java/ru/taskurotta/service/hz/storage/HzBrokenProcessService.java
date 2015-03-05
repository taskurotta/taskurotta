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

    private static final String WILDCARD_SYMBOL = "%";

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

        if (searchCommand.getProcessId() != null) {
            Collection<BrokenProcess> result = null;
            BrokenProcess bp = brokenProcessIMap.get(searchCommand.getProcessId());
            if (bp != null) {
                result = new ArrayList<BrokenProcess>();
                result.add(bp);
            }
            return result;
        }


        if (StringUtils.hasText(searchCommand.getStartActorId())) {
            predicates.add(new Predicates.LikePredicate("startActorId", searchCommand.getStartActorId() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getBrokenActorId())) {
            predicates.add(new Predicates.LikePredicate("brokenActorId", searchCommand.getBrokenActorId() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            predicates.add(new Predicates.LikePredicate("errorClassName", searchCommand.getErrorClassName() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            predicates.add(new Predicates.LikePredicate("errorMessage", searchCommand.getErrorMessage() + WILDCARD_SYMBOL));
        }

        if (searchCommand.getEndPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", 0l, searchCommand.getEndPeriod()));
        }

        if (searchCommand.getStartPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", searchCommand.getStartPeriod(), Long.MAX_VALUE));
        }

        Collection<BrokenProcess> result = null;
        if (predicates.isEmpty()) {
            result = brokenProcessIMap.values();
        } else {
            Predicate[] predicateArray = new Predicate[predicates.size()];
            Predicate predicate = new Predicates.AndPredicate(predicates.toArray(predicateArray));
            result = brokenProcessIMap.values(predicate);
        }

        logger.trace("Found [{}] broken processes by command[{}]", result!=null?result.size():null, searchCommand);
        return result;
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
