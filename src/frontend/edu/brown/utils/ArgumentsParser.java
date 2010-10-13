/***************************************************************************
 *   Copyright (C) 2009 by H-Store Project                                 *
 *   Brown University                                                      *
 *   Massachusetts Institute of Technology                                 *
 *   Yale University                                                       *
 *                                                                         *
 *   Permission is hereby granted, free of charge, to any person obtaining *
 *   a copy of this software and associated documentation files (the       *
 *   "Software"), to deal in the Software without restriction, including   *
 *   without limitation the rights to use, copy, modify, merge, publish,   *
 *   distribute, sublicense, and/or sell copies of the Software, and to    *
 *   permit persons to whom the Software is furnished to do so, subject to *
 *   the following conditions:                                             *
 *                                                                         *
 *   The above copyright notice and this permission notice shall be        *
 *   included in all copies or substantial portions of the Software.       *
 *                                                                         *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       *
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    *
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.*
 *   IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR     *
 *   OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, *
 *   ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR *
 *   OTHER DEALINGS IN THE SOFTWARE.                                       *
 ***************************************************************************/
package edu.brown.utils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.voltdb.catalog.*;

import edu.brown.catalog.CatalogUtil;
import edu.brown.correlations.ParameterCorrelations;
import edu.brown.costmodel.AbstractCostModel;
import edu.brown.costmodel.SingleSitedCostModel;
import edu.brown.costmodel.TimeIntervalCostModel;
import edu.brown.designer.*;
import edu.brown.designer.indexselectors.*;
import edu.brown.designer.mappers.*;
import edu.brown.designer.partitioners.*;
import edu.brown.hashing.*;
import edu.brown.markov.EstimationThresholds;
import edu.brown.statistics.*;
import edu.brown.workload.*;
import edu.brown.workload.filters.*;

/**
 * 
 * @author pavlo
 *
 */
public class ArgumentsParser {
    protected static final Logger LOG = Logger.getLogger(ArgumentsParser.class.getName());

    /**
     * Hack to hook in log4j.properties
     */
    private static boolean initialized_log4j = false;
    private static Thread refresh_thread = null;
    
    public static void setupLogging() {
        if (initialized_log4j) return;
        // Hack for testing...
        String paths[] = new String[]{
            System.getProperty("log4j.configuration", "./log4j.properties"),
            "/home/pavlo/Documents/H-Store/SVN-Brown/branches/protorpc-messaging-branch/log4j.properties",
            "/home/pavlo/Documents/H-Store/SVN-Brown/trunk/log4j.properties",
            "/host/work/hstore/src/log4j.properties",
        };
        for (String p : paths) {
            File file = new File(p);
            if (file.exists()) {
                org.apache.log4j.PropertyConfigurator.configure(file.getAbsolutePath());
                Logger.getRootLogger().debug("Loaded log4j configuration file '" + file.getAbsolutePath() + "'");
                initialized_log4j = true;
                break;
            }
        } // FOR
    }
    public static void refreshLogging(final long interval) {
        if (refresh_thread == null) {
            LOG.debug("Starting log4j refresh thread [update interval = " + interval + "]");
            refresh_thread = new Thread() {
                public void run() {
                    if (!initialized_log4j) setupLogging();
                    Thread self = Thread.currentThread();
                    while (!self.isInterrupted()) {
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        initialized_log4j = false;
                        setupLogging();
                        LOG.debug("Refreshed log4j configuration");
                    }
                }
            };
            refresh_thread.setDaemon(true);
            refresh_thread.start();
        }
    }
    static {
        ArgumentsParser.setupLogging();
        ArgumentsParser.refreshLogging(10000); // 180000l); // 3 min
    }

    // --------------------------------------------------------------
    // INPUT PARAMETERS
    // --------------------------------------------------------------
    
    public static final String PARAM_CATALOG                = "catalog";
    public static final String PARAM_CATALOG_JAR            = PARAM_CATALOG + ".jar";
    public static final String PARAM_CATALOG_OUTPUT         = PARAM_CATALOG + ".output";
    public static final String PARAM_CATALOG_TYPE           = PARAM_CATALOG + ".type";
    public static final String PARAM_CATALOG_SCHEMA         = PARAM_CATALOG + ".schema";
    
    public static final String PARAM_WORKLOAD               = "workload";
    public static final String PARAM_WORKLOAD_XACT_LIMIT    = PARAM_WORKLOAD + ".xactlimit";
    public static final String PARAM_WORKLOAD_XACT_OFFSET   = PARAM_WORKLOAD + ".xactoffset";
    public static final String PARAM_WORKLOAD_QUERY_LIMIT   = PARAM_WORKLOAD + ".querylimit";
    public static final String PARAM_WORKLOAD_REMOVE_DUPES  = PARAM_WORKLOAD + ".removedupes";
    public static final String PARAM_WORKLOAD_PROC_EXCLUDE  = PARAM_WORKLOAD + ".procexclude";
    public static final String PARAM_WORKLOAD_PROC_INCLUDE  = PARAM_WORKLOAD + ".procinclude";
    public static final String PARAM_WORKLOAD_PROC_INCLUDE_MULTIPLIER  = PARAM_WORKLOAD_PROC_INCLUDE + ".multiplier";
    public static final String PARAM_WORKLOAD_OUTPUT        = PARAM_WORKLOAD + ".output";
    public static final String PARAM_WORKLOAD_CLASS         = PARAM_WORKLOAD + ".class";
    
    public static final String PARAM_STATS                  = "stats";
    public static final String PARAM_STATS_OUTPUT           = PARAM_STATS + ".output";
    public static final String PARAM_STATS_SCALE_FACTOR     = PARAM_STATS + ".scalefactor";

    public static final String PARAM_CORRELATIONS           = "correlations";
    public static final String PARAM_CORRELATIONS_OUTPUT    = PARAM_CORRELATIONS + ".output";
    public static final String PARAM_CORRELATIONS_THRESHOLD    = PARAM_CORRELATIONS + ".threshold";
    
    public static final String PARAM_MARKOV                 = "markov";
    public static final String PARAM_MARKOV_OUTPUT          = PARAM_MARKOV + ".output";
    public static final String PARAM_MARKOV_THRESHOLDS      = PARAM_MARKOV + ".thresholds";
    public static final String PARAM_MARKOV_THRESHOLDS_OUTPUT = PARAM_MARKOV_THRESHOLDS + ".output";
    
    public static final String PARAM_DESIGNER               = "designer";
    public static final String PARAM_DESIGNER_PARTITIONER   = PARAM_DESIGNER + ".partitioner";
    public static final String PARAM_DESIGNER_MAPPER        = PARAM_DESIGNER + ".mapper";
    public static final String PARAM_DESIGNER_INDEXER       = PARAM_DESIGNER + ".indexer";
    public static final String PARAM_DESIGNER_THREADS       = PARAM_DESIGNER + ".threads";
    public static final String PARAM_DESIGNER_INTERVALS     = PARAM_DESIGNER + ".intervals";
    public static final String PARAM_DESIGNER_COSTMODEL     = PARAM_DESIGNER + ".costmodel";
    public static final String PARAM_DESIGNER_HINTS         = PARAM_DESIGNER + ".hints";
    public static final String PARAM_DESIGNER_HINTS_PREFIX  = PARAM_DESIGNER_HINTS + ".";
    public static final String PARAM_DESIGNER_CHECKPOINT    = PARAM_DESIGNER + ".checkpoint";
    
    public static final String PARAM_PARTITION_PLAN         = "partitionplan";
    public static final String PARAM_PARTITION_PLAN_OUTPUT  = PARAM_PARTITION_PLAN + ".output";
    public static final String PARAM_PARTITION_PLAN_APPLY   = PARAM_PARTITION_PLAN + ".apply";
    
    public static final String PARAM_PARTITION_MAP          = "partitionmap";
    public static final String PARAM_PARTITION_MAP_OUTPUT   = PARAM_PARTITION_MAP + ".output";
    
    public static final String PARAM_INDEX_PLAN             = "indexplan";
    public static final String PARAM_INDEX_PLAN_OUTPUT      = PARAM_INDEX_PLAN + ".output";
    
    public static final String PARAM_HASHER                 = "hasher";
    public static final String PARAM_HASHER_CLASS           = PARAM_HASHER + ".class";
    public static final String PARAM_HASHER_PROFILE         = PARAM_HASHER + ".profile";
    public static final String PARAM_HASHER_OUTPUT          = PARAM_HASHER + ".output";

    private static final String PARAM_COORDINATOR           = "coordinator";
    public static final String PARAM_COORDINATOR_HOST       = PARAM_COORDINATOR + ".host";
    public static final String PARAM_COORDINATOR_PORT       = PARAM_COORDINATOR + ".port";
    public static final String PARAM_COORDINATOR_PARTITION  = PARAM_COORDINATOR + ".partition";
    public static final String PARAM_COORDINATOR_STATUS_INTERVAL = PARAM_COORDINATOR + ".statusinterval";

    private static final String PARAM_NODE                  = "node";
    public static final String PARAM_NODE_HOST              = PARAM_NODE + ".host";
    public static final String PARAM_NODE_PORT              = PARAM_NODE + ".port";
    public static final String PARAM_NODE_PARTITION         = PARAM_NODE + ".partition";
    public static final String PARAM_NODE_SITE              = PARAM_NODE + ".site";
    
    private static final String PARAM_DTXN                  = "dtxn";
    public static final String PARAM_DTXN_CONF              = PARAM_DTXN + ".conf";
    public static final String PARAM_DTXN_ENGINE            = PARAM_DTXN + ".engine";
    public static final String PARAM_DTXN_COORDINATOR       = PARAM_DTXN + ".coordinator";
    
    public static final String PARAM_SIMULATOR              = "simulator";
    public static final String PARAM_SIMULATOR_CONF_OUTPUT  = PARAM_SIMULATOR + ".conf.output";
    public static final String PARAM_SIMULATOR_HOST         = PARAM_SIMULATOR + ".host";
    public static final String PARAM_SIMULATOR_NUM_HOSTS    = PARAM_SIMULATOR + ".numhosts";
    public static final String PARAM_SIMULATOR_HOST_PARTITIONS = PARAM_SIMULATOR + ".host.partitions";
    public static final String PARAM_SIMULATOR_HOST_CORES   = PARAM_SIMULATOR + ".host.cores";
    public static final String PARAM_SIMULATOR_HOST_THREADS = PARAM_SIMULATOR + ".host.threads";
    public static final String PARAM_SIMULATOR_HOST_MEMORY  = PARAM_SIMULATOR + ".host.memory";
    public static final String PARAM_SIMULATOR_PORT         = PARAM_SIMULATOR + ".port";
    public static final String PARAM_SIMULATOR_ID           = PARAM_SIMULATOR + ".id";
    public static final String PARAM_SIMULATOR_PARTITION    = PARAM_SIMULATOR + ".partition";
    public static final String PARAM_SIMULATOR_CLIENT_THREADS = PARAM_SIMULATOR + ".client.threads";
    public static final String PARAM_SIMULATOR_CLIENT_TIME  = PARAM_SIMULATOR + ".client.time";
    
    public static final List<String> PARAMS = new ArrayList<String>();
    static {
        for (Field field : ArgumentsParser.class.getDeclaredFields()) {
            try {
                if (field.getName().startsWith("PARAM_")) {
                    ArgumentsParser.PARAMS.add(field.get(null).toString());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        } // FOR
    }; // STATIC
    
    /**
     * Parameter Key -> Value
     */
    private final Map<String, String> params = new LinkedHashMap<String, String>();
    
    /**
     * "Leftover" Parameters (getopt style)
     */
    private final List<String> opt_params = new ArrayList<String>();
    
    /**
     * Special Case: Designer Hints Override
     */
    private final Map<String, String> hints_params = new ListOrderedMap<String, String>();
    
    /**
     * Catalog Attributes
     */
    public Catalog catalog = null;
    public Database catalog_db = null;
    public File catalog_path = null;
    public ProjectType catalog_type = null;
    
    /**
     * Workload Trace Attributes
     */
    public AbstractWorkload workload = null;
    public String workload_path = null;
    public Long workload_xact_limit = null;
    public Long workload_xact_offset = 0l;
    public Long workload_query_limit = null;
    public AbstractWorkload.Filter workload_filter = null;
    
    /**
     * Workload Statistics Attributes
     */
    public WorkloadStatistics stats = null;
    public String stats_path = null;
    
    /**
     * Transaction Estimation Stuff
     */
    public final ParameterCorrelations param_correlations = new ParameterCorrelations();
    public EstimationThresholds thresholds;
    
    /**
     * Designer Components
     */
    public int max_concurrent = 1;
    public int num_intervals = 100;
    public final DesignerHints designer_hints = new DesignerHints();
    public File designer_checkpoint;
    public Class<? extends AbstractPartitioner> partitioner_class = HeuristicPartitioner.class;
    public Class<? extends AbstractMapper> mapper_class = AffinityMapper.class;
    public Class<? extends AbstractIndexSelector> indexer_class = SimpleIndexSelector.class;
    public PartitionPlan pplan;
    public PartitionMapping pmap;
    public IndexPlan iplan;
    public Class<? extends AbstractCostModel> costmodel_class = SingleSitedCostModel.class;
    public AbstractCostModel costmodel;
    
    /**
     * Hasher
     */
    public Class<? extends AbstractHasher> hasher_class = DefaultHasher.class;
    public AbstractHasher hasher;
    
    /**
     * Empty Constructor
     */
    public ArgumentsParser() {
        // Nothing to do
    }
    
    public List<String> getOptParams() {
        return (this.opt_params);
    }
    
    public int getOptParamCount() { 
        return (this.opt_params.size());
    }
    
    public String getOptParam(int idx) {
        return (this.opt_params.get(idx));
    }
    
    public Integer getIntOptParam(int idx) {
        String val = this.opt_params.get(idx);
        Integer ret = null;
        if (val != null) ret = Integer.valueOf(val);
        return (ret);
    }
    
    public Long getLongOptParam(int idx) {
        String val = this.opt_params.get(idx);
        Long ret = null;
        if (val != null) ret = Long.valueOf(val);
        return (ret);
    }
    
    public Map<String, String> getParams() {
        return (this.params);
    }
    
    public String getParam(String key) {
        return (this.params.get(key));
    }
    
    public Integer getIntParam(String key) {
        String val = this.params.get(key);
        Integer ret = null;
        if (val != null) ret = Integer.valueOf(val);
        return (ret);
    }
    
    public Long getLongParam(String key) {
        String val = this.params.get(key);
        Long ret = null;
        if (val != null) ret = Long.valueOf(val);
        return (ret);
    }

    public Double getDoubleParam(String key) {
        String val = this.params.get(key);
        Double ret = null;
        if (val != null) ret = Double.valueOf(val);
        return (ret);
    }
    
    public Boolean getBooleanParam(String key) {
        String val = this.params.get(key);
        Boolean ret = null;
        if (val != null) ret = Boolean.valueOf(val);
        return (ret);
    }
    
    public boolean hasParam(String key) {
        return (this.params.get(key) != null);
    }
    
    public boolean hasIntParam(String key) {
        if (this.hasParam(key)) {
            try {
                Long val = Long.valueOf(this.params.get(key));
                if (val != null) return (true);
            } catch (NumberFormatException ex) {
                // Nothing...
            }
        }
        return (false);
    }
    
    public boolean hasDoubleParam(String key) {
        if (this.hasParam(key)) {
            try {
                Double val = Double.valueOf(this.params.get(key));
                if (val != null) return (true);
            } catch (NumberFormatException ex) {
                // Nothing...
            }
        }
        return (false);
    }
    
    public void setDatabase(Database catalog_db) {
        this.catalog_db = catalog_db;
    }
    
    /**
     * Check whether they have all the parameters they need
     * @param params
     * @throws IllegalArgumentException
     */
    public void require(String...params) throws IllegalArgumentException {
        for (String param : params) {
            if (!this.hasParam(param)) {
                throw new IllegalArgumentException("Missing parameter '" + param + "'. Required Parameters = " + Arrays.asList(params));
            }
        } // FOR
        return;
    }
    
    /**
     * Return an object with the proper objects loaded
     * @param args
     * @return
     * @throws Exception
     */
    public static ArgumentsParser load(String args[]) throws Exception {
        ArgumentsParser au = new ArgumentsParser();
        au.process(args);
        
//        System.out.println("catalog: " + au.catalog);
//        System.out.println("catalog_db: " + au.catalog_db);
//        System.out.println("workload: " + au.workload);
//        System.out.println("workload.limit: " + au.workload_limit);
//        System.out.println("stats: " + au.stats);
        
        return (au);
    }
    
    /**
     * 
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void process(String[] args) throws Exception {
        for (String arg : args) {
            String[] parts = arg.split("=",2);
            if (parts[0].startsWith("-")) parts[0] = parts[0].substring(1);
            parts[0] = parts[0].toLowerCase();
            
            if (parts.length == 1) {
                if (!parts[0].startsWith("${")) this.opt_params.add(parts[0]);
            } else if (parts[1].startsWith("${")) {
                continue;
            } else if (parts[0].startsWith(PARAM_DESIGNER_HINTS_PREFIX)) {
                String param = parts[0].replace(PARAM_DESIGNER_HINTS_PREFIX, "").toUpperCase();
                DesignerHints.Members m = EnumUtil.get(DesignerHints.Members.values(), param);
                if (m == null) throw new Exception("Unknown DesignerHints parameter: " + param);
                this.hints_params.put(m.name(), parts[1]);
            } else if (PARAMS.contains(parts[0])) {
                this.params.put(parts[0], parts[1]);
            } else if (parts[0].startsWith("#")) {
                // IGNORE!
            } else {
                throw new Exception("Unknown parameter '" + parts[0] + "'");
            }
        } // FOR
        
        // -------------------------------------------------------
        // CATALOGS
        // -------------------------------------------------------
        
        //
        // Text File
        //
        if (this.params.containsKey(PARAM_CATALOG)) {
            String path = this.params.get(PARAM_CATALOG);
            LOG.debug("Loading catalog from file '" + path + "'");
            this.catalog = CatalogUtil.loadCatalog(path);
            if (this.catalog == null) throw new Exception("Failed to load catalog object from file '" + path + "'");
            this.catalog_db = CatalogUtil.getDatabase(catalog);
            this.catalog_path = new File(path);
        //
        // Jar File
        //
        } else if (this.params.containsKey(PARAM_CATALOG_JAR)) {
            String path = this.params.get(PARAM_CATALOG_JAR);
            this.params.put(PARAM_CATALOG, path);
            File jar_file = new File(path);
            this.catalog = CatalogUtil.loadCatalogFromJar(path);
            if (this.catalog == null) throw new Exception("Failed to load catalog object from jar file '" + path + "'");
            LOG.debug("Loaded catalog from jar file '" + path + "'");
            this.catalog_db = CatalogUtil.getDatabase(catalog);
            this.catalog_path = jar_file;
            
            if (!this.params.containsKey(PARAM_CATALOG_TYPE)) {
                String jar_name = jar_file.getName();
                int jar_idx = jar_name.lastIndexOf(".jar");
                if (jar_idx != -1) {
                    ProjectType type = ProjectType.get(jar_name.substring(0, jar_idx));
                    if (type != null) {
                        LOG.debug("Set catalog type '" + type + "' from catalog jar file name");
                        this.catalog_type = type;
                        this.params.put(PARAM_CATALOG_TYPE, this.catalog_type.toString());
                    }
                }
            }
        //
        // Schema File
        //
        } else if (this.params.containsKey(PARAM_CATALOG_SCHEMA)) {
            String path = this.params.get(PARAM_CATALOG_SCHEMA); 
            this.catalog = CompilerUtil.compileCatalog(path);
            if (this.catalog == null) throw new Exception("Failed to load schema from '" + path + "'");
            LOG.debug("Loaded catalog from schema file '" + path + "'");
            this.catalog_db = CatalogUtil.getDatabase(catalog);
            this.catalog_path = new File(path);
        }
        
        //
        // Catalog Type
        //
        if (this.params.containsKey(PARAM_CATALOG_TYPE)) {
            String catalog_type = this.params.get(PARAM_CATALOG_TYPE);
            ProjectType type = ProjectType.get(catalog_type);
            if (type == null) {
                throw new Exception("Unknown catalog type '" + catalog_type + "'");
            }
            this.catalog_type = type;
        }
        
        // -------------------------------------------------------
        // PHYSICAL DESIGN COMPONENTS
        // -------------------------------------------------------
        if (this.params.containsKey(PARAM_PARTITION_PLAN)) {
            assert(this.catalog_db != null);
            File path = new File(this.params.get(PARAM_PARTITION_PLAN));
            LOG.debug("Loading in partition plan from '" + path + "'");
            this.pplan = new PartitionPlan();
            this.pplan.load(path.getAbsolutePath(), this.catalog_db);
            
            // Apply!
            if (this.params.containsKey(PARAM_PARTITION_PLAN_APPLY) && this.getBooleanParam(PARAM_PARTITION_PLAN_APPLY)) {
                LOG.info("Applying PartitionPlan '" + path.getName() + "' to catalog");
                this.pplan.apply(this.catalog_db);
            }
        }
        
        // -------------------------------------------------------
        // SAMPLE WORKLOAD TRACE
        // -------------------------------------------------------
        
        //
        // Workload Trace
        //
        if (this.params.containsKey(PARAM_WORKLOAD)) {
            assert(this.catalog_db != null);
            String path = new File(this.params.get(PARAM_WORKLOAD)).getAbsolutePath();
            
            // This will prune out duplicate trace records...
            if (params.containsKey(PARAM_WORKLOAD_REMOVE_DUPES)) {
                this.workload_filter = new DuplicateTraceFilter();
            }
            
            // Include/exclude procedures from the traces
            if (params.containsKey(PARAM_WORKLOAD_PROC_INCLUDE) || params.containsKey(PARAM_WORKLOAD_PROC_EXCLUDE)) {
                ProcedureNameFilter filter = new ProcedureNameFilter();
                if (this.workload_filter != null) {
                    this.workload_filter.attach(filter);
                } else {
                    this.workload_filter = filter;
                }
                
                String temp = params.get(PARAM_WORKLOAD_PROC_INCLUDE);
                if (temp != null && !temp.equals(ProcedureNameFilter.INCLUDE_ALL)) {
                    
                    // We can take the counts for PROC_INCLUDE and scale them with the multiplier
                    double multiplier = 1.0d;
                    if (this.hasDoubleParam(PARAM_WORKLOAD_PROC_INCLUDE_MULTIPLIER)) {
                        multiplier = this.getDoubleParam(PARAM_WORKLOAD_PROC_INCLUDE_MULTIPLIER);
                    }
                    
                    for (String proc_name : params.get(PARAM_WORKLOAD_PROC_INCLUDE).split(",")) {
                        int limit = -1;
                        // Check if there is a limit for this procedure
                        if (proc_name.contains(":")) {
                            String pieces[] = proc_name.split(":");
                            proc_name = pieces[0];
                            limit = (int)Math.round(Integer.parseInt(pieces[1]) * multiplier);
                        }
                        filter.include(proc_name, limit);
                    } // FOR
                }
                temp = params.get(PARAM_WORKLOAD_PROC_EXCLUDE);
                if (temp != null) {
                    for (String proc_name : params.get(PARAM_WORKLOAD_PROC_EXCLUDE).split(",")) {
                        filter.exclude(proc_name);
                    } // FOR
                }
            }
            if (params.containsKey(PARAM_WORKLOAD_XACT_OFFSET)) {
                this.workload_xact_offset = Long.parseLong(params.get(PARAM_WORKLOAD_XACT_OFFSET));
            }
            if (params.containsKey(PARAM_WORKLOAD_XACT_LIMIT)) {
                this.workload_xact_limit = Long.parseLong(params.get(PARAM_WORKLOAD_XACT_LIMIT));
                ProcedureLimitFilter filter = new ProcedureLimitFilter(this.workload_xact_limit, this.workload_xact_offset);
                if (this.workload_filter != null) {
                    this.workload_filter.attach(filter);
                } else {
                    this.workload_filter = filter;
                }
            } else if (this.workload_xact_offset > 0) {
                ProcedureLimitFilter filter = new ProcedureLimitFilter(-1l, this.workload_xact_offset);
                if (this.workload_filter != null) {
                    this.workload_filter.attach(filter);
                } else {
                    this.workload_filter = filter;
                }
            }
            if (params.containsKey(PARAM_WORKLOAD_QUERY_LIMIT)) {
                this.workload_query_limit = Long.parseLong(params.get(PARAM_WORKLOAD_QUERY_LIMIT));
                QueryLimitFilter filter = new QueryLimitFilter(this.workload_query_limit);
                if (this.workload_filter != null) {
                    this.workload_filter.attach(filter);
                } else {
                    this.workload_filter = filter;
                }
            }
            if (this.workload_filter != null) LOG.debug("Workload Filters: " + this.workload_filter);
            this.workload = new WorkloadTraceFileOutput(this.catalog);
            ((WorkloadTraceFileOutput)this.workload).load(path, this.catalog_db, this.workload_filter);
            this.workload_path = new File(path).getAbsolutePath();
            if (this.workload_filter != null) this.workload_filter.reset();
        }
        
        //
        // Workload Statistics
        //
        if (this.catalog_db != null) {
            this.stats = new WorkloadStatistics(this.catalog_db);
            if (this.params.containsKey(PARAM_STATS)) {
                String path = this.params.get(PARAM_STATS);
                LOG.debug("Loading in workload statistics from '" + path + "'");
                this.stats.load(path, this.catalog_db);
                this.stats_path = new File(path).getAbsolutePath();
            }
        }
        
        // -------------------------------------------------------
        // DESIGNER COMPONENTS
        // -------------------------------------------------------

        if (this.params.containsKey(PARAM_DESIGNER_THREADS)) {
            this.max_concurrent = Integer.valueOf(this.params.get(PARAM_DESIGNER_THREADS)); 
        }
        if (this.params.containsKey(PARAM_DESIGNER_INTERVALS)) {
            this.num_intervals = Integer.valueOf(this.params.get(PARAM_DESIGNER_INTERVALS)); 
        }
        if (this.params.containsKey(PARAM_DESIGNER_HINTS)) {
            String path = this.params.get(PARAM_DESIGNER_HINTS);
            LOG.debug("Loading in designer hints from '" + path + "'");
            this.designer_hints.load(path, catalog_db, this.hints_params);
        }
        if (this.params.containsKey(PARAM_DESIGNER_CHECKPOINT)) {
            this.designer_checkpoint = new File(this.params.get(PARAM_DESIGNER_CHECKPOINT));
        }
        
        String designer_attributes[] = { PARAM_DESIGNER_PARTITIONER,
                                         PARAM_DESIGNER_MAPPER,
                                         PARAM_DESIGNER_INDEXER,
                                         PARAM_DESIGNER_COSTMODEL };
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        for (String key : designer_attributes) {
            if (this.params.containsKey(key)) {
                String target_name = this.params.get(key);
                Class<?> target_class = loader.loadClass(target_name);
                assert(target_class != null);
                LOG.debug("Set " + key + " class to " + target_class.getName());
                
                if (key.equals(PARAM_DESIGNER_PARTITIONER)) {
                    this.partitioner_class = (Class<? extends AbstractPartitioner>)target_class;
                } else if (key.equals(PARAM_DESIGNER_MAPPER)) {
                    this.mapper_class = (Class<? extends AbstractMapper>)target_class;
                } else if (key.equals(PARAM_DESIGNER_INDEXER)) {
                    this.indexer_class = (Class<? extends AbstractIndexSelector>)target_class;
                } else if (key.equals(PARAM_DESIGNER_COSTMODEL)) {
                    this.costmodel_class = (Class<? extends AbstractCostModel>)target_class;
                    
                    // Special Case: TimeIntervalCostModel
                    if (target_name.endsWith(TimeIntervalCostModel.class.getSimpleName())) {
                        this.costmodel = new TimeIntervalCostModel<SingleSitedCostModel>(this.catalog_db, SingleSitedCostModel.class, this.num_intervals);
                    } else {
                        this.costmodel = ClassUtil.newInstance(this.costmodel_class, new Object[]{this.catalog_db}, new Class[]{Database.class});
                    }
                } else {
                    assert(false) : "Invalid key '" + key + "'";
                }
            }
        } // FOR

        // -------------------------------------------------------
        // TRANSACTION ESTIMATION COMPONENTS
        // -------------------------------------------------------
        if (this.params.containsKey(PARAM_CORRELATIONS)) {
            assert(this.catalog_db != null);
            File path = new File(this.params.get(PARAM_CORRELATIONS));
            if (path.exists()) {
                this.param_correlations.load(path.getAbsolutePath(), this.catalog_db);
            } else {
                LOG.warn("The parameter correlations file '" + path + "' does not exist");
            }
        }
        if (this.params.containsKey(PARAM_MARKOV_THRESHOLDS)) {
            assert(this.catalog_db != null);
            this.thresholds = new EstimationThresholds();
            File path = new File(this.params.get(PARAM_MARKOV_THRESHOLDS));
            if (path.exists()) {
                this.thresholds.load(path.getAbsolutePath(), this.catalog_db);
            } else {
                LOG.warn("The estimation thresholds file '" + path + "' does not exist");
            }
        }
        
        // -------------------------------------------------------
        // HASHER
        // -------------------------------------------------------
        if (this.catalog != null) {
            if (this.params.containsKey(PARAM_HASHER_CLASS)) {
                String hasherClassName = this.params.get(PARAM_HASHER_CLASS);
                this.hasher_class = (Class<? extends AbstractHasher>)loader.loadClass(hasherClassName);
            }
            Constructor<? extends AbstractHasher> constructor = this.hasher_class.getConstructor(new Class[]{ Database.class, Integer.class });
            int num_partitions = CatalogUtil.getNumberOfPartitions(this.catalog_db);
            this.hasher = constructor.newInstance(new Object[]{ this.catalog_db, num_partitions });
            if (!(this.hasher instanceof DefaultHasher)) LOG.debug("Loaded hasher " + this.hasher.getClass());
            
            if (this.params.containsKey(PARAM_HASHER_PROFILE)) {
                this.hasher.load(this.params.get(PARAM_HASHER_PROFILE), null);
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
//        try {
//            Class<?> arg_class = this.getClass();
//            for (ArgumentsParser.Attribute element : ArgumentsParser.Attribute.values()) {
//                Field field = arg_class.getField(element.toString().toLowerCase());
//                Object value = field.get(this); 
//                buffer.append(element).append(":\t");
//                if (value instanceof Class<?>) {
//                    buffer.append(((Class<?>)value).getCanonicalName());
//                } else {
//                    buffer.append(value);
//                }
//                buffer.append("\n");
//            } // FOR
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
        return buffer.toString();
    }
}
