    static long time = System.currentTimeMillis();
    static ConcurrentMap<Long, OpContext> operations = new ConcurrentHashMap<>();

    static class OpContext {
        BackupAwareOperation op;
        public StackTraceElement[] stack;
        public String threadId;
    }

    private void registerBackups(BackupAwareOperation op, long callId) {
        final long oldCallId = ((Operation) op).getCallId();
        final OperationServiceImpl operationService = nodeEngine.operationService;
        if (oldCallId != 0) {
            operationService.deregisterBackupCall(oldCallId);
        }
        operationService.registerBackupCall(callId);

        OpContext opContext = new OpContext();
        opContext.op = op;
        opContext.stack = Thread.currentThread().getStackTrace();
        opContext.threadId = Thread.currentThread().getName();

        operations.put(callId, opContext);
        long newTime = System.currentTimeMillis();
        if (newTime - 10000 > time) {
            time = newTime;

            ConcurrentMap<Long, Semaphore> backupCalls = operationService.getBackupCalls();
            logger.info("\n\nBackup calls registry size " + backupCalls.size());

            Map<String, AtomicInteger> opStat = new HashMap();
            Map<String, OpContext> lastOpContext = new HashMap();

            for (Long id: backupCalls.keySet()) {
                OpContext storedOpContext = operations.get(id);
                if (storedOpContext == null) {
                    logger.info("id without op = " + id);
                    continue;
                }
                String opClassName = storedOpContext.op.getClass().getName();
                AtomicInteger counter = opStat.get(opClassName);
                if (counter == null) {
                    counter = new AtomicInteger(0);
                    opStat.put(opClassName, counter);
                }
                counter.incrementAndGet();
                lastOpContext.put(opClassName, storedOpContext);
            }

            for (Map.Entry<String, AtomicInteger> entry: opStat.entrySet()) {
                logger.info("\t" + entry.getKey() + " :: " + entry.getValue());
                logger.info("last op stack:\n " + stackToString(lastOpContext.get(entry.getKey()).stack));
            }
        }
    }

    private static String stackToString(StackTraceElement[] stackTraceElements) {
        StringBuffer sb = new StringBuffer();
        for (StackTraceElement element: stackTraceElements) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    
    
    
    
    
    
    
    
    
    private void load(QueueItem item) throws Exception {
            int bulkLoad = Math.min(getItemQueue().size(), store.getBulkLoad());
            if (bulkLoad == 1) {
                item.setData(store.load(item.getItemId()));
            } else if (bulkLoad > 1) {
                ListIterator<QueueItem> iter = getItemQueue().listIterator();
                HashSet<Long> keySet = new HashSet<Long>(bulkLoad);
                for (int i = 0; i < bulkLoad; i++) {
                    keySet.add(iter.next().getItemId());
                }
                Map<Long, Data> values = store.loadAll(keySet);
    
                int loaded = values.size();
                int shouldLoad = keySet.size();
                if (loaded != shouldLoad) {
                    logger.warning("Store data load failed! loaded ["+loaded+"] out of["+shouldLoad+"]. Possible data loss, Trigger queue cleanup...");
                    triggerCleanup();
                }
    
                dataMap.putAll(values);
                item.setData(getDataFromMap(item.getItemId()));
            }
        }
    
        //TODO: remove itemIdAware, just cleanup item queue from null values got on bulk-load
        private void triggerCleanup() {
            if (itemIdAware != null) {
                long minId = itemIdAware.getMinItemId();
                logger.warning("Store cleanup: current queue id["+idGenerator+"], min stored id["+minId+"]");
                int cnt = 0;
                if (minId<0) {//there are no backing collection
                    cnt = getItemQueue().size();
                    getItemQueue().clear();
                } else {
                    LinkedList<QueueItem> queue = getItemQueue();
                    QueueItem item = null;
                    while ( (item = queue.pollFirst()) != null
                            && (item.getItemId() < minId) ) {
                        cnt++;
                    }
                    if (item != null) {
                        queue.offerFirst(item); //return polled item to queue
                    }
                }
                logger.warning("Store cleanup: ["+cnt+"] items removed");
            } else {
                logger.warning("Cannot trigger store cleanup: ItemIdAware implementation is not set!");
            }
        }