package com.okay;


import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;



import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;

import org.apache.jorphan.collections.HashTree;
import org.joor.Reflect;


import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author zhou
 * @date 2019/11/25
 */
public class Performance {

    private static final String JMETERHOME = "D:\\apache-jmeter-5.1.1";
    private static  String jmxFile = "report\\jmeter_api_sample.jmx";
    private static  String reportFile = "report\\report.jtl";
    private static  String csvFile = "report\\report.csv";



    public static void main(String[] args) throws IOException {


       //Set jmeter home for the jmeter utils to load
        File jmeterHome = new File(JMETERHOME);
        String slash = System.getProperty("file.separator");

        if(jmeterHome.exists()) {
            File jmeterProperties = new File(jmeterHome.getPath() + slash + "bin" + slash + "jmeter.properties");

            if (jmeterProperties.exists()) {
                //Jmeter引擎
                StandardJMeterEngine standardJMeterEngine = new StandardJMeterEngine();
                //JMeter initialization (properties, log levels, locale, etc)
                JMeterUtils.setJMeterHome(jmeterHome.getPath());
                JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
                JMeterUtils.initLogging();// you can comment this line out to see extra log messages of i.e. DEBUG level
                JMeterUtils.initLocale();

                // JMeter Test Plan, basically JOrphan HashTree
                HashTree testPlanTree = new HashTree();
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

                // 将测试计划添加到测试配置树种
                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                // 将http请求采样器添加到线程组下
                threadGroupHashTree.add(examplecomSampler);
                threadGroupHashTree.add(responseAssertion);

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
                ResultCollectorUtil csvlogger = new ResultCollectorUtil(summer);
                csvlogger.setFilename(csvFile);
                testPlanTree.add(testPlanTree.getArray()[0], logger);
                testPlanTree.add(testPlanTree.getArray()[0], csvlogger);

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
     *
     * return TestPlan
     */
     public static TestPlan createTestPlan(){

         //创建测试计划
         TestPlan testPlan = new TestPlan("压测");
         testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
         testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
         testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());
         testPlan.setSerialized(true);
         testPlan.setTearDownOnShutdown(true);
         return  testPlan;
     }



    /**
     * 创建线程组
     *
     * @return
     */
    public static ThreadGroup createThreadGroup() {

        // Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("线程组");
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(0);
        threadGroup.setScheduler(true);
        threadGroup.setDuration(1);
        threadGroup.setDelay(0);
        threadGroup.setSamplerController(createLoopController());
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
        loopController.setFirst(true);
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
        HeaderManager headerManager = new HeaderManager();
//                headerManager.setProperty("Content-Type", "multipart/form-data");
        CookieManager cookieManager = new CookieManager();
        cookieManager.setProperty("","");
        httpSamplerProxy.setName("天气预报");
        httpSamplerProxy.setDomain("www.weather.com.cn");
        httpSamplerProxy.setPort(80);
        httpSamplerProxy.setPath("/data/sk/101010100.html");
        httpSamplerProxy.setMethod("GET");
        httpSamplerProxy.setConnectTimeout("5000");
        httpSamplerProxy.setUseKeepAlive(true);
        httpSamplerProxy.setHeaderManager(headerManager);
        httpSamplerProxy.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSamplerProxy.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        return httpSamplerProxy;
    }


    /**
     * 设置检查点
     * return
     */

    private static ResponseAssertion createResponseAssertion(){

        //设置检查点
        ResponseAssertion responseAssertion = new ResponseAssertion();
        responseAssertion.addTestString("\"cityid\":\"101010100\"");
        responseAssertion.setTestFieldResponseData();
        return  responseAssertion;
    }


}
