import org.eclipse.mat.snapshot.*;
import org.eclipse.mat.snapshot.model.*;
import org.eclipse.mat.util.IProgressListener;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class HeapProbe {
    static ISnapshot S;
    static final IProgressListener L = new IProgressListener() {
        public void beginTask(String s, int i) {}
        public void done() {}
        public boolean isCanceled() { return false; }
        public void setCanceled(boolean b) {}
        public void subTask(String s) {}
        public void worked(int i) {}
        public void sendUserMessage(Severity s, String m, Throwable t) {}
    };

    static org.eclipse.core.runtime.content.IContentType CT;
    static Object DESC;

    static void initPlatform() throws Exception {
        DESC = java.lang.reflect.Proxy.newProxyInstance(
                HeapProbe.class.getClassLoader(),
                new Class[]{org.eclipse.core.runtime.content.IContentDescription.class},
                (p, m, a) -> {
                    switch (m.getName()) {
                        case "isRequested": return Boolean.TRUE;
                        case "getCharset": return "UTF-8";
                        case "getContentType": return CT;
                        default: return null;
                    }
                });
        CT = (org.eclipse.core.runtime.content.IContentType) java.lang.reflect.Proxy.newProxyInstance(
                HeapProbe.class.getClassLoader(),
                new Class[]{org.eclipse.core.runtime.content.IContentType.class},
                (p, m, a) -> {
                    switch (m.getName()) {
                        case "getId": return "org.eclipse.mat.JavaHeapDump";
                        case "getName": return "Java Heap Dump";
                        case "isKindOf": return Boolean.TRUE;
                        case "getDefaultDescription": return DESC;
                        case "getDescriptionFor": return DESC;
                        case "getFileSpecs": return new String[]{"*.hprof"};
                        case "getBaseType": return null;
                        case "isAssociatedWith": return Boolean.TRUE;
                        default: return null;
                    }
                });
        org.eclipse.core.runtime.content.IContentTypeManager mgr =
                (org.eclipse.core.runtime.content.IContentTypeManager) java.lang.reflect.Proxy.newProxyInstance(
                        HeapProbe.class.getClassLoader(),
                        new Class[]{org.eclipse.core.runtime.content.IContentTypeManager.class},
                        (p, m, a) -> {
                            switch (m.getName()) {
                                case "getContentType": return CT;
                                case "findContentTypesFor": return new org.eclipse.core.runtime.content.IContentType[]{CT};
                                case "getContentTypes": return new org.eclipse.core.runtime.content.IContentType[]{CT};
                                default: return null;
                            }
                        });
        org.osgi.framework.Filter filter = (org.osgi.framework.Filter) java.lang.reflect.Proxy.newProxyInstance(
                HeapProbe.class.getClassLoader(), new Class[]{org.osgi.framework.Filter.class},
                (p, m, a) -> Boolean.TRUE);
        org.osgi.framework.BundleContext ctx = (org.osgi.framework.BundleContext) java.lang.reflect.Proxy.newProxyInstance(
                HeapProbe.class.getClassLoader(), new Class[]{org.osgi.framework.BundleContext.class},
                (p, m, a) -> {
                    if (m.getName().equals("createFilter")) return filter;
                    if (m.getName().equals("getProperty")) return "stub";
                    if (m.getReturnType() == boolean.class) return Boolean.FALSE;
                    if (m.getReturnType() == int.class) return 0;
                    return null;
                });
        org.osgi.util.tracker.ServiceTracker tracker =
                new org.osgi.util.tracker.ServiceTracker(ctx, org.eclipse.core.runtime.content.IContentTypeManager.class, null) {
                    @Override public Object getService() { return mgr; }
                };
        // IPreferencesService-заглушка (hprofStrictness -> "" -> DEFAULT)
        org.eclipse.core.runtime.preferences.IPreferencesService prefs =
                (org.eclipse.core.runtime.preferences.IPreferencesService) proxied(
                        org.eclipse.core.runtime.preferences.IPreferencesService.class, (p, m, a) -> {
                            if (m.getReturnType() == String.class) return "";
                            if (m.getReturnType() == boolean.class) return Boolean.FALSE;
                            return null;
                        });
        org.osgi.util.tracker.ServiceTracker prefsTracker =
                new org.osgi.util.tracker.ServiceTracker(ctx, org.eclipse.core.runtime.preferences.IPreferencesService.class, null) {
                    @Override public Object getService() { return prefs; }
                };
        Object ip = Class.forName("org.eclipse.core.internal.runtime.InternalPlatform").getMethod("getDefault").invoke(null);
        java.lang.reflect.Field f = ip.getClass().getDeclaredField("contentTracker");
        f.setAccessible(true);
        f.set(ip, tracker);
        java.lang.reflect.Field pf2 = ip.getClass().getDeclaredField("preferencesTracker");
        pf2.setAccessible(true);
        pf2.set(ip, prefsTracker);
        // HprofPlugin: статика + поле bundle в Plugin
        org.eclipse.mat.hprof.HprofPlugin hp = new org.eclipse.mat.hprof.HprofPlugin();
        java.lang.reflect.Field bfield = org.eclipse.core.runtime.Plugin.class.getDeclaredField("bundle");
        bfield.setAccessible(true);
        org.osgi.framework.Bundle bundle = (org.osgi.framework.Bundle) proxied(
                org.osgi.framework.Bundle.class, (p, m, a) -> {
                    if (m.getName().equals("getSymbolicName")) return "org.eclipse.mat.hprof";
                    if (m.getReturnType() == boolean.class) return Boolean.TRUE;
                    return null;
                });
        bfield.set(hp, bundle);
        java.lang.reflect.Field hpf = org.eclipse.mat.hprof.HprofPlugin.class.getDeclaredField("plugin");
        hpf.setAccessible(true);
        hpf.set(null, hp);
        // MATPlugin: статика + bundle (нужен для ClassSpecificNameResolverRegistry)
        org.eclipse.mat.internal.MATPlugin mp = new org.eclipse.mat.internal.MATPlugin();
        org.osgi.framework.Bundle mpBundle = (org.osgi.framework.Bundle) proxied(
                org.osgi.framework.Bundle.class, (p, m, a) -> {
                    if (m.getName().equals("getSymbolicName")) return "org.eclipse.mat.api";
                    if (m.getReturnType() == boolean.class) return Boolean.TRUE;
                    return null;
                });
        bfield.set(mp, mpBundle);
        java.lang.reflect.Field mpf = org.eclipse.mat.internal.MATPlugin.class.getDeclaredField("plugin");
        mpf.setAccessible(true);
        mpf.set(null, mp);
        bootstrapParserPlugin();
        java.lang.reflect.Field mpt = org.eclipse.mat.internal.MATPlugin.class.getDeclaredField("tracker");
        mpt.setAccessible(true);
        mpt.set(mp, ET);
        System.out.println("PLATFORM_STUB_OK");
    }

    static org.eclipse.core.runtime.dynamichelpers.IExtensionTracker ET;

    static Object proxied(Class<?> iface, java.lang.reflect.InvocationHandler h) {
        return java.lang.reflect.Proxy.newProxyInstance(HeapProbe.class.getClassLoader(), new Class[]{iface}, h);
    }

    static void bootstrapParserPluginPublic() throws Exception { bootstrapParserPlugin(); }
    static void bootstrapParserPlugin() throws Exception {
        // IExtension + IConfigurationElement фальшивки под <parser> из plugin.xml hprof
        org.eclipse.core.runtime.IExtension ext = (org.eclipse.core.runtime.IExtension) proxied(
                org.eclipse.core.runtime.IExtension.class, (p, m, a) -> {
                    switch (m.getName()) {
                        case "getNamespaceIdentifier": return "org.eclipse.mat.hprof";
                        case "getSimpleIdentifier": return "hprof";
                        case "getId": return "hprof";
                        default: return null;
                    }
                });
        org.eclipse.core.runtime.IConfigurationElement cfg = (org.eclipse.core.runtime.IConfigurationElement) proxied(
                org.eclipse.core.runtime.IConfigurationElement.class, (p, m, a) -> {
                    switch (m.getName()) {
                        case "getDeclaringExtension": return ext;
                        case "getChildren": return new org.eclipse.core.runtime.IConfigurationElement[0];
                        case "createExecutableExtension":
                            if ("objectReader".equals(a[0])) return new org.eclipse.mat.hprof.HprofHeapObjectReader();
                            if ("indexBuilder".equals(a[0])) return new org.eclipse.mat.hprof.HprofIndexBuilder();
                            return null;
                        case "getAttribute":
                            if ("fileExtension".equals(a[0])) return "hprof,bin,hprof.gz";
                            if ("name".equals(a[0])) return "Java HPROF";
                            return null;
                        default: return null;
                    }
                });
        // IExtensionTracker + IExtensionRegistry + IRegistryProvider
        ET =
                (org.eclipse.core.runtime.dynamichelpers.IExtensionTracker) proxied(
                        org.eclipse.core.runtime.dynamichelpers.IExtensionTracker.class, (p, m, a) -> {
                            if (m.getName().equals("getExtensions")) return new org.eclipse.core.runtime.IExtension[0];
                            if (m.getReturnType() == String.class) return "";
                            return null;
                        });
        org.eclipse.core.runtime.IExtensionPoint extPoint = (org.eclipse.core.runtime.IExtensionPoint) proxied(
                org.eclipse.core.runtime.IExtensionPoint.class, (p, m, a) -> {
                    if (m.getName().equals("getExtensions")) return new org.eclipse.core.runtime.IExtension[0];
                    if (m.getReturnType() == String.class) return "org.eclipse.mat.parser.parser";
                    return null;
                });
        org.eclipse.core.runtime.IExtensionRegistry reg = (org.eclipse.core.runtime.IExtensionRegistry) proxied(
                org.eclipse.core.runtime.IExtensionRegistry.class, (p, m, a) -> {
                    if (m.getName().equals("getExtensionPoint")) return extPoint;
                    return null;
                });
        org.eclipse.core.runtime.spi.IRegistryProvider prov = (org.eclipse.core.runtime.spi.IRegistryProvider) proxied(
                org.eclipse.core.runtime.spi.IRegistryProvider.class, (p, m, a) -> reg);
        org.eclipse.core.runtime.RegistryFactory.setDefaultRegistryProvider(prov);
        // ParserRegistry с делегатом hprof
        org.eclipse.mat.parser.internal.util.ParserRegistry parserRegistry =
                new org.eclipse.mat.parser.internal.util.ParserRegistry(ET);
        org.eclipse.mat.snapshot.SnapshotFormat fmt = new org.eclipse.mat.snapshot.SnapshotFormat("Java HPROF", new String[]{"hprof"});
        java.lang.reflect.Constructor<?> ctor = null;
        for (java.lang.reflect.Constructor<?> c : org.eclipse.mat.parser.internal.util.ParserRegistry.Parser.class.getDeclaredConstructors()) {
            if (c.getParameterCount() == 4) { c.setAccessible(true); ctor = c; break; }
        }
        if (ctor == null) throw new IllegalStateException("Parser 4-arg ctor not found");
        Object parser = ctor.newInstance(parserRegistry, cfg, fmt, new java.util.regex.Pattern[]{java.util.regex.Pattern.compile(".*\\.hprof")});
        java.lang.reflect.Field df = org.eclipse.mat.util.RegistryReader.class.getDeclaredField("delegates");
        df.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Object> ds = (java.util.Set<Object>) df.get(parserRegistry);
        ds.add(parser);
        // ParserPlugin: статика + поле registry
        org.eclipse.mat.parser.internal.ParserPlugin pp = new org.eclipse.mat.parser.internal.ParserPlugin();
        java.lang.reflect.Field pf = org.eclipse.mat.parser.internal.ParserPlugin.class.getDeclaredField("plugin");
        pf.setAccessible(true);
        pf.set(null, pp);
        System.out.println("DBG after set: field=" + pf.get(null) + " default=" + org.eclipse.mat.parser.internal.ParserPlugin.getDefault());
        java.lang.reflect.Field rf = org.eclipse.mat.parser.internal.ParserPlugin.class.getDeclaredField("registry");
        rf.setAccessible(true);
        rf.set(pp, parserRegistry);
    }

    public static void main(String[] args) throws Exception {
        initPlatform();
        org.eclipse.mat.parser.internal.SnapshotFactoryImpl impl = new org.eclipse.mat.parser.internal.SnapshotFactoryImpl();
        S = impl.openSnapshot(new File(args[0]), new HashMap<String, String>(), L);
        System.out.println("OPENED ok");
        sectionInfo();
        sectionHistogram();
        sectionHzMaps();
        sectionHzStats();
        sectionMaxSizes();
        sectionQueuesExecutors();
        sectionBusiness();
        sectionThreads();
        sectionMetrics();
        sectionAqs();
        sectionRetainedCandidates();
        impl.dispose(S);
        System.out.println("DONE");
    }

    // ---------- helpers ----------
    static IClass cls(String name) {
        try {
            for (IClass c : S.getClasses()) if (name.equals(c.getName())) return c;
        } catch (Exception e) {}
        return null;
    }

    static List<IClass> classesMatching(Pattern p) throws Exception {
        List<IClass> out = new ArrayList<>();
        for (IClass c : S.getClasses()) if (p.matcher(c.getName()).find()) out.add(c);
        return out;
    }

    static Object f(IInstance o, String name) {
        for (Field fd : o.getFields()) if (fd.getName().equals(name)) return fd.getValue();
        return null;
    }

    static long addrOf(Object v) {
        if (v instanceof ObjectReference) return ((ObjectReference) v).getObjectAddress();
        if (v instanceof IObject) return ((IObject) v).getObjectAddress();
        return 0;
    }

    static IObject ref(Object v) throws Exception {
        long a = addrOf(v);
        if (a == 0) return null;
        try { return S.getObject(S.mapAddressToId(a)); } catch (Exception e) { return null; }
    }

    static String strOf(int id) {
        try {
            IObject o = S.getObject(id);
            if (o instanceof IInstance && "java.lang.String".equals(((IInstance) o).getClazz().getName()))
                return decodeString(id);
            if (o instanceof org.eclipse.mat.parser.model.PrimitiveArrayImpl) {
                Object content = ((org.eclipse.mat.parser.model.PrimitiveArrayImpl) o).getValueArray();
                if (content instanceof char[]) return new String((char[]) content);
                if (content instanceof byte[]) return new String((byte[]) content, "UTF-8");
            }
            return o.getTechnicalName();
        } catch (Exception e) { return "<err>"; }
    }

    static String decodeString(int strId) throws Exception {
        IObject so = S.getObject(strId);
        if (!(so instanceof IInstance)) return null;
        IInstance si = (IInstance) so;
        if (!"java.lang.String".equals(si.getClazz().getName())) return null;
        Object vv = f(si, "value");
        long va = addrOf(vv);
        if (va == 0) return "";
        IObject arr = S.getObject(S.mapAddressToId(va));
        if (arr instanceof org.eclipse.mat.parser.model.PrimitiveArrayImpl) {
            Object content = ((org.eclipse.mat.parser.model.PrimitiveArrayImpl) arr).getValueArray();
            if (content instanceof char[]) return new String((char[]) content);
            if (content instanceof byte[]) return new String((byte[]) content, "UTF-8");
        }
        return "<undecodable>";
    }

    static String strOf(Object v) throws Exception {
        long a = addrOf(v);
        return a == 0 ? null : strOf(S.mapAddressToId(a));
    }

    static String rawDesc(Object v) {
        if (v == null) return "null";
        if (v instanceof String || v instanceof Integer || v instanceof Long || v instanceof Boolean || v instanceof Double || v instanceof Float || v instanceof Short || v instanceof Byte) return v.toString();
        return v.getClass().getSimpleName() + (v instanceof ObjectReference ? " @0x" + Long.toHexString(((ObjectReference) v).getObjectAddress()) : "");
    }

    static int id(Object v) throws Exception {
        long a = addrOf(v);
        try { return S.mapAddressToId(a); } catch (Exception e) { return -1; }
    }

    /** approximate ConcurrentHashMap entry count via table walk */
    static int chmCount(IObject chm) throws Exception {
        if (!(chm instanceof IInstance)) return -1;
        IInstance c = (IInstance) chm;
        IObject t = ref(f(c, "table"));
        if (t == null || !(t instanceof IObjectArray)) return -1;
        int n = 0;
        long[] arr = ((IObjectArray) t).getReferenceArray();
        for (long a : arr) {
            if (a == 0) continue;
            int node = S.mapAddressToId(a);
            n++;
            long hash = chmNodeHash(node);
            for (;;) {
                IInstance nd = (IInstance) S.getObject(node);
                long na = addrOf(f(nd, "next"));
                if (na == 0) break;
                int nid = S.mapAddressToId(na);
                if (chmNodeHash(nid) != hash) break;
                n++;
                node = nid;
            }
        }
        return n;
    }

    static long chmNodeHash(int nodeId) throws Exception {
        try {
            Object h = f((IInstance) S.getObject(nodeId), "hash");
            if (h instanceof Integer) return ((Integer) h).intValue() & 0xffffffffL;
            if (h instanceof Long) return ((Long) h) & 0xffffffffL;
        } catch (Exception e) {}
        return -1;
    }

    static IInstance one(String className) {
        IClass c = cls(className);
        if (c == null || c.getNumberOfObjects() == 0) return null;
        try { return (IInstance) S.getObject(c.getObjectIds()[0]); } catch (Exception e) { return null; }
    }

    static void printFields(IInstance o, String prefix) {
        for (Field fd : o.getFields()) {
            Object v = fd.getValue();
            String vdesc;
            if (v == null) vdesc = "null";
            else if (v instanceof String || v instanceof Integer || v instanceof Long || v instanceof Boolean || v instanceof Double || v instanceof Float || v instanceof Short || v instanceof Byte || v instanceof Character)
                vdesc = v.toString();
            else vdesc = v.getClass().getSimpleName() + (v instanceof ObjectReference ? " @" + Long.toHexString(((ObjectReference) v).getObjectAddress()) : "");
            System.out.println(prefix + fd.getName() + " = " + vdesc);
        }
    }

    static void sec(String name) {
        System.out.println("\n########## " + name + " ##########");
        long t0 = System.currentTimeMillis();
        System.out.println(name + " start");
    }

    // ---------- sections ----------
    static void sectionInfo() {
        sec("INFO");
        try {
            SnapshotInfo info = S.getSnapshotInfo();
            System.out.println(info);
        } catch (Exception e) { System.out.println("info err: " + e); }
    }

    static void sectionHistogram() throws Exception {
        sec("HISTOGRAM");
        Histogram h = S.getHistogram(L);
        List<HistogramRecord> recs = new ArrayList<>(h.getClassHistogramRecords());
        recs.sort(HistogramRecord.reverseComparator(HistogramRecord.COMPARATOR_FOR_USEDHEAPSIZE));
        System.out.println("-- top 40 by SHALLOW --");
        for (int i = 0; i < Math.min(40, recs.size()); i++) {
            HistogramRecord r = recs.get(i);
            System.out.printf("%d  %-75s n=%-9d shallow=%15d retained=%15d%n",
                    i + 1, r.getLabel(), r.getNumberOfObjects(), r.getUsedHeapSize(), r.getRetainedHeapSize());
        }
        List<HistogramRecord> byRet = new ArrayList<>(recs);
        byRet.sort(HistogramRecord.reverseComparator(HistogramRecord.COMPARATOR_FOR_RETAINEDHEAPSIZE));
        System.out.println("-- top 40 by RETAINED --");
        for (int i = 0; i < Math.min(40, byRet.size()); i++) {
            HistogramRecord r = byRet.get(i);
            System.out.printf("%d  %-75s n=%-9d shallow=%15d retained=%15d%n",
                    i + 1, r.getLabel(), r.getNumberOfObjects(), r.getUsedHeapSize(), r.getRetainedHeapSize());
        }
        long totalShallow = 0, totalRet = 0;
        for (HistogramRecord r : recs) { totalShallow += r.getUsedHeapSize(); if (r.getRetainedHeapSize() > 0) totalRet += 0; }
        System.out.println("sum shallow all classes = " + totalShallow);
        System.out.println("-- taskurotta classes with n>100 --");
        for (HistogramRecord r : recs)
            if (r.getLabel().contains("taskurotta") && r.getNumberOfObjects() > 100)
                System.out.printf("  %-60s n=%-10d shallow=%15d retained=%15d%n", r.getLabel(), r.getNumberOfObjects(), r.getUsedHeapSize(), -r.getRetainedHeapSize());
        System.out.println("NOTE: retained values above are sign-flipped class-level aggregates (API stores them negative)");
    }

    static void sectionHzMaps() {
        sec("HZ_MAPS");
        try {
            IInstance ctx = one("com.hazelcast.map.impl.MapServiceContextImpl");
            if (ctx == null) { System.out.println("no MapServiceContextImpl"); return; }
            printFields(ctx, "ctx: ");
            IClass mcCls = cls("com.hazelcast.map.impl.MapContainer");
            List<IInstance> mcList = new ArrayList<>();
            for (int id : mcCls.getObjectIds()) mcList.add((IInstance) S.getObject(id));
            System.out.println("MapContainer instances: " + mcList.size());
            for (IInstance mc : mcList) {
                System.out.println("MapContainer name=[" + strOf(f(mc, "name")) + "] @0x" + Long.toHexString(mc.getObjectAddress()));
            }
            Map<String, long[]> perMap = new TreeMap<>(); // name -> {entries, entryShallow, recordShallow}
            int partCount = -1;
            long noMaps = 0, noTable = 0, diagShown = 0;
            if (ctx != null) {
                IObject pcs = ref(f(ctx, "partitionContainers"));
                if (pcs instanceof IObjectArray) partCount = ((IObjectArray) pcs).getReferenceArray().length;
                System.out.println("partitionContainers length: " + partCount);
                for (long pAddr : ((IObjectArray) pcs).getReferenceArray()) {
                    if (pAddr == 0) continue;
                    IInstance pc = (IInstance) S.getObject(S.mapAddressToId(pAddr));
                    Object mapsRaw = f(pc, "maps");
                    IObject mapsObj = ref(mapsRaw);
                    if (!(mapsObj instanceof IInstance)) { noMaps++; continue; }
                    IObject mTable = ref(f((IInstance) mapsObj, "table"));
                    if (!(mTable instanceof IObjectArray)) { noTable++; continue; }
                    if (diagShown++ == 0) {
                        System.out.println("DIAG maps class=" + mapsObj.getClazz().getName() + " tableLen=" + ((IObjectArray) mTable).getReferenceArray().length);
                        for (long da : ((IObjectArray) mTable).getReferenceArray()) {
                            if (da == 0) continue;
                            IInstance dnd = (IInstance) S.getObject(S.mapAddressToId(da));
                            System.out.println("DIAG node class=" + dnd.getClazz().getName());
                            for (Field fd : dnd.getFields())
                                System.out.println("DIAG   node field " + fd.getName() + " = " + rawDesc(fd.getValue()));
                            break;
                        }
                    }
                    for (long a : ((IObjectArray) mTable).getReferenceArray()) {
                        if (a == 0) continue;
                        int node = S.mapAddressToId(a);
                        long hash = chmNodeHash(node);
                        for (;;) {
                            IInstance nd = (IInstance) S.getObject(node);
                            String mname = strOf(f(nd, "key"));
                            IInstance storage = (IInstance) ref(f(nd, "val"));
                            if (storage != null) countRecordStore(storage, mname, perMap);
                            long na = addrOf(f(nd, "next"));
                            if (na == 0) break;
                            int nid = S.mapAddressToId(na);
                            if (chmNodeHash(nid) != hash) break;
                            node = nid;
                        }
                    }
                }
            }
            System.out.println("DIAG partitionContainers: noMaps=" + noMaps + " noTable=" + noTable);
            System.out.println("-- per-map entry counts (all partitions) --");
            for (Map.Entry<String, long[]> e : perMap.entrySet())
                System.out.printf("MAP %-28s entries=%-8d entryShallow=%,d recordShallow=%,d%n",
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]);
            if (ctx != null) { System.out.println("-- sample MapServiceContextImpl fields --"); printFields(ctx, "  "); }
            IInstance pc = one("com.hazelcast.map.impl.PartitionContainer");
            if (pc != null) {
                System.out.println("-- sample PartitionContainer fields --"); printFields(pc, "  ");
                IObject mapsObj = ref(f(pc, "maps"));
                if (mapsObj instanceof IInstance) {
                    IObject mTable = ref(f((IInstance) mapsObj, "table"));
                    if (mTable instanceof IObjectArray) {
                        int shown = 0;
                        for (long a : ((IObjectArray) mTable).getReferenceArray()) {
                            if (a == 0 || shown++ >= 3) continue;
                            IInstance nd = (IInstance) S.getObject(S.mapAddressToId(a));
                            System.out.println("-- sample maps entry node class=" + nd.getClazz().getName() + " --");
                            for (Field fd : nd.getFields())
                                System.out.println("     node field " + fd.getName() + " = " + rawDesc(fd.getValue()));
                            IInstance storage = (IInstance) ref(f(nd, "value"));
                            if (storage != null) { System.out.println("  storage fields:"); printFields(storage, "    "); }
                        }
                    }
                }
            }
            IClass rsCls = cls("com.hazelcast.map.impl.recordstore.DefaultRecordStore");
            System.out.println("DefaultRecordStore count: " + (rsCls == null ? "?" : rsCls.getNumberOfObjects()));
            IClass sCls = cls("com.hazelcast.map.impl.recordstore.StorageImpl");
            System.out.println("StorageImpl count: " + (sCls == null ? "?" : sCls.getNumberOfObjects()));
            IInstance rs = one("com.hazelcast.map.impl.recordstore.DefaultRecordStore");
            if (rs != null) { System.out.println("-- sample DefaultRecordStore fields --"); printFields(rs, "  "); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void countRecordStore(IInstance recordStore, String mname, Map<String, long[]> perMap) throws Exception {
        if (mname == null) mname = "<unknown>";
        long[] acc = perMap.computeIfAbsent(mname, k -> new long[3]);
        // RecordStore (DefaultRecordStore) -> storage (StorageImpl) -> records (CHM Data->Record)
        IInstance storage = (IInstance) ref(f(recordStore, "storage"));
        if (storage == null) storage = recordStore;
        IObject recordsObj = ref(f(storage, "records"));
        if (!(recordsObj instanceof IInstance)) return;
        IObject table = ref(f((IInstance) recordsObj, "table"));
        if (!(table instanceof IObjectArray)) return;
        for (long a : ((IObjectArray) table).getReferenceArray()) {
            if (a == 0) continue;
            int node = S.mapAddressToId(a);
            long hash = chmNodeHash(node);
            for (;;) {
                IInstance nd = (IInstance) S.getObject(node);
                long va = addrOf(f(nd, "val"));
                if (va != 0) {
                    IObject vobj = S.getObject(S.mapAddressToId(va));
                    acc[0]++;
                    acc[1] += vobj.getUsedHeapSize();
                    if (vobj.getClazz().getName().contains("DataRecord")) acc[2] += vobj.getUsedHeapSize();
                }
                long na = addrOf(f(nd, "next"));
                if (na == 0) break;
                int nid = S.mapAddressToId(na);
                if (chmNodeHash(nid) != hash) break;
                node = nid;
            }
        }
    }

    static void sectionHzStats() throws Exception {
        sec("HZ_STATS");
        IInstance ctx = one("com.hazelcast.map.impl.MapServiceContextImpl");
        IInstance lsp = (IInstance) ref(f(ctx, "localMapStatsProvider"));
        IObject statsMap = ref(f(lsp, "statsMap"));
        if (!(statsMap instanceof IInstance)) { System.out.println("no statsMap"); return; }
        IObject table = ref(f((IInstance) statsMap, "table"));
        if (!(table instanceof IObjectArray)) { System.out.println("no table"); return; }
        System.out.println("-- HZ local map stats (own accounting) --");
        for (long a : ((IObjectArray) table).getReferenceArray()) {
            if (a == 0) continue;
            int node = S.mapAddressToId(a);
            long hash = chmNodeHash(node);
            for (;;) {
                IInstance nd = (IInstance) S.getObject(node);
                String mname = strOf(f(nd, "key"));
                IInstance st = (IInstance) ref(f(nd, "val"));
                if (st != null) {
                    System.out.printf("HZSTAT %-30s ownedEntryCount=%-8d backupEntryCount=%-6d ownedMemoryCost=%15d backupMemoryCost=%15d heapCost=%15d dirty=%-5d locked=%-4d creationTime=%s%n",
                            mname, f(st, "ownedEntryCount"), f(st, "backupEntryCount"),
                            f(st, "ownedEntryMemoryCost"), f(st, "backupEntryMemoryCost"),
                            f(st, "heapCost"), f(st, "dirtyEntryCount"), f(st, "lockedEntryCount"),
                            f(st, "creationTime"));
                }
                long na = addrOf(f(nd, "next"));
                if (na == 0) break;
                int nid = S.mapAddressToId(na);
                if (chmNodeHash(nid) != hash) break;
                node = nid;
            }
        }
        // per-map retained: сумма retained по StorageImpl (271 на мап)
        System.out.println("-- per-map exclusive heap (sum retained of 271 StorageImpl) --");
        Map<String, long[]> perMapRet = new TreeMap<>();
        for (long pAddr : ((IObjectArray) ref(f(ctx, "partitionContainers"))).getReferenceArray()) {
            if (pAddr == 0) continue;
            IInstance pc = (IInstance) S.getObject(S.mapAddressToId(pAddr));
            IObject mapsObj = ref(f(pc, "maps"));
            if (!(mapsObj instanceof IInstance)) continue;
            IObject mTable = ref(f((IInstance) mapsObj, "table"));
            if (!(mTable instanceof IObjectArray)) continue;
            for (long ma : ((IObjectArray) mTable).getReferenceArray()) {
                if (ma == 0) continue;
                int mnode = S.mapAddressToId(ma);
                long mhash = chmNodeHash(mnode);
                for (;;) {
                    IInstance mnd = (IInstance) S.getObject(mnode);
                    String mname = strOf(f(mnd, "key"));
                    IInstance recordStore = (IInstance) ref(f(mnd, "val"));
                    if (recordStore != null && mname != null) {
                        IInstance storage = (IInstance) ref(f(recordStore, "storage"));
                        if (storage != null) {
                            long r = S.getRetainedHeapSize(S.mapAddressToId(storage.getObjectAddress()));
                            perMapRet.computeIfAbsent(mname, k -> new long[2]);
                            perMapRet.get(mname)[0] += r;
                            perMapRet.get(mname)[1]++;
                        }
                    }
                    long na = addrOf(f(mnd, "next"));
                    if (na == 0) break;
                    int nid = S.mapAddressToId(na);
                    if (chmNodeHash(nid) != mhash) break;
                    mnode = nid;
                }
            }
        }
        for (Map.Entry<String, long[]> e : perMapRet.entrySet())
            System.out.printf("MAPRET %-30s retained=%15d (partitions=%d)%n", e.getKey(), e.getValue()[0], e.getValue()[1]);
    }

    static void sectionMaxSizes() throws Exception {
        sec("MAXSIZES");
        IClass mcCls = cls("com.hazelcast.config.MapConfig");
        if (mcCls == null) { System.out.println("no MapConfig class"); return; }
        for (int cid : mcCls.getObjectIds()) {
            IInstance mc = (IInstance) S.getObject(cid);
            String nm = strOf(f(mc, "name"));
            IInstance msc = (IInstance) ref(f(mc, "maxSizeConfig"));
            Object policy = msc == null ? null : f(msc, "maxSizePolicy");
            Object size = msc == null ? null : f(msc, "size");
            System.out.printf("MAXSIZE %-30s policy=%s sizePercent=%s%n", nm, policy, size);
        }
        // queues: QueueConfig? имя + size
        IClass qc = cls("com.hazelcast.config.QueueConfig");
        if (qc != null) {
            int n = 0;
            for (int cid : qc.getObjectIds()) {
                IInstance q = (IInstance) S.getObject(cid);
                IInstance msc = (IInstance) ref(f(q, "maxSizeConfig"));
                if (msc != null && n++ < 12)
                    System.out.printf("QMAXSIZE %-40s policy=%s sizePercent=%s%n", strOf(f(q, "name")), f(msc, "maxSizePolicy"), f(msc, "size"));
            }
        }
    }

    static void sectionQueuesExecutors() {
        sec("QUEUES_EXEC");
        try {
            Pattern p = Pattern.compile("queue|Queue|executor|Executor");
            List<IClass> found = classesMatching(p);
            for (IClass c : found) {
                if (c.getNumberOfObjects() > 0 && c.getName().startsWith("com.hazelcast"))
                    System.out.printf("HZ %-80s n=%d%n", c.getName(), c.getNumberOfObjects());
            }
            // queue containers
            IClass qc = cls("com.hazelcast.collection.impl.CollectionContainer");
            if (qc != null && qc.getNumberOfObjects() > 0) {
                for (int id : qc.getObjectIds()) {
                    IInstance o = (IInstance) S.getObject(id);
                    System.out.println("CollectionContainer @0x" + Long.toHexString(o.getObjectAddress()) + ":");
                    printFields(o, "   ");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void sectionBusiness() {
        sec("BUSINESS");
        try {
            for (String n : new String[]{"ru.taskurotta.service.console.model.Process",
                    "ru.taskurotta.transport.model.Task",
                    "ru.taskurotta.transport.model.TaskDecision",
                    "ru.taskurotta.service.dependency.links.Graph"}) {
                IClass c = cls(n);
                if (c != null) System.out.printf("%-55s n=%d%n", n, c.getNumberOfObjects());
            }
            IClass proc = cls("ru.taskurotta.service.console.model.Process");
            if (proc != null && proc.getNumberOfObjects() > 0) {
                int[] ids = proc.getObjectIds();
                Map<String, Long> states = new TreeMap<>();
                long finishedWithEnd = 0;
                IInstance sample = null;
                for (int pid : ids) {
                    IInstance p = (IInstance) S.getObject(pid);
                    Object st = f(p, "state");
                    String s = st == null ? "null" : st.toString();
                    states.merge(s, 1L, Long::sum);
                    if (sample == null) sample = p;
                }
                System.out.println("Process state distribution: " + states);
                System.out.println("-- sample Process fields --");
                printFields(sample, "  ");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void sectionThreads() {
        sec("THREADS");
        try {
            IClass t = cls("java.lang.Thread");
            int[] ids = t.getObjectIds();
            System.out.println("Thread objects: " + ids.length);
            IInstance thSample = (IInstance) S.getObject(ids[0]);
            System.out.println("-- sample Thread fields --");
            printFields(thSample, "  ");
            for (int i = 0; i < Math.min(2, ids.length); i++) {
                IInstance thd = (IInstance) S.getObject(ids[i]);
                Object nv = f(thd, "name");
                long na = addrOf(nv);
                int nid = S.mapAddressToId(na);
                IObject no = S.getObject(nid);
                Object tidv = f(thd, "tid");
                if (no instanceof org.eclipse.mat.parser.model.PrimitiveArrayImpl) {
                    Object content = ((org.eclipse.mat.parser.model.PrimitiveArrayImpl) no).getValueArray();
                    if (content instanceof char[]) {
                        char[] ch = (char[]) content;
                        StringBuilder hex = new StringBuilder();
                        for (int k = 0; k < Math.min(16, ch.length); k++) hex.append(String.format("%04x ", (int) ch[k]));
                        System.out.println("NAMEDIAG tid=" + tidv + " nameRef=0x" + Long.toHexString(na)
                                + " len=" + ch.length + " hex=[" + hex + "] text=[" + new String(ch) + "]");
                    }
                } else {
                    System.out.println("NAMEDIAG tid=" + tidv + " nameRef=0x" + Long.toHexString(na) + " resolved=" + no.getClazz().getName());
                }
            }
            Map<String, Long> byPrefix = new TreeMap<>();
            Map<String, Long> byState = new TreeMap<>();
            String[] THREAD_STATUS = {"NEW", "RUNNABLE", "BLOCKED_ON_OBJECT_MONITOR", "WAITING", "TIMED_WAITING", "PARKED", "TERMINATED"};
            for (int i = 0; i < Math.min(3, ids.length); i++) {
                IInstance thd = (IInstance) S.getObject(ids[i]);
                Object nv = f(thd, "name");
                long na = addrOf(nv);
                System.out.println("STRDIAG " + (nv == null ? "null" : nv.getClass().getName()) + " addr=0x" + Long.toHexString(na));
                if (na != 0) {
                    int nid = S.mapAddressToId(na);
                    IObject no = S.getObject(nid);
                    System.out.println("STRDIAG   resolved class=" + (no instanceof IInstance ? ((IInstance) no).getClazz().getName() : no.getClass().getName()) + " techName=" + no.getTechnicalName());
                    if (no instanceof IInstance && "java.lang.String".equals(((IInstance) no).getClazz().getName())) {
                        Object vv = f((IInstance) no, "value");
                        System.out.println("STRDIAG   value field = " + (vv == null ? "null" : vv.getClass().getName() + " @0x" + Long.toHexString(addrOf(vv))));
                        System.out.println("STRDIAG   decoded = [" + decodeString(nid) + "]");
                    }
                }
            }
            for (int tid : ids) {
                IInstance th = (IInstance) S.getObject(tid);
                String name = strOf(f(th, "name"));
                if (name == null) name = "<null>";
                String prefix;
                int m = name.indexOf('-');
                if (m > 0 && !name.startsWith("qtp")) prefix = name.substring(0, m + 1);
                else if (name.startsWith("qtp")) prefix = "qtp...";
                else prefix = name.length() > 24 ? name.substring(0, 24) : name;
                byPrefix.merge(prefix, 1L, Long::sum);
                Object st = f(th, "threadStatus");
                String stName = (st instanceof Integer && ((Integer) st) >= 0 && ((Integer) st) < THREAD_STATUS.length) ? THREAD_STATUS[(Integer) st] : String.valueOf(st);
                byState.merge(stName, 1L, Long::sum);
            }
            Map<String, Long> dwTargets = new TreeMap<>();
            IInstance dwSample = null;
            for (int tid : ids) {
                IInstance th = (IInstance) S.getObject(tid);
                String nm = strOf(f(th, "name"));
                if (nm != null && nm.startsWith("dw-")) {
                    Object tg = f(th, "target");
                    IObject tgo = ref(tg);
                    String cn = tgo == null ? "<null>" : tgo.getClazz().getName();
                    dwTargets.merge(cn, 1L, Long::sum);
                    if (dwSample == null) dwSample = th;
                }
            }
            System.out.println("dw- threads by target class:");
            dwTargets.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).forEach(e ->
                    System.out.println("  " + e.getValue() + "  " + e.getKey()));
            if (dwSample != null) {
                IObject tgo = ref(f(dwSample, "target"));
                System.out.println("-- sample dw- thread: " + strOf(f(dwSample, "name")) + " state=" + f(dwSample, "threadStatus") + " target=" + (tgo == null ? "null" : tgo.getClazz().getName()) + " --");
                if (tgo instanceof IInstance) printFields((IInstance) tgo, "  ");
            }
            System.out.println("by prefix:");
            byPrefix.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).forEach(e ->
                    System.out.println("  " + e.getValue() + "  " + e.getKey()));
            System.out.println("by state: " + byState);
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void sectionMetrics() {
        sec("METRICS");
        try {
            for (long addr : new long[]{0xc28b4bc0L, 0xc287bb28L}) {
                IObject o = S.getObject(S.mapAddressToId(addr));
                System.out.println(o.getClazz().getName() + " @0x" + Long.toHexString(o.getObjectAddress()) + ":");
                printFields((IInstance) o, "  ");
                for (String holderName : new String[]{"lastHourDataHolder", "lastDayDataHolder", "dataHolder"}) {
                    IInstance h = (IInstance) ref(f((IInstance) o, holderName));
                    if (h == null) continue;
                    int cnt = chmCount(h);
                    System.out.println("  " + holderName + " rows=" + cnt + " (approx CHM count)");
                    // sample keys
                    int shown = 0;
                    IObject table = ref(f(h, "table"));
                    if (table instanceof IObjectArray) {
                        for (long a : ((IObjectArray) table).getReferenceArray()) {
                            if (a == 0 || shown >= 15) continue;
                            int node = S.mapAddressToId(a);
                            IInstance nd = (IInstance) S.getObject(node);
                            String key = strOf(f(nd, "key"));
                            if (key != null) { System.out.println("    key: " + key); shown++; }
                        }
                    }
                }
            }
            for (String n : new String[]{"ru.taskurotta.service.metrics.model.DataRowVO",
                    "ru.taskurotta.service.metrics.model.NumberDataRowVO",
                    "ru.taskurotta.service.metrics.model.DataPointVO",
                    "ru.taskurotta.service.metrics.Metric"}) {
                IClass c = cls(n);
                if (c != null) System.out.printf("%-60s n=%d%n", n, c.getNumberOfObjects());
            }
            IInstance row = one("ru.taskurotta.service.metrics.model.DataRowVO");
            if (row != null) {
                System.out.println("-- sample DataRowVO --");
                printFields(row, "  ");
                IObject ds = ref(f(row, "dsCounts"));
                if (ds instanceof IObjectArray) System.out.println("  dsCounts length = " + ((IObjectArray) ds).getReferenceArray().length);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void sectionAqs() {
        sec("AQS");
        try {
            long target = 0xc25346c0L;
            try {
                IObject t = S.getObject(S.mapAddressToId(target));
                System.out.println("0xc25346c0 = " + t.getClazz().getName());
            } catch (Exception e) {
                System.out.println("0xc25346c0 NOT FOUND in dump (thdump is 8h later; addresses shifted by GC)");
            }
            // scan all AQS-subclass objects with big waiter queues
            List<IClass> aqsClasses = classesMatching(Pattern.compile("Synchronizer|\\$Sync|\\$FairSync|ConditionObject"));
            int aqsScanned = 0, aqsObjects = 0;
            for (IClass c : aqsClasses) {
                if (c.getName().contains("AbstractQueuedSynchronizer") || c.getName().contains("Sync")) {
                    if (c.getNumberOfObjects() == 0) continue;
                    aqsObjects += c.getNumberOfObjects();
                    for (int aid : c.getObjectIds()) {
                        IInstance a = (IInstance) S.getObject(aid);
                        int waiters = 0;
                        Set<Long> waiterThreadIds = new HashSet<>();
                        Object head = f(a, "head");
                        long ha = addrOf(head);
                        while (ha != 0) {
                            IInstance nd = (IInstance) S.getObject(S.mapAddressToId(ha));
                            Object th = f(nd, "thread");
                            long tAddr = addrOf(th);
                            if (tAddr != 0) waiterThreadIds.add(tAddr);
                            waiters++;
                            ha = addrOf(f(nd, "next"));
                        }
                        if (waiters >= 2) {
                            Object owner = f(a, "owner");
                            System.out.printf("AQS %s @0x%x state=%s waiters=%d distinctThreadWaiters=%d owner=%s%n",
                                    c.getName(), a.getObjectAddress(), f(a, "state"), waiters, waiterThreadIds.size(), owner);
                            // inbound: who references this AQS?
                            for (int refId : S.getInboundRefererIds(aid)) {
                                IObject r = S.getObject(refId);
                                System.out.println("   referred-by: " + r.getClazz().getName() + " @0x" + Long.toHexString(r.getObjectAddress()));
                                for (int r2 : S.getInboundRefererIds(refId)) {
                                    IObject r2o = S.getObject(r2);
                                    System.out.println("      referred-by: " + r2o.getClazz().getName() + " @0x" + Long.toHexString(r2o.getObjectAddress()));
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("AQS scan: classes=" + aqsClasses.size() + " objects=" + aqsObjects + " (threshold>=2 waiters, output above)");
            // thread id -> alive check helper
            IClass tcls = cls("java.lang.Thread");
            Map<Long, String> threadIds = new HashMap<>();
            for (int tid : tcls.getObjectIds()) {
                IInstance th = (IInstance) S.getObject(tid);
                Object idv = f(th, "tid");
                Object nm = f(th, "name");
                if (idv instanceof Integer) threadIds.put(((Integer) idv).longValue(), strOf(nm));
                else if (idv instanceof Long) threadIds.put((Long) idv, strOf(nm));
            }
            System.out.println("alive threads indexed by threadId: " + threadIds.size());
            System.out.println("THREADID_MAP_START");
            threadIds.forEach((k, v) -> System.out.println(k + " " + v));
            System.out.println("THREADID_MAP_END");
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void sectionRetainedCandidates() {
        sec("RETAINED_CANDIDATES");
        try {
            List<long[]> cands = new ArrayList<>(); // {addr}
            String[] names = {
                    "ru.taskurotta.service.metrics.handler.MetricsDataHandler",
                    "ru.taskurotta.service.metrics.handler.NumberDataHandler",
                    "ru.taskurotta.service.metrics.handler.GcMetricHandler",
                    "ru.taskurotta.service.metrics.handler.OperationMetricsHandler",
                    "ru.taskurotta.service.metrics.handler.QueueSizeMetricHandler",
                    "com.hazelcast.map.impl.MapServiceContextImpl",
                    "com.hazelcast.internal.serialization.impl.SerializationServiceV1",
                    "com.hazelcast.core.HazelcastInstanceFactory"
            };
            for (String n : names) {
                IClass c = cls(n);
                if (c != null && c.getNumberOfObjects() > 0)
                    for (int oid : c.getObjectIds()) cands.add(new long[]{S.getObject(oid).getObjectAddress()});
            }
            for (long[] c : cands) {
                long t0 = System.currentTimeMillis();
                long r = S.getRetainedHeapSize(S.mapAddressToId(c[0]));
                System.out.printf("retained @0x%x = %,d (%ds)%n", c[0], r, (System.currentTimeMillis() - t0) / 1000);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
