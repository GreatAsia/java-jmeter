package com.okay;


import java.io.File;

import org.apache.jmeter.JMeter;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.joor.Reflect;

/**
 * @author zhou
 * @date 2019/12/30
 */
public class TestPlanLauncher {

       private static final String checkData = "\"cityid\":\"101010100\"";


        public static void main(String[] args) {
            // jemter 引擎
            StandardJMeterEngine standardJMeterEngine = new StandardJMeterEngine();
            // 设置不适用gui的方式调用jmeter
            System.setProperty(JMeter.JMETER_NON_GUI, "true");
            // 设置jmeter.properties文件，我们将jmeter文件存放在resources中，通过classload
            String path = TestPlanLauncher.class.getClassLoader().getResource("jmeter.properties").getPath();
            File jmeterPropertiesFile = new File(path);
            if (jmeterPropertiesFile.exists()) {
                JMeterUtils.loadJMeterProperties(jmeterPropertiesFile.getPath());
                HashTree testPlanTree = new HashTree();
                // 创建测试计划
                TestPlan testPlan = createTestPlan();
                // 创建线程组
                ThreadGroup threadGroup = createThreadGroup();
                // 创建循环控制器
                LoopController loopController = createLoopController();
                // 线程组设置循环控制
                threadGroup.setSamplerController(loopController);
                // 创建http请求收集器
                HTTPSamplerProxy examplecomSampler = createHTTPSamplerProxy();
                //创建检查点
                ResponseAssertion responseAssertion = createResponseAssertion(checkData);

                // 将测试计划添加到测试配置树种
                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                // 将http请求采样器添加到线程组下
                threadGroupHashTree.add(examplecomSampler);
                threadGroupHashTree.add(responseAssertion);
                //增加结果收集
                SummariserUtils summer = null;
                String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
                if (summariserName.length() > 0) {
                    summer = new SummariserUtils(summariserName);
                }
                ResultCollectorUtil logger = new ResultCollectorUtil(summer);
                testPlanTree.add(testPlanTree.getArray(), logger);

                // 配置jmeter
                standardJMeterEngine.configure(testPlanTree);
                // 运行
                standardJMeterEngine.run();


                //获取聚合报告结果
                String info = Reflect.on(summer).get("summerMessage");
                System.out.println("info:" + info);
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
        headerManager.setProperty("Content-Type", "multipart/form-data");


        CookieManager cookieManager = new CookieManager();
        cookieManager.setProperty("","");
        cookieManager.add(new Cookie());

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

    private static ResponseAssertion createResponseAssertion(String checkData){

        //设置检查点
        ResponseAssertion responseAssertion = new ResponseAssertion();
        responseAssertion.addTestString(checkData);
        responseAssertion.setTestFieldResponseData();
        return  responseAssertion;
    }



}
