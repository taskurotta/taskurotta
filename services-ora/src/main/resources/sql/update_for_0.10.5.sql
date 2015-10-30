alter table TSK_SCHEDULED add LIMIT number;
update TSK_SCHEDULED s1 set limit = (select queue_limit from TSK_SCHEDULED s2 where s1.ID = s2.ID);
alter table tsk_scheduled drop column queue_limit;