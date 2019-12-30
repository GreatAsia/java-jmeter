package com.okay;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.reporters.AbstractListenerElement;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.ResultCollectorHelper;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.samplers.Clearable;
import org.apache.jmeter.samplers.Remoteable;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.Visualizer;
import org.apache.jorphan.util.JMeterError;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhou
 * @date 2019/12/26
 */
public class ResultCollectorUtil extends AbstractListenerElement implements SampleListener, Clearable, Serializable, TestStateListener, Remoteable, NoThreadClone {

    private static final Logger log = LoggerFactory.getLogger(ResultCollectorUtil.class);
    private static final long serialVersionUID = 234L;
    private static final String TEST_IS_LOCAL = "*local*";
    private static final String TESTRESULTS_START = "<testResults>";
    private static final String TESTRESULTS_START_V1_1_PREVER = "<testResults version=\"";
    private static final String TESTRESULTS_START_V1_1_POSTVER = "\">";
    private static final String TESTRESULTS_END = "</testResults>";
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final int MIN_XML_FILE_LEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".length() + "<testResults>".length() + "</testResults>".length();
    public static final String FILENAME = "filename";
    private static final String SAVE_CONFIG = "saveConfig";
    private static final String ERROR_LOGGING = "ResultCollector.error_logging";
    private static final String SUCCESS_ONLY_LOGGING = "ResultCollector.success_only_logging";
    private static final boolean SAVING_AUTOFLUSH = JMeterUtils.getPropDefault("jmeter.save.saveservice.autoflush", false);
    private static final Object LOCK = new Object();
    private static final Map<String, ResultCollectorUtil.FileEntry> files = new HashMap();
    private static Thread shutdownHook;
    private static int instanceCount;
    private transient volatile PrintWriter out;
    private volatile boolean inTest;
    private volatile boolean isStats;
    private volatile SummariserUtils summariser;

    public ResultCollectorUtil() {
        this((SummariserUtils)null);
    }

    public ResultCollectorUtil(SummariserUtils summer) {
        this.inTest = false;
        this.isStats = false;
        this.setErrorLogging(false);
        this.setSuccessOnlyLogging(false);
        this.setProperty(new ObjectProperty("saveConfig", new SampleSaveConfiguration()));
        this.summariser = summer;
    }
     @Override
    public Object clone() {
         ResultCollectorUtil clone = (ResultCollectorUtil)super.clone();
        clone.setSaveConfig((SampleSaveConfiguration)clone.getSaveConfig().clone());
        clone.summariser = this.summariser;
        return clone;
    }

    private void setFilenameProperty(String f) {
        this.setProperty("filename", f);
    }

    public String getFilename() {
        return this.getPropertyAsString("filename");
    }

    public boolean isErrorLogging() {
        return this.getPropertyAsBoolean("ResultCollector.error_logging");
    }

    public final void setErrorLogging(boolean errorLogging) {
        this.setProperty(new BooleanProperty("ResultCollector.error_logging", errorLogging));
    }

    public final void setSuccessOnlyLogging(boolean value) {
        if (value) {
            this.setProperty(new BooleanProperty("ResultCollector.success_only_logging", true));
        } else {
            this.removeProperty("ResultCollector.success_only_logging");
        }

    }

    public boolean isSuccessOnlyLogging() {
        return this.getPropertyAsBoolean("ResultCollector.success_only_logging", false);
    }

    public boolean isSampleWanted(boolean success) {
        boolean errorOnly = this.isErrorLogging();
        boolean successOnly = this.isSuccessOnlyLogging();
        return isSampleWanted(success, errorOnly, successOnly);
    }

    public static boolean isSampleWanted(boolean success, boolean errorOnly, boolean successOnly) {
        return !errorOnly && !successOnly || success && successOnly || !success && errorOnly;
    }

    public void setFilename(String f) {
        if (!this.inTest) {
            this.setFilenameProperty(f);
        }
    }
    @Override
    public void testEnded(String host) {
        Object var2 = LOCK;
        synchronized(LOCK) {
            --instanceCount;
            if (instanceCount <= 0) {
                if (shutdownHook != null) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } else {
                    log.warn("Should not happen: shutdownHook==null, instanceCount={}", instanceCount);
                }

                finalizeFileOutput();
                this.out = null;
                this.inTest = false;
            }
        }

        if (this.summariser != null) {
            this.summariser.testEnded(host);
        }

    }
    @Override
    public void testStarted(String host) {
        Object var2 = LOCK;
        synchronized(LOCK) {
            if (instanceCount == 0) {
                shutdownHook = new Thread(new ResultCollectorUtil.ShutdownHook());
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            }

            ++instanceCount;

            try {
                if (this.out == null) {
                    try {
                        this.out = getFileWriter(this.getFilename(), this.getSaveConfig());
                    } catch (FileNotFoundException var5) {
                        this.out = null;
                    }
                }

                if (this.getVisualizer() != null) {
                    this.isStats = this.getVisualizer().isStats();
                }
            } catch (Exception var6) {
                log.error("Exception occurred while initializing file output.", var6);
            }
        }

        this.inTest = true;
        if (this.summariser != null) {
            this.summariser.testStarted(host);
        }

    }
    @Override
    public void testEnded() {
        this.testEnded("*local*");
    }
    @Override
    public void testStarted() {
        this.testStarted("*local*");
    }

    public void loadExistingFile() {
        Visualizer visualizer = this.getVisualizer();
        if (visualizer != null) {
            boolean parsedOK = false;
            String filename = this.getFilename();
            File file = new File(filename);
            if (file.exists()) {
                try {
                    FileReader fr = new FileReader(file);
                    Throwable var6 = null;

                    try {
                        BufferedReader dataReader = new BufferedReader(fr, 300);
                        Throwable var8 = null;

                        try {
                            String line = dataReader.readLine();
                            dataReader.close();
                            if (line == null) {
                                log.warn("{} is empty", filename);
                            } else if (!line.startsWith("<?xml ")) {
//                                CSVSaveService.processSamples(filename, visualizer, this);
                                parsedOK = true;
                            } else {
                                try {
                                    FileInputStream fis = new FileInputStream(file);
                                    Throwable var11 = null;

                                    try {
                                        BufferedInputStream bufferedInputStream = new BufferedInputStream(fis);
                                        Throwable var13 = null;

                                        try {
//                                            SaveService.loadTestResults(bufferedInputStream, new ResultCollectorHelper(this, visualizer));
                                            parsedOK = true;
                                        } catch (Throwable var117) {
                                            var13 = var117;
                                            throw var117;
                                        } finally {
                                            if (bufferedInputStream != null) {
                                                if (var13 != null) {
                                                    try {
                                                        bufferedInputStream.close();
                                                    } catch (Throwable var116) {
                                                        var13.addSuppressed(var116);
                                                    }
                                                } else {
                                                    bufferedInputStream.close();
                                                }
                                            }

                                        }
                                    } catch (Throwable var119) {
                                        var11 = var119;
                                        throw var119;
                                    } finally {
                                        if (fis != null) {
                                            if (var11 != null) {
                                                try {
                                                    fis.close();
                                                } catch (Throwable var115) {
                                                    var11.addSuppressed(var115);
                                                }
                                            } else {
                                                fis.close();
                                            }
                                        }

                                    }
                                } catch (Exception var121) {
                                    if (log.isWarnEnabled()) {
                                        log.warn("Failed to load {} using XStream. Error was: {}", filename, var121.toString());
                                    }
                                }
                            }
                        } catch (Throwable var122) {
                            var8 = var122;
                            throw var122;
                        } finally {
                            if (dataReader != null) {
                                if (var8 != null) {
                                    try {
                                        dataReader.close();
                                    } catch (Throwable var114) {
                                        var8.addSuppressed(var114);
                                    }
                                } else {
                                    dataReader.close();
                                }
                            }

                        }
                    } catch (Throwable var124) {
                        var6 = var124;
                        throw var124;
                    } finally {
                        if (fr != null) {
                            if (var6 != null) {
                                try {
                                    fr.close();
                                } catch (Throwable var113) {
                                    var6.addSuppressed(var113);
                                }
                            } else {
                                fr.close();
                            }
                        }

                    }
                } catch (JMeterError | RuntimeException | IOException var126) {
                    log.warn("Problem reading JTL file: {}", file, var126);
                } finally {
                    if (!parsedOK) {
                        GuiPackage.showErrorMessage("Error loading results file - see log file", "Result file loader");
                    }

                }
            } else {
                GuiPackage.showErrorMessage("Error loading results file - could not open file", "Result file loader");
            }

        }
    }

    private static void writeFileStart(PrintWriter writer, SampleSaveConfiguration saveConfig) {
        if (saveConfig.saveAsXml()) {
            writer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.print("\n");
            String pi = saveConfig.getXmlPi();
            if (pi.length() > 0) {
                writer.println(pi);
            }

            writer.print("<testResults version=\"");
            writer.print(SaveService.getVERSION());
            writer.print("\">");
            writer.print("\n");
        } else if (saveConfig.saveFieldNames()) {
            writer.println(CSVSaveService.printableFieldNamesToString(saveConfig));
        }

    }

    private static void writeFileEnd(PrintWriter pw, SampleSaveConfiguration saveConfig) {
        if (saveConfig.saveAsXml()) {
            pw.print("\n");
            pw.print("</testResults>");
            pw.print("\n");
        }

    }

    private static PrintWriter getFileWriter(String pFilename, SampleSaveConfiguration saveConfig) throws IOException {
        if (pFilename != null && pFilename.length() != 0) {
            if (log.isDebugEnabled()) {
                log.debug("Getting file: {} in thread {}", pFilename, Thread.currentThread().getName());
            }

            String filename = FileServer.resolveBaseRelativeName(pFilename);
            filename = (new File(filename)).getCanonicalPath();
            ResultCollectorUtil.FileEntry fe = (ResultCollectorUtil.FileEntry)files.get(filename);
            PrintWriter writer = null;
            boolean trimmed = true;
            if (fe == null) {
                if (saveConfig.saveAsXml()) {
                    trimmed = trimLastLine(filename);
                } else {
                    trimmed = (new File(filename)).exists();
                }

                File pdir = (new File(filename)).getParentFile();
                if (pdir != null) {
                    if (pdir.mkdirs() && log.isInfoEnabled()) {
                        log.info("Folder at {} was created", pdir.getAbsolutePath());
                    }

                    if (!pdir.exists()) {
                        log.warn("Error creating directories for {}", pdir);
                    }
                }

                writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename, trimmed)), SaveService.getFileEncoding(StandardCharsets.UTF_8.name())), SAVING_AUTOFLUSH);
                if (log.isDebugEnabled()) {
                    log.debug("Opened file: {} in thread {}", filename, Thread.currentThread().getName());
                }

                files.put(filename, new ResultCollectorUtil.FileEntry(writer, saveConfig));
            } else {
                writer = fe.pw;
            }

            if (!trimmed) {
                log.debug("Writing header to file: {}", filename);
                writeFileStart(writer, saveConfig);
            }

            return writer;
        } else {
            return null;
        }
    }

    private static boolean trimLastLine(String filename) {
        try {
            RandomAccessFile raf = new RandomAccessFile(filename, "rw");
            Throwable var2 = null;

            boolean var5;
            try {
                long len = raf.length();
                if (len >= (long)MIN_XML_FILE_LEN) {
                    raf.seek(len - (long)"</testResults>".length() - 10L);
                    long pos = raf.getFilePointer();

                    int end;
                    String line;
                    for(end = 0; (line = raf.readLine()) != null; pos = raf.getFilePointer()) {
                        end = line.indexOf("</testResults>");
                        if (end >= 0) {
                            break;
                        }
                    }

                    if (line == null) {
                        log.warn("Unexpected EOF trying to find XML end marker in {}", filename);
                        boolean var9 = false;
                        return var9;
                    }

                    raf.setLength(pos + (long)end);
                    return true;
                }

                var5 = false;
            } catch (Throwable var22) {
                var2 = var22;
                throw var22;
            } finally {
                if (raf != null) {
                    if (var2 != null) {
                        try {
                            raf.close();
                        } catch (Throwable var21) {
                            var2.addSuppressed(var21);
                        }
                    } else {
                        raf.close();
                    }
                }

            }

            return var5;
        } catch (FileNotFoundException var24) {
            return false;
        } catch (IOException var25) {
            if (log.isWarnEnabled()) {
                log.warn("Error trying to find XML terminator. {}", var25.toString());
            }

            return false;
        }
    }
    @Override
    public void sampleStarted(SampleEvent e) {
    }
    @Override
    public void sampleStopped(SampleEvent e) {
    }
    @Override
    public void sampleOccurred(SampleEvent event) {

        //获取响应数据
        SampleResult result = event.getResult();
        System.out.println("ResponseCode==" + result.getResponseCode());
        System.out.println("ResponseData==" + result.getResponseDataAsString());

        if (this.isSampleWanted(result.isSuccessful())) {
            this.sendToVisualizer(result);
            if (this.out != null && !this.isResultMarked(result) && !this.isStats) {
                SampleSaveConfiguration config = this.getSaveConfig();
                result.setSaveConfig(config);

                try {
                    if (config.saveAsXml()) {
                        SaveService.saveSampleResult(event, this.out);
                    } else {
                        CSVSaveService.saveSampleResult(event, this.out);
                    }
                } catch (Exception var5) {
                    log.error("Error trying to record a sample", var5);
                }
            }
        }

        if (this.summariser != null) {
            this.summariser.sampleOccurred(event);
        }

    }

    protected final void sendToVisualizer(SampleResult r) {
        if (this.getVisualizer() != null) {
            this.getVisualizer().add(r);
        }

    }

    private boolean isResultMarked(SampleResult res) {
        String filename = this.getFilename();
        return res.markFile(filename);
    }

    public void flushFile() {
        if (this.out != null) {
            log.info("forced flush through ResultCollector#flushFile");
            this.out.flush();
        }

    }

    private static void finalizeFileOutput() {
        Iterator var0 = files.entrySet().iterator();

        while(var0.hasNext()) {
            Entry<String, ResultCollectorUtil.FileEntry> me = (Entry)var0.next();
            String key = (String)me.getKey();
            ResultCollectorUtil.FileEntry value = (ResultCollectorUtil.FileEntry)me.getValue();

            try {
                log.debug("Closing: {}", key);
                writeFileEnd(value.pw, value.config);
                value.pw.close();
                if (value.pw.checkError()) {
                    log.warn("Problem detected during use of {}", key);
                }
            } catch (Exception var5) {
                log.error("Error closing file {}", key, var5);
            }
        }

        files.clear();
    }

    public SampleSaveConfiguration getSaveConfig() {
        try {
            return (SampleSaveConfiguration)this.getProperty("saveConfig").getObjectValue();
        } catch (ClassCastException var2) {
            this.setSaveConfig(new SampleSaveConfiguration());
            return this.getSaveConfig();
        }
    }

    public void setSaveConfig(SampleSaveConfiguration saveConfig) {
        this.getProperty("saveConfig").setObjectValue(saveConfig);
    }
    @Override
    public void clearData() {
    }

    private static final class ShutdownHook implements Runnable {
        private ShutdownHook() {
        }

        @Override
        public void run() {
            ResultCollectorUtil.log.info("Shutdown hook started");
            synchronized(ResultCollectorUtil.LOCK) {
                ResultCollectorUtil.finalizeFileOutput();
            }

            ResultCollectorUtil.log.info("Shutdown hook ended");
        }
    }

    private static class FileEntry {
        final PrintWriter pw;
        final SampleSaveConfiguration config;

        FileEntry(PrintWriter printWriter, SampleSaveConfiguration sampleSaveConfiguration) {
            this.pw = printWriter;
            this.config = sampleSaveConfiguration;
        }
    }
}
