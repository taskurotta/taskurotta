package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.console.manager.ActorConfigManager;
import ru.taskurotta.backend.console.model.ActorVO;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.statistics.QueueBalanceVO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * User: stukushin, dimadin
 * Date: 04.09.13 17:06
 */
@Path("/console/actor/{action}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ActorResource {

    private static final Logger logger = LoggerFactory.getLogger(ActorResource.class);

    public static final String ACTION_BLOCK = "block";
    public static final String ACTION_UNBLOCK = "unblock";
    public static final String ACTION_LIST = "list";

    private ActorConfigManager actorConfigManager;

    private ConfigBackend configBackend;

    @POST
    public Response blockActor(String actorId, @PathParam("action") String action) {
        try {
            if (ACTION_BLOCK.equals(action)) {
                logger.debug("Blocking actor [{}]", actorId);
                configBackend.blockActor(actorId);

            } else if (ACTION_UNBLOCK.equals(action)) {
                logger.debug("Unblocking actor [{}]", actorId);
                configBackend.unblockActor(actorId);

            } else {
                logger.error("Unknown actor action["+action+"] getted");
                return Response.serverError().build();
            }
            return Response.ok().build();
        } catch (Throwable e) {
            logger.error("Catch exception while ["+action+"] actor [" + actorId +"]", e);
            return Response.serverError().build();
        }
    }

    @GET
    public Response listActors(@PathParam("action") String action,
                               @QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {

        try {
            if (ACTION_LIST.equals(action)) {
                GenericPage<ActorExtVO> extActors = extendActorFeatures(actorConfigManager.getActorList(pageNum.or(1), pageSize.or(10)));
                logger.debug("Actor list getted is [{}]", extActors);

                return Response.ok(extActors, MediaType.APPLICATION_JSON).build();
            } else {
                logger.error("Unknown actor action["+action+"] getted");
                return Response.serverError().build();
            }
        } catch (Throwable e) {
            logger.error("Error for action ["+action+"]", e);
            return Response.serverError().build();
        }

    }

    private GenericPage<ActorExtVO> extendActorFeatures(GenericPage<ActorVO> actors) {
        GenericPage<ActorExtVO> result = null;
        if (actors != null) {
            List<ActorExtVO> items = new ArrayList<>();
            if (actors.getItems()!=null && !actors.getItems().isEmpty()) {
                for (ActorVO actor: actors.getItems()) {
                    ActorExtVO extActor = new ActorExtVO(actor);
                    extActor.queueState = actorConfigManager.getQueueState(actor.getActorId());
                    if(extActor.queueState != null) {
                        extActor.dayRate = getOverallRate(extActor.queueState.getTotalInDay(), extActor.queueState.getInDayPeriod(), extActor.queueState.getTotalOutDay(), extActor.queueState.getOutDayPeriod());
                        extActor.hourRate = getOverallRate(extActor.queueState.getTotalInHour(), extActor.queueState.getInHourPeriod(), extActor.queueState.getTotalOutHour(), extActor.queueState.getOutHourPeriod());
                    }

                    items.add(extActor);
                }
            }
            result = new GenericPage<ActorExtVO>(items, actors.getPageNumber(), actors.getPageSize(), actors.getTotalCount());
        }
        return result;
    }

    private double getOverallRate(int inTotal, long[] inPeriod, int outTotal, long[] outPeriod) {
        double incomeRate = round(getRate(inTotal, inPeriod), 4);
        double outcomeRate = round(getRate(outTotal, outPeriod), 4);

        return round(incomeRate-outcomeRate, 2);
    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        try {
            BigDecimal bd = new BigDecimal(String.valueOf(value));
            bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
            return bd.doubleValue();
        } catch(NumberFormatException e) {
            logger.debug("Exception at rounding value [{}] by [{}] palces", value, places);
            return 0;
        }
    }

    private Double getRate(int count, long[] period) {
        Double result = 0d;

        if (count>0 && period[0]>0 && period[1]>0) {
            long time = period[1]-period[0];
            result = Double.valueOf(count*1000)/Double.valueOf(time);
        }

        return result;
    }

    //ActorVO with extended features for console UI
    protected static class ActorExtVO extends ActorVO implements Serializable {

        protected QueueBalanceVO queueState;
        protected double hourRate = 0d;
        protected double dayRate = 0d;

        public ActorExtVO(ActorVO actor) {
            this.actorId = actor.getActorId();
            this.blocked = actor.isBlocked();
            this.queueName = actor.getQueueName();
            this.lastPoll = actor.getLastPoll();
            this.lastRelease = actor.getLastRelease();
        }

        public QueueBalanceVO getQueueState() {
            return queueState;
        }

        public double getHourRate() {
            return hourRate;
        }

        public double getDayRate() {
            return dayRate;
        }

        @Override
        public String toString() {
            return "ActorExtVO{" +
                    "queueState=" + queueState +
                    ", hourRate=" + hourRate +
                    ", dayRate=" + dayRate +
                    "} " + super.toString();
        }
    }

    @Required
    public void setActorConfigManager(ActorConfigManager actorConfigManager) {
        this.actorConfigManager = actorConfigManager;
    }

    @Required
    public void setConfigBackend(ConfigBackend configBackend) {
        this.configBackend = configBackend;
    }
}
