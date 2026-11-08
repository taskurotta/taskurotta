import org.eclipse.mat.snapshot.*;
import org.eclipse.mat.snapshot.model.*;
import org.eclipse.mat.util.IProgressListener;

import java.io.File;
import java.util.*;

/**
 * Универсальный (без хардкода адресов) срез метрик-подсистемы для серии hprof-дампов:
 * retained хэндлеров, число строк в holders, число ключей Metric, объём содержимого колец.
 * Использование: java -cp <mat-plugins>:<driver> MetricsCurve <dump.hprof>
 */
public class MetricsCurve {
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
                MetricsCurve.class.getClassLoader(),
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
                MetricsCurve.class.getClassLoader(),
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
                        MetricsCurve.class.getClassLoader(),
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
                MetricsCurve.class.getClassLoader(), new Class[]{org.osgi.framework.Filter.class},
                (p, m, a) -> Boolean.TRUE);
        org.osgi.framework.BundleContext ctx = (org.osgi.framework.BundleContext) java.lang.reflect.Proxy.newProxyInstance(
                MetricsCurve.class.getClassLoader(), new Class[]{org.osgi.framework.BundleContext.class},
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
        // PlatformActivator.context: без него Pass1Parser NPE-шится в Platform.inDebugMode()
        // (показывается только при полном парсинге; с готовым индексом — не вызывается)
        java.lang.reflect.Field pc = Class.forName("org.eclipse.core.internal.runtime.PlatformActivator").getDeclaredField("context");
        pc.setAccessible(true);
        pc.set(null, ctx);
        Object ip = Class.forName("org.eclipse.core.internal.runtime.InternalPlatform").getMethod("getDefault").invoke(null);
        java.lang.reflect.Field f = ip.getClass().getDeclaredField("contentTracker");
        f.setAccessible(true);
        f.set(ip, tracker);
        java.lang.reflect.Field pf2 = ip.getClass().getDeclaredField("preferencesTracker");
        pf2.setAccessible(true);
        pf2.set(ip, prefsTracker);
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
        return java.lang.reflect.Proxy.newProxyInstance(MetricsCurve.class.getClassLoader(), new Class[]{iface}, h);
    }

    static void bootstrapParserPlugin() throws Exception {
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
        org.eclipse.mat.parser.internal.ParserPlugin pp = new org.eclipse.mat.parser.internal.ParserPlugin();
        java.lang.reflect.Field pf = org.eclipse.mat.parser.internal.ParserPlugin.class.getDeclaredField("plugin");
        pf.setAccessible(true);
        pf.set(null, pp);
        java.lang.reflect.Field rf = org.eclipse.mat.parser.internal.ParserPlugin.class.getDeclaredField("registry");
        rf.setAccessible(true);
        rf.set(pp, parserRegistry);
    }

    // ---------- helpers ----------
    static IClass cls(String name) {
        try {
            for (IClass c : S.getClasses()) if (name.equals(c.getName())) return c;
        } catch (Exception e) {}
        return null;
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

    static String strOf(Object v) throws Exception {
        long a = addrOf(v);
        if (a == 0) return null;
        try {
            IObject o = S.getObject(S.mapAddressToId(a));
            if (o instanceof IInstance && "java.lang.String".equals(((IInstance) o).getClazz().getName()))
                return decodeString(S.mapAddressToId(a));
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

    static int chmNodeHash(int node) {
        try {
            IInstance nd = (IInstance) S.getObject(node);
            for (Field fd : nd.getFields())
                if (fd.getName().equals("hash")) {
                    Object h = fd.getValue();
                    if (h instanceof Integer) return (Integer) h;
                }
        } catch (Exception e) {}
        return 0;
    }

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

    static List<IInstance> instancesOf(String className) throws Exception {
        List<IInstance> out = new ArrayList<>();
        IClass c = cls(className);
        if (c == null) return out;
        for (long id : c.getObjectIds()) out.add((IInstance) S.getObject((int) id));
        return out;
    }

    // ---------- main ----------
    public static void main(String[] args) throws Exception {
        initPlatform();
        org.eclipse.mat.parser.internal.SnapshotFactoryImpl impl = new org.eclipse.mat.parser.internal.SnapshotFactoryImpl();
        S = impl.openSnapshot(new File(args[0]), new HashMap<String, String>(), L);
        System.out.println("OPENED ok: " + new File(args[0]).getName());
        long t0 = System.currentTimeMillis();

        // 1) хэндлеры: retained + holders (Map-поля) с числом строк и суммарным объёмом строк
        String[] handlers = {
                "ru.taskurotta.service.metrics.handler.MetricsDataHandler",
                "ru.taskurotta.service.metrics.handler.NumberDataHandler",
                "ru.taskurotta.service.metrics.handler.GcMetricHandler",
                "ru.taskurotta.service.metrics.handler.OperationMetricsHandler",
                "ru.taskurotta.service.metrics.handler.QueueSizeMetricHandler"};
        for (String hn : handlers) {
            List<IInstance> hs = instancesOf(hn);
            if (hs.isEmpty()) { System.out.println("HANDLER " + hn + " n=0"); continue; }
            for (IInstance h : hs) {
                System.out.println("HANDLER " + hn + " retained=" + h.getRetainedHeapSize());
                for (Field fd : h.getFields()) {
                    IObject v = ref(fd.getValue());
                    if (v == null || !(v instanceof IInstance)) continue;
                    String vcls = ((IInstance) v).getClazz().getName();
                    if (!vcls.contains("ConcurrentHashMap")) continue;
                    int cnt = chmCount(v);
                    long rowsBytes = 0;
                    int sampled = 0;
                    try {
                        IObject table = ref(f((IInstance) v, "table"));
                        if (table instanceof IObjectArray) {
                            for (long a : ((IObjectArray) table).getReferenceArray()) {
                                if (a == 0) continue;
                                int node = S.mapAddressToId(a);
                                IInstance nd = (IInstance) S.getObject(node);
                                long va = addrOf(f(nd, "value"));
                                if (va != 0) rowsBytes += S.getObject(S.mapAddressToId(va)).getUsedHeapSize();
                                if (sampled < 5) {
                                    String key = strOf(f(nd, "key"));
                                    if (key != null) System.out.println("  holder " + fd.getName() + " key: " + key);
                                    sampled++;
                                }
                            }
                        }
                    } catch (Exception e) { System.out.println("  holder " + fd.getName() + " walk err: " + e); }
                    System.out.println("  holder " + fd.getName() + " rows=" + cnt + " rowsShallowSum=" + rowsBytes);
                }
            }
        }

        // 2) ключи метрик: Metric -> dataSets
        List<IInstance> metrics = instancesOf("ru.taskurotta.service.metrics.Metric");
        long totalKeys = 0;
        Map<String, Integer> perMetric = new TreeMap<>();
        for (IInstance m : metrics) {
            IObject ds = ref(f(m, "dataSets"));
            int cnt = (ds instanceof IInstance) ? chmCount(ds) : -1;
            totalKeys += Math.max(0, cnt);
            String name = strOf(f(m, "name"));
            perMetric.merge(name == null ? "<null>" : name, Math.max(0, cnt), Integer::sum);
        }
        System.out.println("METRIC_KEYS metricInstances=" + metrics.size() + " totalKeys=" + totalKeys);
        for (Map.Entry<String, Integer> e : perMetric.entrySet())
            System.out.println("  metric " + e.getKey() + " keys=" + e.getValue());

        // 3) содержимое колец: классы + Double[]
        String[] ring = {"ru.taskurotta.service.metrics.model.DataRowVO",
                "ru.taskurotta.service.metrics.model.NumberDataRowVO",
                "ru.taskurotta.service.metrics.model.DataPointVO"};
        for (String rn : ring) {
            IClass c = cls(rn);
            if (c == null) { System.out.println("RING " + rn + " n=0"); continue; }
            long n = c.getNumberOfObjects();
            long sum = 0;
            for (long id : c.getObjectIds()) sum += S.getObject((int) id).getUsedHeapSize();
            System.out.println("RING " + rn + " n=" + n + " shallowSum=" + sum);
        }
        // 3а) ТОЧНОЕ: строки holder'ов = объекты (референсится только holder'ом),
        //      CheckPoint = 1 на каждый ключ Metric.dataSets
        IClass drCls = cls("ru.taskurotta.service.metrics.model.DataRowVO");
        if (drCls != null) {
            Map<Integer, Long> byLen = new TreeMap<>();
            for (long id : drCls.getObjectIds()) {
                IInstance row = (IInstance) S.getObject((int) id);
                IObject aar = ref(f(row, "dsCounts"));
                int len = 0;
                if (aar instanceof IInstance) {
                    IObject arr = ref(f((IInstance) aar, "array"));
                    if (arr instanceof IObjectArray) len = ((IObjectArray) arr).getReferenceArray().length;
                }
                byLen.merge(len, 1L, Long::sum);
            }
            System.out.println("ROWLEN DataRowVO byRingLength: " + byLen);
        }
        IClass ndrCls = cls("ru.taskurotta.service.metrics.model.NumberDataRowVO");
        if (ndrCls != null) {
            Map<Integer, Long> byLen = new TreeMap<>();
            for (long id : ndrCls.getObjectIds()) {
                IInstance row = (IInstance) S.getObject((int) id);
                IObject aar = ref(f(row, "data"));
                int len = 0;
                if (aar instanceof IInstance) {
                    IObject arr = ref(f((IInstance) aar, "array"));
                    if (arr instanceof IObjectArray) len = ((IObjectArray) arr).getReferenceArray().length;
                }
                byLen.merge(len, 1L, Long::sum);
            }
            System.out.println("ROWLEN NumberDataRowVO byRingLength: " + byLen);
        }
        IClass cpCls = cls("ru.taskurotta.service.metrics.CheckPoint");
        if (cpCls != null)
            System.out.println("CHECKPOINTS_EXACT n=" + cpCls.getNumberOfObjects()
                    + " (1 CheckPoint на каждый ключ Metric.dataSets)");
        IClass dbl = cls("double[]");
        if (dbl != null) {
            long n = dbl.getNumberOfObjects();
            long sum = 0;
            List<long[]> top = new ArrayList<>();
            for (long id : dbl.getObjectIds()) {
                IObject o = S.getObject((int) id);
                long sz = o.getUsedHeapSize();
                sum += sz;
                top.add(new long[]{sz, (int) id});
            }
            top.sort((x, y) -> Long.compare(y[0], x[0]));
            System.out.println("RING [D (Double[]) n=" + n + " totalBytes=" + sum);
            for (int i = 0; i < Math.min(10, top.size()); i++)
                System.out.println("  top double[] bytes=" + top.get(i)[0]);
        }
        IClass dblarr2 = cls("double[]");
        IClass intarr = cls("int[]");
        if (intarr != null) {
            long n = intarr.getNumberOfObjects();
            long sum = 0;
            for (long id : intarr.getObjectIds()) sum += S.getObject((int) id).getUsedHeapSize();
            System.out.println("RING [I (int[]) n=" + n + " totalBytes=" + sum);
        }

        System.out.println("ELAPSED_MS " + (System.currentTimeMillis() - t0));
        impl.dispose(S);
        System.out.println("DONE");
    }
}
