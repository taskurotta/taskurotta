select '1. task: '||count(*) from task
union
select '2. qb$math_action_decider: '||count(*) from qb$math_action_decider
union
select '4. qb$summarizer: ' || count(*) from qb$summarizer
union
select '5. qb$multiplier: ' || count(*) from qb$multiplier
union
select '3. qb$number_generator: ' || count(*) from qb$number_generator;
