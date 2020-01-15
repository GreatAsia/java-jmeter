package com.okay;


import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.assertions.gui.AssertionGui;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.RandomVariableConfig;
import org.apache.jmeter.config.RandomVariableConfigBeanInfo;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.RandomController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.RandomControlGui;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.CookiePanel;
import org.apache.jmeter.protocol.http.gui.HTTPArgumentsPanel;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.SummaryReport;
import org.apache.jorphan.collections.HashTree;
import org.joor.Reflect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author zhou
 * @date 2019/11/25
 */
public class Performance {

    private static final String JMETERHOME = "D:\\apache-jmeter-5.1.1";
    private static String jmxFile = "report\\jmeter_api_sample.jmx";
    private static String reportFile = "report\\report.jtl";
    private static String csvFile = "report\\report.csv";


    public static void main(String[] args) throws IOException {


        //Set jmeter home for the jmeter utils to load
        File jmeterHome = new File(JMETERHOME);
        String slash = System.getProperty("file.separator");

        if (jmeterHome.exists()) {
            String path = TestPlanLauncher.class.getClassLoader().getResource("jmeter.properties").getPath();
            File jmeterProperties = new File(path);
//            File jmeterProperties = new File(jmeterHome.getPath() + slash + "bin" + slash + "jmeter.properties");

            if (jmeterProperties.exists()) {
                //Jmeter引擎
                StandardJMeterEngine standardJMeterEngine = new StandardJMeterEngine();
                //JMeter initialization (properties, log levels, locale, etc)
                JMeterUtils.setJMeterHome(jmeterHome.getPath());
                JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
                JMeterUtils.initLogging();// you can comment this line out to see extra log messages of i.e. DEBUG level
                JMeterUtils.initLocale();

                // 创建测试计划
                TestPlan testPlan = createTestPlan();
                // 创建http请求收集器
                HTTPSamplerProxy examplecomSampler = createHTTPSamplerProxy();
                // 创建循环控制器
                LoopController loopController = createLoopController();
                // 创建线程组
                ThreadGroup threadGroup = createThreadGroup();
                //创建检查点
                ResponseAssertion responseAssertion = createResponseAssertion();
                // 线程组设置循环控制
                threadGroup.setSamplerController(loopController);
                //创建cookie
                CookieManager cookieManager = createCookieManager();
                //创建header
                HeaderManager headerManager = createHeaderManager();
                //创建随机生成数
                RandomVariableConfig randomVariableConfig = createRandomVariableConfig();

                // JMeter Test Plan, basically JOrphan HashTree
                HashTree testPlanTree = new HashTree();
                // 将测试计划添加到测试配置树种
                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                // 将http请求采样器添加到线程组下
                threadGroupHashTree.add(examplecomSampler);
                threadGroupHashTree.add(headerManager);
                threadGroupHashTree.add(cookieManager);
                threadGroupHashTree.add(responseAssertion);
                threadGroupHashTree.add(randomVariableConfig);

                // save generated test plan to JMeter's .jmx file format
                SaveService.saveTree(testPlanTree, new FileOutputStream(jmxFile));

                //增加结果收集
                //add Summarizer output to get test progress in stdout like:
                // summary =      2 in   1.3s =    1.5/s Avg:   631 Min:   290 Max:   973 Err:     0 (0.00%)
                SummariserUtils summer = null;
                String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
                if (summariserName.length() > 0) {
                    summer = new SummariserUtils(summariserName);
                }

                // Store execution results into a .jtl file, we can save file as csv also
                ResultCollectorUtil logger = new ResultCollectorUtil(summer);
                logger.setFilename(reportFile);
//                ResultCollectorUtil csvlogger = new ResultCollectorUtil(summer);
//                csvlogger.setFilename(csvFile);
                testPlanTree.add(testPlanTree.getArray()[0], logger);
//                testPlanTree.add(testPlanTree.getArray()[0], csvlogger);

                // Run Test Plan
                standardJMeterEngine.configure(testPlanTree);
                standardJMeterEngine.run();

                //获取聚合报告结果
                String info = Reflect.on(summer).get("summerMessage");
                System.out.println("info:" + info);

                System.out.println("Test completed. See " + jmeterHome + slash + "report.jtl file for results");
                System.out.println("JMeter .jmx script is available at " + jmeterHome + slash + "jmeter_api_sample.jmx");

            }

        }

    }


    /**
     * 创建测试计划
     * <p>
     * return TestPlan
     */
    public static TestPlan createTestPlan() {

        //创建测试计划
        TestPlan testPlan = new TestPlan();

        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        //用户自定义变量
        testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());
        testPlan.setFunctionalMode(false);
        testPlan.setSerialized(true);
        testPlan.setTearDownOnShutdown(true);


        testPlan.setName("测试计划");
        testPlan.setEnabled(true);
        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());

        return testPlan;
    }


    /**
     * 创建线程组
     *
     * @return
     */
    public static ThreadGroup createThreadGroup() {

        // Thread Group
        ThreadGroup threadGroup = new ThreadGroup();

        threadGroup.setSamplerController(createLoopController());
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(0);
        threadGroup.setScheduler(true);
        threadGroup.setDuration(5);
        threadGroup.setDelay(0);
        threadGroup.setProperty("ThreadGroup.on_sample_error","continue");

        threadGroup.setName("线程组");
        threadGroup.setEnabled(true);
        threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());

        return threadGroup;
    }

    /**
     * 创建循环控制器
     *
     * @return
     */
    public static LoopController createLoopController() {
        // Loop Controller
        LoopController loopController = new LoopController();
        loopController.setLoops(-1);
        loopController.setContinueForever(false);

        loopController.setName("控制器");
        loopController.setEnabled(true);
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.initialize();
        return loopController;
    }

    /**
     * 创建http采样器
     *
     * @return
     */
    public static HTTPSamplerProxy createHTTPSamplerProxy() {

        HTTPSamplerProxy httpSamplerProxy = new HTTPSamplerProxy();


        //TODO：区分Get  Post  Post-Json请求
        //POST_JSON
//        httpSamplerProxy.setPostBodyRaw(true);
//        //添加body参数
//        HTTPArgument httpArgument = new HTTPArgument();
//        httpArgument.setAlwaysEncoded(false);
//        httpArgument.setValue(data);
//        httpArgument.setMetaData("=");
//        Arguments arguments = new Arguments();
//        arguments.addArgument(httpArgument);
//
//        arguments.setName("Body参数");
//        arguments.setEnabled(true);
//        arguments.setProperty(TestElement.TEST_CLASS, Arguments.class.getName());
//        arguments.setProperty(TestElement.GUI_CLASS, HTTPArgumentsPanel.class.getName());
//        httpSamplerProxy.setArguments(arguments);


        //POST_FORM请求
        Arguments arguments = new Arguments();
        String params = "id=1164647&type=1";
        String[] paramArray = params.split("&");
        for (int i = 0; i < paramArray.length; i++) {

            String arg[] = paramArray[i].split("=");
            HTTPArgument httpArgument = new HTTPArgument();
            httpArgument.setAlwaysEncoded(false);
            httpArgument.setValue(arg[1]);
            httpArgument.setMetaData("=");
            httpArgument.setUseEquals(true);
            httpArgument.setName(arg[0]);
            arguments.addArgument(httpArgument);
        }

        arguments.setName("Paramters参数");
        arguments.setEnabled(true);
        arguments.setProperty(TestElement.TEST_CLASS, Arguments.class.getName());
        arguments.setProperty(TestElement.GUI_CLASS, HTTPArgumentsPanel.class.getName());
        httpSamplerProxy.setArguments(arguments);

//        //GET请求
//        httpSamplerProxy.setPath("HOST+PATH" + "?" + "Params");
//        httpSamplerProxy.setMethod("GET");


        httpSamplerProxy.setPath("https://jiaoshi-dev.xk12.cn/resource/res_info");
        httpSamplerProxy.setMethod("POST");
        httpSamplerProxy.setFollowRedirects(true);
        httpSamplerProxy.setAutoRedirects(false);
        httpSamplerProxy.setUseKeepAlive(true);
        httpSamplerProxy.setDoMultipart(false);
        httpSamplerProxy.setConnectTimeout("5000");
        //配置属性
        httpSamplerProxy.setName("已发布课程");
        httpSamplerProxy.setEnabled(true);
        httpSamplerProxy.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSamplerProxy.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

        return httpSamplerProxy;
    }


    /**
     * 设置检查点
     * return
     */

    private static ResponseAssertion createResponseAssertion() {

        //设置检查点
        ResponseAssertion responseAssertion = new ResponseAssertion();
        responseAssertion.addTestString("\"code\":0");
        responseAssertion.setTestFieldResponseData();
        responseAssertion.setToContainsType();

        responseAssertion.setName("响应断言");
        responseAssertion.setEnabled(true);
        responseAssertion.setProperty(TestElement.TEST_CLASS, ResponseAssertion.class.getName());
        responseAssertion.setProperty(TestElement.GUI_CLASS, AssertionGui.class.getName());
        return responseAssertion;
    }

    private static CookieManager createCookieManager() {

        CookieManager cookieManager = new CookieManager();


        Map<String, String> cookies = new HashMap<>();
        cookies.put("org_id", "132");
        cookies.put("user_action_cookie", "user_action_60b01bda-dda1-4c18-83b4-64901f1f089d_61951058256");
        cookies.put("teacher_id", "4f6e7efbd77a4fc98fce5e18529448cc");
        cookies.put("noriental", "945378f9-c4f4-46e3-b95e-e855ff7230c0");

        Iterator cookieTt = cookies.entrySet().iterator();
        while (cookieTt.hasNext()) {
            Map.Entry entry = (Map.Entry) cookieTt.next();
            Cookie cookie = new Cookie();

            cookie.setName(entry.getKey().toString());
            cookie.setValue(entry.getValue().toString());
            cookie.setDomain("jiaoshi-dev.xk12.cn");
            cookie.setPath("/");
            cookie.setSecure(true);
            cookie.setVersion(0);
            cookie.setDomainSpecified(true);
            cookie.setPathSpecified(true);
            cookieManager.add(cookie);
        }

        cookieManager.setName("cookie管理器");
        cookieManager.setEnabled(true);
        cookieManager.setProperty(TestElement.TEST_CLASS, CookieManager.class.getName());
        cookieManager.setProperty(TestElement.GUI_CLASS, CookiePanel.class.getName());


        System.out.println("count" + cookieManager.getCookieCount());
        return cookieManager;
    }

    private static HeaderManager createHeaderManager() {

        HeaderManager headerManager = new HeaderManager();

        Map<String, String> headers = new HashMap<>();
        headers.put("requestId", "${requestid}");
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Header header = new Header();
            header.setName(entry.getKey().toString());
            header.setValue(entry.getValue().toString());
            headerManager.add(header);
        }

        headerManager.setName("header管理器");
        headerManager.setEnabled(true);
        headerManager.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
        headerManager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());


        return headerManager;

    }


    private static RandomVariableConfig createRandomVariableConfig(){
        RandomVariableConfig randomVariableConfig = new RandomVariableConfig();

        randomVariableConfig.setProperty("maximumValue","99999999999999999");
        randomVariableConfig.setProperty("minimumValue","11111111111111111");
        randomVariableConfig.setProperty("outputFormat","zhang-");
        randomVariableConfig.setProperty("perThread",true);
        randomVariableConfig.setProperty("randomSeed","");
        randomVariableConfig.setProperty("variableName","requestid");

        randomVariableConfig.setName("Random Variable");
        randomVariableConfig.setEnabled(true);
        randomVariableConfig.setProperty(TestElement.TEST_CLASS, RandomVariableConfig.class.getName());
        randomVariableConfig.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());


        return randomVariableConfig;
    }



   private static ResultCollector createResultCollector(){

       ResultCollector resultCollector = new ResultCollector();
       resultCollector.setErrorLogging(false);
       resultCollector.setFilename("");

       SampleSaveConfiguration sampleSaveConfiguration = new SampleSaveConfiguration();
       sampleSaveConfiguration.setTime(true);
       sampleSaveConfiguration.setTimestamp(true);
       sampleSaveConfiguration.setSuccess(true);
       sampleSaveConfiguration.setLabel(true);
       sampleSaveConfiguration.setCode(true);
       sampleSaveConfiguration.setMessage(true);
       sampleSaveConfiguration.setThreadName(true);
       sampleSaveConfiguration.setDataType(true);
       sampleSaveConfiguration.setEncoding(false);
       sampleSaveConfiguration.setAssertions(true);
       sampleSaveConfiguration.setSubresults(true);
       sampleSaveConfiguration.setResponseData(true);
       sampleSaveConfiguration.setSamplerData(true);
       sampleSaveConfiguration.setAsXml(false);
       sampleSaveConfiguration.setFieldNames(true);
       sampleSaveConfiguration.setRequestHeaders(true);
       sampleSaveConfiguration.setAssertionResultsFailureMessage(true);
       sampleSaveConfiguration.setBytes(true);
       sampleSaveConfiguration.setSentBytes(true);
       sampleSaveConfiguration.setUrl(true);
       sampleSaveConfiguration.setFileName(true);
       sampleSaveConfiguration.setHostname(true);
       sampleSaveConfiguration.setThreadCounts(true);
       sampleSaveConfiguration.setSampleCount(true);
       sampleSaveConfiguration.setConnectTime(true);



       resultCollector.setSaveConfig(sampleSaveConfiguration);
       resultCollector.setName("汇总报告");
       resultCollector.setEnabled(true);
       resultCollector.setProperty(TestElement.TEST_CLASS, ResultCollector.class.getName());
       resultCollector.setProperty(TestElement.GUI_CLASS, SummaryReport.class.getName());


       return  resultCollector;

   }
}
